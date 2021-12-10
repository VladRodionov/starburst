package org.bigbase.textprovider.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.bigbase.textprovider.FileIndex;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestCommands{

  static String filePath = "/Users/vrodionov/Development/starburst/text-provider/src/test/resources/test.txt";
  String[] expected = new String[] {
      "OK\r\nAAAAAAA\r\n",
      "OK\r\nBBBBBBBBBB\r\n",
      "OK\r\nCCCCCCCCCCCC\r\n",
      "OK\r\nDDDDDDDDDDDDDDD\r\n",
      "OK\r\nEEEEEEEEE\r\n",
      "OK\r\nFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF\r\n",
      "OK\r\nHHHHHHHHHHHHHHHHH\r\n",
      "OK\r\nGGGGGGGGGGGGGGGGGGGGG\r\n",
      "OK\r\nJJJJJJJJJJJJJJJJJJJJJJJJJJ\r\n",
      "OK\r\nIII\r\n",
      "OK\r\nKKKKKKKKKKKKKKKKKKKKKKKKKKK\r\n",
      "OK\r\nLLLLLLL\r\n",
      "OK\r\nM\r\n",
      "OK\r\nN\r\n",
      "OK\r\nOOOOOOOOOOOO\r\n",
      "OK\r\nPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP\r\n",
      "OK\r\nRRRRRRRRRR\r\n",
      "OK\r\nSSSSSSSSSSSSS\r\n",
      "OK\r\nTTTTTTTTTTTTTTTTTTT\r\n",
      "OK\r\nQQQQQQQQQ\r\n",
      "OK\r\nUUUUUUUUU\r\n",
      "OK\r\nZZZZZZZZZZZZZZZZ\r\n",
      "OK\r\nXXXXXXXXX\r\n",
      "OK\r\nYYYYYYYYYYYYYY\r\n",
      "OK\r\nW\r\n"
  };
  
  @BeforeClass
  public static void setUp() throws IOException {
    FileIndex.openOrCreate(filePath);

  }
  @Test
  public void testQUIT() throws IOException {
    Command cmd = Command.getCommand("quit");
    assertTrue (cmd instanceof ERR);
    cmd = Command.getCommand("QUIT");
    assertTrue (cmd instanceof QUIT);
    cmd = Command.getCommand("QUIT A");
    assertTrue (cmd instanceof QUIT);
    String result = cmd.executeForTest(filePath, null, null);
    assertEquals("ERR\r\n", result);
  }
  
  @Test
  public void testSHUTDOWN() throws IOException {
    Command cmd = Command.getCommand("shutdown");
    assertTrue (cmd instanceof ERR);
    cmd = Command.getCommand("SHUTDOWN");
    assertTrue (cmd instanceof SHUTDOWN);
    cmd = Command.getCommand("SHUTDOWN A");
    assertTrue (cmd instanceof SHUTDOWN); 
    String result = cmd.executeForTest(filePath, null, null);
    assertEquals("ERR\r\n", result);
  }
  
  @Test
  public void testGET() throws IOException {
    Command cmd = Command.getCommand("get");
    assertTrue (cmd instanceof ERR);
    cmd = Command.getCommand("GET");
    assertTrue (cmd instanceof GET);
    String result = cmd.executeForTest(filePath, null, null);
    assertEquals("ERR\r\n", result);
    
    cmd = Command.getCommand("GET A");
    assertTrue (cmd instanceof GET); 
    result = cmd.executeForTest(filePath, null, null);
    assertEquals("ERR\r\n", result);
    
    RandomAccessFile file = new RandomAccessFile(filePath, "r");
    FileIndex index = FileIndex.getIndexFor(filePath);
    
    cmd = Command.getCommand("GET -10");
    assertTrue (cmd instanceof GET); 
    result = cmd.executeForTest("GET -10", file, index);
    assertEquals("ERR\r\n", result);

    cmd = Command.getCommand("GET 0");
    assertTrue (cmd instanceof GET); 
    result = cmd.executeForTest("GET 0", file, index);
    assertEquals("ERR\r\n", result);
    
    cmd = Command.getCommand("GET "+ (expected.length + 1));
    assertTrue (cmd instanceof GET); 
    result = cmd.executeForTest("GET " + (expected.length + 1), file, index);
    assertEquals("ERR\r\n", result);
    
    for (int i = 1; i <= expected.length; i++) {
      String s = "GET " + i;
      cmd = Command.getCommand(s);
      assertTrue (cmd instanceof GET); 
      result = cmd.executeForTest(s, file, index);
      assertEquals(expected[i-1], result);
    }
  }
  
}
