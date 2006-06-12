// $Header$

import java.io.IOException;

/** Ersatz replacement for {@link java.io.StringReader} from JSE. */
final class StringReader extends java.io.Reader {
  private String s;
  /** Index of the current read position.  -1 if closed. */
  private int current;  // = 0
  /**
   * Index of the current mark (set with {@link StringReader#mark}).
   */
  private int mark;     // = 0;

  StringReader(String s) {
    this.s = s;
  }

  public void close() {
    current = -1;
  }

  public void mark() {
    mark = current;
  }

  public boolean markSupported() {
    return true;
  }

  public int read() throws IOException {
    if (current < 0) {
      throw new IOException();
    }
    if (current >= s.length()) {
      return -1;
    }
    return s.charAt(current++);
  }

  public int read(char[] cbuf, int off, int len) throws IOException {
    if (current < 0 || len < 0) {
      throw new IOException();
    }
    if (current >= s.length()) {
      return 0;
    }
    if (current + len > s.length()) {
      len = s.length() - current;
    }
    for (int i=0; i<len; ++i) {
      cbuf[off+i] = s.charAt(current+i);
    }
    current += len;
    return len;
  }

  public void reset() {
    current = mark;
  }
}
