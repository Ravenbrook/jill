// $Header$

/**
 * Contains Lua's base library.  The base library is generally
 * considered essential for running any Lua program.  The base library
 * can be opened using the {@link BaseLib#open} method.
 */
public final class BaseLib extends LuaJavaCallback {
  // Each function in the base library corresponds to an instance of
  // this class which is associated (the 'which' member) with an integer
  // which is unique within this class.  They are taken from the following
  // set.
  private static final int ASSERT = 1;
  private static final int COLLECTGARBAGE = 2;
  private static final int DOFILE = 3;
  private static final int ERROR = 4;
  private static final int GCINFO = 5;
  private static final int GETFENV = 6;
  private static final int GETMETATABLE = 7;
  private static final int LOADFILE = 8;
  private static final int LOAD = 9;
  private static final int LOADSTRING = 10;
  private static final int NEXT = 11;
  private static final int PCALL = 12;
  private static final int PRINT = 13;
  private static final int RAWEQUAL = 14;
  private static final int RAWGET = 15;
  private static final int RAWSET = 16;
  private static final int SELECT = 17;
  private static final int SETFENV = 18;
  private static final int SETMETATABLE = 19;
  private static final int TONUMBER = 20;
  private static final int TOSTRING = 21;
  private static final int TYPE = 22;
  private static final int UNPACK = 23;
  private static final int XPCALL = 24;

  /**
   * Which library function this object represents.  This value should
   * be one of the "enums" defined in the class.
   */
  private int which;

  /** Constructs instance, filling in the 'which' member. */
  private BaseLib(int which) {
    this.which = which;
  }

  public int luaFunction(Lua L) {
    switch (which) {
      case PRINT:
        return print(L);
    }
    return 0;
  }

  public static void open(Lua L) {
    // set global _G
    L.setglobal("_G", L.getGlobals());
    r(L, "assert", ASSERT);
    r(L, "collectgarbage", COLLECTGARBAGE);
    r(L, "dofile", DOFILE);
    r(L, "error", ERROR);
    r(L, "gcinfo", GCINFO);
    r(L, "getfenv", GETFENV);
    r(L, "getmetatable", GETMETATABLE);
    r(L, "loadfile", LOADFILE);
    r(L, "load", LOAD);
    r(L, "loadstring", LOADSTRING);
    r(L, "next", NEXT);
    r(L, "pcall", PCALL);
    r(L, "print", PRINT);
    r(L, "rawequal", RAWEQUAL);
    r(L, "rawget", RAWGET);
    r(L, "rawset", RAWSET);
    r(L, "select", SELECT);
    r(L, "setfenv", SETFENV);
    r(L, "setmetatable", SETMETATABLE);
    r(L, "tonumber", TONUMBER);
    r(L, "tostring", TOSTRING);
    r(L, "type", TYPE);
    r(L, "unpack", UNPACK);
    r(L, "xpcall", XPCALL);
    // :todo: ipairs and pairs
  }

  /** Register a function. */
  private static void r(Lua L, String name, int which) {
    BaseLib f = new BaseLib(which);
    L.setglobal(name, f);
  }

  /** Implements print. */
  private static int print(Lua L) {
    int n = L.gettop();
    Object tostring = L.getglobal("tostring");
    for(int i=1; i<=n; ++i) {
      L.push(tostring);
      L.pushvalue(i);
      L.call(1, 1);
      String s = L.toString(L.value(-1));
      if (s == null) {
        // :todo: error
	throw new NullPointerException();
      }
      if (i>1) {
        System.out.print('\t');
      }
      System.out.print(s);
      L.pop(1);
    }
    System.out.println();
    return 0;
  }
}
