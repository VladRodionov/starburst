package org.bigbase.textprovider.commands;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.SelectionKey;

import org.bigbase.textprovider.FileIndex;
import org.bigbase.textprovider.Server;

/**
 * SHUTDOWN command implementation 
 */
public class SHUTDOWN implements Command {

  @Override
  public void execute(String cmd, RandomAccessFile file, FileIndex index,
      SelectionKey key) throws IOException {
    if (cmd.equals("SHUTDOWN")) {
      // Call main Server shutdown (this is the asynchronous call)
      Server.shutdown();
    } else {
      sendERR(key);
    }
  }

  @Override
  public String executeForTest(String cmd, RandomAccessFile file, FileIndex index) 
      throws IOException {
    if (cmd.equals("SHUTDOWN")) {
      return null;//
    } else {
      return "ERR\r\n";
    }
  }
}
