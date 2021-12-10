package org.bigbase.textprovider;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * This class provides text file index implementation.
 * The index is a file-based to support very large files
 * when index won't fit in RAM
 * All performance relies on OS page cache. For small files
 * which fit into RAM performance must be good. As file size increases
 * performance will decrease but the service will work until all data
 * fits server's disk. 
 *
 */
public class FileIndex {
  public static class Range {
    public final long offset;
    public final long size;
    
    public Range(long off, long size) {
      this.offset = off;
      this.size = size;
    }
    
    @Override
    public String toString() {
      return "off:" + offset + " size:"+ size;
    }
  }
  
  /*
   * Logger
   */
  private static final Logger log = LogManager.getLogger(FileIndex.class);
  
  /*
   * Thread local cache of file indexes
   */
  private static ThreadLocal<Map<String, FileIndex>> indexMap = new ThreadLocal<Map<String, FileIndex>>() {

    @Override
    protected Map<String, FileIndex> initialValue() {
      return new HashMap<>();
    }
    
  };
  
  /* Index file as RAF */
  private RandomAccessFile indexFile;
  /* Total number of lines in a text file */
  private long totalLines = 0;
  /* Buffer to read index data into */
  private ByteBuffer buf = ByteBuffer.allocateDirect(16);
  
  /**
   * Constructor
   * @param filePath path to an index file
   * @throws IOException
   */
  private FileIndex(String filePath) throws IOException {
    this.indexFile = new RandomAccessFile(filePath, "r");
    this.totalLines = this.indexFile.length() / 8 - 1;
  }
  
  /**
   * Thread local get index for a text file
   * @param fileName text file absolute path name
   * @return file index
   * @throws IOException
   */
  public static FileIndex getIndexFor(String fileName) throws IOException {
    Map<String, FileIndex> map = indexMap.get();
    fileName += ".index";
    FileIndex index = map.get(fileName);
    if (index == null) {
      index = new FileIndex(fileName);
      map.put(fileName, index);
    }
    return index;
  }
  
  /**
   * Get range for a line with a given number
   * @param lineNumber
   * @return offset + size range in an indexed file
   * @throws IOException
   */
  public Range getRange(long lineNumber) throws IOException {
    if (lineNumber < 1 || lineNumber > totalLines) {
      return null;
    }
    
    long offset = (lineNumber - 1) * 8;
    buf.clear();
    int read = 0;
    while(read < 16) {
      read += indexFile.getChannel().read(buf, offset);
    }
    buf.flip();
    LongBuffer lb = buf.asLongBuffer();
    long off = lb.get();
    long size = lb.get() - off - 1;
    return new Range(off, size);
    
  }
  
  /**
   * Close index
   */
  public void close() {
    try {
      if (indexFile != null) {
        indexFile.close();
      }
    } catch(IOException e) {}
  }
  
  /**
   * Opens existing one or creates new index file
   * @param filePath path to  text file
   * @return file index instance
   * @throws IOException
   */
  public static void openOrCreate(String filePath) throws IOException {
    Path path = Path.of(filePath);
    Path file = path.getFileName();
    Path indexFile = Path.of(file.toString() + ".index");
    Path index = path.resolveSibling(indexFile);
    
    if(Files.exists(index)) {
      log.info("Found existing index file {}", index.toAbsolutePath());
      FileTime parentModTime = Files.getLastModifiedTime(path);
      FileTime indexModTime = Files.getLastModifiedTime(index);
      if (parentModTime.toMillis() < indexModTime.toMillis()) {
        log.info("Skipping index build for {}", path.toAbsolutePath());
        return;
      } else {
        // Delete current index and rebuild it
        Files.delete(index);
      }
    }
    buildIndex(path, index);
  }
  
  /**
   * Builds index file for a given file
   * @param filePath text file path
   * @param indexPath index file path
   * @throws IOException
   */
  private static void buildIndex(Path filePath, Path indexPath) 
      throws IOException 
  {
    log.info("Building index for {}", filePath.toAbsolutePath());
    long startTime = System.currentTimeMillis();
    FileOutputStream fos = new FileOutputStream(indexPath.toFile());
    BufferedOutputStream bos = new BufferedOutputStream(fos, 64 * 1024);
    DataOutputStream dos = new DataOutputStream(bos);
    dos.writeLong(0);
    try ( Stream<String> lines = Files.lines(filePath);) {
      Iterator<String> it = lines.iterator();
      long offset = 0;
      while(it.hasNext()) {
        String s = it.next();
        offset += s.length() + 1 /*1 byte for '\n'*/;
        dos.writeLong(offset);
      }
      dos.flush();
      dos.close();
    }
    long endTime = System.currentTimeMillis();
    log.info("Finished building index in {} ms", endTime - startTime);
  }  
}
