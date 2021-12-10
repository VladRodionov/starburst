package org.bigbase.textprovider.commands;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.SelectionKey;

import org.bigbase.textprovider.FileIndex;

/**
 * 
 * Command which sends ERR back to client
 *
 */
public class ERR implements Command {

  @Override
  public void execute(String cmd, RandomAccessFile file, FileIndex index,
      SelectionKey key) throws IOException {
    sendERR(key);
  }
  
  @Override
  public String executeForTest(String cmd, RandomAccessFile file, FileIndex index) {
    return "ERR\r\n";
  }

}
