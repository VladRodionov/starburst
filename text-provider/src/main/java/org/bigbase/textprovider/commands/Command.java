package org.bigbase.textprovider.commands;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

import org.bigbase.textprovider.FileIndex;

/**
 * Service command interface
 * Implementations MUST be state-less
 *
 */
public interface Command {
  
  public final static byte[] OK = "OK\r\n".getBytes();
  public final static byte[] ERR = "ERR\r\n".getBytes();
  public final static byte[] CRLF = "\r\n".getBytes();
  
  /*
   * Text provider commands map.
   */
  public static ThreadLocal<HashMap<String, Command>> commandMapTLS =
      new ThreadLocal<HashMap<String, Command>>() {
        @Override
        protected HashMap<String, Command> initialValue() {
          return new HashMap<String, Command>();
        }
      };
  /**
   * Generic command interface
   * @param cmd command string
   * @param file text file to work on
   * @param index file index
   * @param key selection key (client connection)
   * @throws IOException
   */
  public void execute(String cmd, RandomAccessFile file, FileIndex index, SelectionKey key)
      throws IOException;
  
  /**
   * For testing only
   * Commands which returns data must override
   * @param cmd command string
   * @param file text file 
   * @param index text file index
   * @return command response as a string
   */
  public default String executeForTest(String cmd, RandomAccessFile file, FileIndex index) 
    throws IOException
  {
    return null;
  }
  /**
   * Sends OK response
   * @param channel
   * @throws IOException
   */
  default void sendOK(SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    send(channel, OK);
  }

  /**
   * Sends ERR response
   * @param channel
   * @throws IOException
   */
  default void sendERR(SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    send(channel, ERR);
  }

  /**
   * Send CRLF
   * @param channel
   * @throws IOException
   */
  default void sendCRLF(SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    send(channel, CRLF);
  }

  /**
   * Sends byte array
   * @param channel
   * @param data
   * @throws IOException
   */
  default void send(SocketChannel channel, byte[] data) throws IOException {
    ByteBuffer buf = ByteBuffer.wrap(data);
    int sent = 0;
    while (sent < data.length) {
      sent += channel.write(buf);
    }
  }
  
  /**
   * Get command for incoming request to process
   * @param str command string 
   * @return command instance or null
   */
  @SuppressWarnings("deprecation")
  
  public static Command getCommand(String str) {
    HashMap<String, Command> map = commandMapTLS.get();
    String cmdName = str.split(" ")[0];
    Command cmd = map.get(cmdName);
    if (cmd == null) {
      try {
        @SuppressWarnings("unchecked")
        Class<Command> cls =
            (Class<Command>) Class.forName("org.bigbase.textprovider.commands." + cmdName);
        cmd = cls.newInstance();
        map.put(cmdName, cmd);
      } catch (Throwable e) {
        return new ERR();
      }
    }
    return cmd;
  }
}
