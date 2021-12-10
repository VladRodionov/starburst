package org.bigbase.textprovider.commands;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.bigbase.textprovider.FileIndex;

/**
 * GET command implementation 
 */
public class GET implements Command {

  @Override
  public void execute(String cmd, RandomAccessFile file, FileIndex index,
      SelectionKey key) throws IOException {
    
    String[] parts = cmd.split(" ");
    if (parts.length != 2) {
      // we expect GET <n> 
      sendERR(key);
      return;
    }
    // try to parse parts[1]
    long lineNumber = -1;
    try {
      lineNumber = Long.parseLong(parts[1]);
    } catch(NumberFormatException e) {
      sendERR(key);
      return;
    }
    FileIndex.Range range = index.getRange(lineNumber);
    if (range == null) {
      sendERR(key);
      return;
    }
    // Send line back to client
    sendOK(key);
    long offset = range.offset;
    long size = range.size;
    long sent = 0;
    FileChannel fc = file.getChannel();
    SocketChannel sc = (SocketChannel) key.channel();
    while(sent < size) {
      sent += fc.transferTo(offset + sent, size - sent, sc);
    }
    sendCRLF(key);  
  }
  
  @Override
  public String executeForTest(String cmd, RandomAccessFile file, FileIndex index)
      throws IOException {
    
    String[] parts = cmd.split(" ");
    if (parts.length != 2) {
      // we expect GET n
      return "ERR\r\n";
    }
    // try to parse parts[1]
    long lineNumber = -1;
    try {
      lineNumber = Long.parseLong(parts[1]);
    } catch(NumberFormatException e) {
      return "ERR\r\n";
    }
    FileIndex.Range range = index.getRange(lineNumber);
    if (range == null) {
      return "ERR\r\n";
    }
    // Send line back to client
    String result = "OK\r\n";
    byte[] buf = new byte[(int) range.size];
    file.seek(range.offset);
    file.readFully(buf);
    result += new String(buf);
    result += "\r\n";
    return result;
  }
}
