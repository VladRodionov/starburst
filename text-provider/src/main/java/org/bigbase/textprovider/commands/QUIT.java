package org.bigbase.textprovider.commands;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bigbase.textprovider.FileIndex;

/**
 * Client QUIT command implementation
 *
 */
public class QUIT implements Command {

  private final static Logger log = LogManager.getLogger(QUIT.class);

  public void execute(String cmd, RandomAccessFile file, FileIndex index,
      SelectionKey key) throws IOException {
    if (cmd.equals("QUIT")) {
      // Shutdown client
      closeClient(key);
    } else {
      sendERR(key);
    }
  }
  
  @Override
  public String executeForTest(String cmd, RandomAccessFile file, FileIndex index) 
      throws IOException {
    if (cmd.equals("QUIT")) {
      return null;//
    } else {
      return "ERR\r\n";
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
}
