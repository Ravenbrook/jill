// $Header$

/**
 * Contains Lua's string library.
 * The library can be opened using the {@link StringLib#open} method.
 */
public final class StringLib extends LuaJavaCallback {
  // Each function in the string library corresponds to an instance of
  // this class which is associated (the 'which' member) with an integer
  // which is unique within this class.  They are taken from the following
  // set.
  private static final int BYTE = 1;
  private static final int CHAR = 2;
  private static final int DUMP = 3;
  private static final int FIND = 4;
  private static final int FORMAT = 5;
  private static final int GFIND = 6;
  private static final int GMATCH = 7;
  private static final int GSUB = 8;
  private static final int LEN = 9;
  private static final int LOWER = 10;
  private static final int MATCH = 11;
  private static final int REP = 12;
  private static final int REVERSE = 13;
  private static final int SUB = 14;
  private static final int UPPER = 15;

  /**
   * Which library function this object represents.  This value should
   * be one of the "enums" defined in the class.
   */
  private int which;

  /** Constructs instance, filling in the 'which' member. */
  private StringLib(int which) {
    this.which = which;
  }

  public int luaFunction(Lua L) {
    switch (which) {
      case LEN:
        return len(L);
      case LOWER:
        return lower(L);
      case REP:
        return rep(L);
      case REVERSE:
        return reverse(L);
      case SUB:
        return sub(L);
      case UPPER:
        return upper(L);
    }
    return 0;
  }

  public static void open(Lua L) {
    Object lib = new LuaTable();
    L.setGlobal("string", lib);

    r(L, "byte", BYTE);
    r(L, "char", CHAR);
    r(L, "dump", DUMP);
    r(L, "find", FIND);
    r(L, "format", FORMAT);
    r(L, "gfind", GFIND);
    r(L, "gmatch", GMATCH);
    r(L, "gsub", GSUB);
    r(L, "len", LEN);
    r(L, "lower", LOWER);
    r(L, "match", MATCH);
    r(L, "rep", REP);
    r(L, "reverse", REVERSE);
    r(L, "sub", SUB);
    r(L, "upper", UPPER);

    LuaTable mt = new LuaTable();
    L.setMetatable("", mt);     // set string metatable
    L.setField(mt, "__index", lib);
  }

  /** Register a function. */
  private static void r(Lua L, String name, int which) {
    StringLib f = new StringLib(which);
    Object lib = L.getGlobal("string");
    L.setField(lib, name, f);
  }

  /** Implements string.len */
  private static int len(Lua L) {
    String s = L.checkString(1);
    L.pushNumber(s.length());
    return 1;
  }

  /** Implements string.lower */
  private static int lower(Lua L) {
    String s = L.checkString(1);
    L.push(s.toLowerCase());
    return 1;
  }

  /** Implements string.rep */
  private static int rep(Lua L) {
    String s = L.checkString(1);
    int n = L.checkInt(2);
    StringBuffer b = new StringBuffer();
    for (int i=0; i<n; ++i) {
      b.append(s);
    }
    L.push(b.toString());
    return 1;
  }

  /** Implements string.reverse */
  private static int reverse(Lua L) {
    String s = L.checkString(1);
    StringBuffer b = new StringBuffer();
    int l = s.length();
    while (--l >= 0) {
      b.append(s.charAt(l));
    }
    L.push(b.toString());
    return 1;
  }

  /* Helper for sub(). */
  private static int posrelat(int pos, String s) {
    if (pos >= 0) {
      return pos;
    }
    int len = s.length();
    return len+pos+1;
  }

  /** Implements string.sub */
  private static int sub(Lua L) {
    String s = L.checkString(1);
    int start = posrelat(L.checkInt(2), s);
    int end = posrelat(L.optInt(3, -1), s);
    if (start < 1) {
      start = 1;
    }
    if (end > s.length()) {
      end = s.length();
    }
    if (start <= end) {
      L.push(s.substring(start-1, end));
    } else {
      L.pushLiteral("");
    }
    return 1;
  }

  /** Implements string.upper */
  private static int upper(Lua L) {
    String s = L.checkString(1);
    L.push(s.toUpperCase());
    return 1;
  }
}
