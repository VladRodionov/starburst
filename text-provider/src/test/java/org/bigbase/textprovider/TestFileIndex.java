package org.bigbase.textprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.bigbase.textprovider.FileIndex.Range;
import org.junit.Test;

public class TestFileIndex {

  String filePath = "/Users/vrodionov/Development/starburst/text-provider/src/test/resources/test.txt";
  String[] expected = new String[] {
      "AAAAAAA",
      "BBBBBBBBBB",
      "CCCCCCCCCCCC",
      "DDDDDDDDDDDDDDD",
      "EEEEEEEEE",
      "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
      "HHHHHHHHHHHHHHHHH",
      "GGGGGGGGGGGGGGGGGGGGG",
      "JJJJJJJJJJJJJJJJJJJJJJJJJJ",
      "III",
      "KKKKKKKKKKKKKKKKKKKKKKKKKKK",
      "LLLLLLL",
      "M",
      "N",
      "OOOOOOOOOOOO",
      "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
      "RRRRRRRRRR",
      "SSSSSSSSSSSSS",
      "TTTTTTTTTTTTTTTTTTT",
      "QQQQQQQQQ",
      "UUUUUUUUU",
      "ZZZZZZZZZZZZZZZZ",
      "XXXXXXXXX",
      "YYYYYYYYYYYYYY",
      "W"
  };
  @Test
  public void testFileIndex() throws IOException {
    
    FileIndex.openOrCreate(filePath);
    FileIndex index = FileIndex.getIndexFor(filePath);
    assertNull(index.getRange(-10));
    assertNull(index.getRange(-1));
    assertNull(index.getRange(0));
    
    assertNull(index.getRange(expected.length + 1));
    assertNull(index.getRange(expected.length + 2));
    assertNull(index.getRange(expected.length + 10));
    
    RandomAccessFile file = new RandomAccessFile(filePath, "r");
    
    for(int i = 1; i <= expected.length; i++) {
      String exp = expected[i - 1];
      FileIndex.Range r = index.getRange(i);
      assertNotNull(r);
      String result = loadAsString(file, r);
      System.out.printf("Range=%s exp=%s result=%s\n", r, exp, result);
      assertEquals(exp, result);
    }
  }
  
  private String loadAsString(RandomAccessFile file, Range r) throws IOException {  
    byte[] buf = new byte[(int)r.size];
    file.seek(r.offset);
    file.readFully(buf);
    return new String(buf);
  }
  
  
}
