package org.bigbase.textprovider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bigbase.textprovider.commands.Command;

/**
 * 
 * Request handler, basically - data sender
 * It receives offset in file, number of bytes to send back to a client
 * and selection key, which represents client's connection
 *
 */
public class RequestHandler implements Runnable {
  private final static Logger log = LogManager.getLogger(RequestHandler.class);

  
  /*
   * Thread local map file_path -> RandomAccesFile (its a cache for a thread pool)
   */
  private static ThreadLocal<Map<String, RandomAccessFile>> fileMap = 
      new ThreadLocal<Map<String, RandomAccessFile>>() {

    @Override
    protected Map<String, RandomAccessFile> initialValue() {
      return new HashMap<>();
    }
    
  };
  
  /**
   * Thread local byte buffer - caches buffers for pool threads
   */
  private static ThreadLocal<ByteBuffer> buffer = new ThreadLocal<ByteBuffer>() {
    @Override
    protected ByteBuffer initialValue() {
      return ByteBuffer.allocateDirect(4096);
    }
  };
  
  /* Selection key to process */
  private SelectionKey key;
  /* Full path to a text file */
  private String fileName;

  /**
   * Constructor
   * @param fileName text file full path
   * @param key selection key (client's connection)
   */
  public RequestHandler(String fileName, SelectionKey key) {
    this.key = key;
    this.fileName = fileName;
  }
  
  @Override
  public void run() {
    if (Thread.interrupted()) {
      shutdown();
      return;
    }
    if (!this.key.isValid()) {
      // Should not happen actually
      return;
    }
    try {
      String request = readRequestAsString();
      Command cmd = Command.getCommand(request);
      cmd.execute(request, getFile(), getIndex(), key);
      key.attach(null); // mark selector as not in-use
    } catch (NonReadableChannelException ee) {
      log.error("[{}]", Thread.currentThread().getName(), ee);
      abort();
    } catch (IOException e) {
      log.error("[{}]", Thread.currentThread().getName(), e);
      // Cancel the key, b/c there is an issue with client connection 
      closeClient(key);
      return;
    }
  }
  /**
   * Cancels a selection key and closes client
   * @param key selection key
   */
   private void closeClient(SelectionKey key) {
     try {
       key.cancel();
       SocketChannel channel = (SocketChannel) key.channel();
       channel.close();
     } catch(IOException e) {
       log.error("[{}]", Thread.currentThread().getName(), e);
     }
   }
   
  /**
   * Reads clients request and returns it as a string
   * @return request string
   * @throws IOException
   */
  private String readRequestAsString() throws IOException {
    ByteBuffer buf = buffer.get();
    buf.clear();
    SocketChannel channel = (SocketChannel) key.channel();
    while(!isCompleteRequest(buf)) {
      channel.read(buf);
    }
    buf.flip();
    byte[] arr = new byte[buf.limit() - 2];
    buf.get(arr);
    return new String(arr);
  }
  
  /**
   * Checks if request is complete. 
   * It returns true when last two bytes read are '\r' '\n'
   * or when buffer is full (wrong request format)
   * @param buf buffer contained read data
   * @return true if complete, false - otherwise
   */
  private boolean isCompleteRequest(ByteBuffer buf) {
    if (buf.position() < 2) return false;
    int pos = buf.position();
    return buf.position() == buf.capacity() || 
        (buf.get(pos - 2) == (byte) '\r' && buf.get(pos -1) == (byte) '\n');
  }
  
  /**
   * Returns thread local instance of RAF
   * @return random access file instance for a processing thread
   * @throws FileNotFoundException
   */
  private RandomAccessFile getFile() throws FileNotFoundException {
     Map<String, RandomAccessFile> map = fileMap.get();
     RandomAccessFile file = map.get(fileName);
     if (file == null) {
       file = new RandomAccessFile(fileName, "r");
       map.put(fileName, file);
     }
     return file;
  }
  
  /**
   * Returns thread local instance of a file's index
   * @return file index
   * @throws IOException
   */
  private FileIndex getIndex() throws IOException {
    return FileIndex.getIndexFor(fileName);
  }
  
  /**
   * Shutdowns 
   */
  private void shutdown() {
    log.info("[{}] shutting down", Thread.currentThread().getName());
    try {
      RandomAccessFile file = getFile();
      file.close();
      FileIndex index = getIndex();
      index.close();
    } catch (IOException e) {
      // swallow it - does not matter
    }
  }

  /**
   * This is a hard error - file is not readable so we shut down everything
   */
  private void abort() {
    log.info("[{}]  aborted.", Thread.currentThread().getName());
    Server.shutdown();
  }

}
