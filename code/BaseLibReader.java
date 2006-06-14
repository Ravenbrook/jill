// $Header$

/**
 * Extends @{link java.io.Reader} to create a Reader from a Lua
 * function.  So that the <code>load</code> function from Lua's base
 * library can be implemented.
 */
final class BaseLibReader extends java.io.Reader {
  private String s = "";
  private int i;        // = 0;
  private Lua L;
  private Object f;

  BaseLibReader(Lua L, Object f) {
    this.L = L;
    this.f = f;
  }

  public void close() {
    f = null;
  }

  public int read() {
    if (i >= s.length()) {
      L.push(f);
      L.call(0, 1);
      if (L.isNil(L.value(-1))) {
        return -1;
      } else if(L.isString(L.value(-1))) {
        s = L.toString(L.value(-1));
        if (s.length() == 0) {
          return -1;
        }
        i = 0;
      } else {
        L.error("reader function must return a string");
      }
    }
    return s.charAt(i++);
  }
    
  public int read(char[] cbuf, int off, int len) {
    for (int i=0; i<len; ++i) {
      int c = read();
      if (c == -1) {
        if (i == 0) {
          return -1;
        } else {
          return i;
        }
      }
      cbuf[off+i] = (char)c;
    }
    return i;
  }
}
