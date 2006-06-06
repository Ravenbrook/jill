// $Header$

import java.util.Enumeration;

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

  private static final int IPAIRS = 25;
  private static final int PAIRS = 26;
  private static final int IPAIRS_AUX = 27;
  private static final int PAIRS_AUX = 28;

  /**
   * Lua value that represents the generator function for ipairs.  In
   * PUC-Rio this is implemented as an upvalue of ipairs.
   */
  private static final Object ipairsauxFunction = new BaseLib(IPAIRS_AUX);
  /**
   * Lua value that represents the generator function for pairs.  In
   * PUC-Rio this is implemented as an upvalue of pairs.
   */
  private static final Object pairsauxFunction = new BaseLib(PAIRS_AUX);

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
      case ERROR:
        return error(L);
      case GETFENV:
        return getfenv(L);
      case IPAIRS:
        return ipairs(L);
      case NEXT:
        return next(L);
      case PAIRS:
        return pairs(L);
      case PCALL:
        return pcall(L);
      case PRINT:
        return print(L);
      case RAWEQUAL:
        return rawequal(L);
      case RAWGET:
        return rawget(L);
      case RAWSET:
        return rawset(L);
      case SELECT:
        return select(L);
      case SETFENV:
        return setfenv(L);
      case TONUMBER:
        return tonumber(L);
      case TOSTRING:
        return tostring(L);
      case TYPE:
        return type(L);
      case UNPACK:
        return unpack(L);
      case IPAIRS_AUX:
        return ipairsaux(L);
      case PAIRS_AUX:
        return pairsaux(L);
    }
    return 0;
  }

  public static void open(Lua L) {
    // set global _G
    L.setGlobal("_G", L.getGlobals());
    r(L, "assert", ASSERT);
    r(L, "collectgarbage", COLLECTGARBAGE);
    r(L, "dofile", DOFILE);
    r(L, "error", ERROR);
    r(L, "gcinfo", GCINFO);
    r(L, "getfenv", GETFENV);
    r(L, "getmetatable", GETMETATABLE);
    r(L, "ipairs", IPAIRS);
    r(L, "loadfile", LOADFILE);
    r(L, "load", LOAD);
    r(L, "loadstring", LOADSTRING);
    r(L, "next", NEXT);
    r(L, "pairs", PAIRS);
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
  }

  /** Register a function. */
  private static void r(Lua L, String name, int which) {
    BaseLib f = new BaseLib(which);
    L.setGlobal(name, f);
  }

  /** Implements error. */
  private static int error(Lua L) {
    int level = L.optInt(2, 1);
    L.setTop(1);
    if (L.isString(L.value(1)) && level > 0) {
      L.insert(L.where(level), 1);
      L.concat(2);
    }
    L.error(L.value(1));
    // NOTREACHED
    return 0;
  }

  /** helper for getfenv and setfenv. */
  private static Object getfunc(Lua L) {
    Object o = L.value(1);
    if (L.isFunction(o)) {
      return o;
    }
    // :todo: support integer args
    throw new IllegalArgumentException();
  }

  /** Implements getfenv. */
  private static int getfenv(Lua L) {
    Object o = getfunc(L);
    if (L.isJavaFunction(o)) {
      L.push(L.getGlobals());
    } else {
      LuaFunction f = (LuaFunction)o;
      L.push(f.getEnv());
    }
    return 1;
  }

  /** Implements next. */
  private static int next(Lua L) {
    L.checkType(1, Lua.TTABLE);
    L.setTop(2);        // Create a 2nd argument is there isn't one
    if (L.next(1)) {
      return 2;
    }
    L.push(Lua.NIL);
    return 1;
  }

  /** Implements ipairs. */
  private static int ipairs(Lua L) {
    L.checkType(1, Lua.TTABLE);
    L.push(ipairsauxFunction);
    L.pushValue(1);
    L.pushNumber(0);
    return 3;
  }

  /** Generator for ipairs. */
  private static int ipairsaux(Lua L) {
    int i = L.checkInt(2);
    L.checkType(1, Lua.TTABLE);
    ++i;
    Object v = L.rawGetI(L.value(1), i);
    if (L.isNil(v)) {
      return 0;
    }
    L.pushNumber(i);
    L.push(v);
    return 2;
  }

  /** Implements pairs.  PUC-Rio uses "next" as the generator for pairs.
   * Jili doesn't do that because it would be way too slow.  We use the
   * java.util.Enumeration returned from java.util.Table.elements.
   */
  private static int pairs(Lua L) {
    L.checkType(1, Lua.TTABLE);
    L.push(pairsauxFunction);                   // return generator,
    LuaTable t = (LuaTable)L.value(1);
    L.push(new Object[] { t, t.keys() });   // state,
    L.push(Lua.NIL);                            // and initial value.
    return 3;
  }

  /** Generator for pairs.  This expects a <var>state</var> and
   * <var>var</var> as (Lua) arguments.
   * The state is setup by {@link BaseLib#pairs} and is a
   * pair of {LuaTable, Enumeration} stored in a 2-element array.  The
   * <var>var</var> is not used.  This is in contrast to the PUC-Rio
   * implementation, where the state is the table, and the var is used
   * to generated the next key in sequence.
   */
  private static int pairsaux(Lua L) {
    Object[] a = (Object[])L.value(1);
    LuaTable t = (LuaTable)a[0];
    Enumeration e = (Enumeration)a[1];
    if (!e.hasMoreElements()) {
      return 0;
    }
    Object key = e.nextElement();
    L.push(key);
    L.push(t.get(key));
    return 2;
  }

  /** Implements pcall. */
  private static int pcall(Lua L) {
    L.checkAny(1);
    int status = L.pcall(L.getTop()-1, Lua.MULTRET, null);
    boolean b = (status == 0);
    L.insert(L.valueOfBoolean(b), 1);
    return L.getTop();
  }

  /** Implements print. */
  private static int print(Lua L) {
    int n = L.getTop();
    Object tostring = L.getGlobal("tostring");
    for(int i=1; i<=n; ++i) {
      L.push(tostring);
      L.pushValue(i);
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

  /** Implements rawequal. */
  private static int rawequal(Lua L) {
    L.checkAny(1);
    L.checkAny(2);
    L.pushBoolean(L.rawEqual(L.value(1), L.value(2)));
    return 1;
  }

  /** Implements rawget. */
  private static int rawget(Lua L) {
    L.checkType(1, Lua.TTABLE);
    L.checkAny(2);
    L.push(L.rawGet(L.value(1), L.value(2)));
    return 1;
  }

  /** Implements rawset. */
  private static int rawset(Lua L) {
    L.checkType(1, Lua.TTABLE);
    L.checkAny(2);
    L.checkAny(3);
    L.rawSet(L.value(1), L.value(2), L.value(3));
    return 0;
  }

  /** Implements select. */
  private static int select(Lua L) {
    int n = L.getTop();
    if (L.type(1) == Lua.TSTRING && "#".equals(L.toString(L.value(1)))) {
      L.pushNumber(n-1);
      return 1;
    }
    int i = L.checkInt(1);
    if (i < 0) {
      i = n + i;
    } else if (i > n) {
      i = n;
    }
    L.argCheck(1 <= i, 1, "index out of range");
    return n-i;
  }

  /** Implements setfenv. */
  private static int setfenv(Lua L) {
    L.checkType(2, Lua.TTABLE);
    Object o = getfunc(L);
    Object first = L.value(1);
    if (L.isNumber(first) && L.toNumber(first) == 0) {
      // :todo: change environment of current thread.
      return 0;
    } else if (L.isJavaFunction(o) || !L.setFenv(first, L.value(2))) {
      // :todo: error
      throw new IllegalArgumentException();
    }
    L.setTop(1);
    return 1;
  }

  /** Implements tonumber. */
  private static int tonumber(Lua L) {
    int base = L.optInt(2, 10);
    if (base == 10) {   // standard conversion
      L.checkAny(1);
      Object o = L.value(1);
      if (L.isNumber(o)) {
        L.pushNumber(L.toNumber(o));
        return 1;
      }
    } else {
      String s = L.checkString(1);
      L.argCheck(2 <= base && base <= 36, 2, "base out of range");
      // :todo: consider stripping space and sharing some code with
      // Lua.vmTostring
      try {
        int i = Integer.parseInt(s, base);
        L.pushNumber(i);
        return 1;
      } catch (NumberFormatException e_) {
      }
    }
    L.push(L.NIL);
    return 1;
  }

  /** Implements tostring. */
  private static int tostring(Lua L) {
    L.checkAny(1);
    Object o = L.value(1);

    // :todo: metamethod
    switch (L.type(1)) {
      case Lua.TNUMBER:
        L.push(L.toString(o));
        break;
      case Lua.TSTRING:
        L.push(o);
        break;
      case Lua.TBOOLEAN:
        if (L.toBoolean(o)) {
          L.pushLiteral("true");
        } else {
          L.pushLiteral("false");
        }
        break;
      case Lua.TNIL:
        L.pushLiteral("nil");
        break;
      default:
        L.push(o.toString());
        break;
    }
    return 1;
  }

  /** Implements type. */
  private static int type(Lua L) {
    L.checkAny(1);
    L.push(L.typeNameOfIndex(1));
    return 1;
  }

  /** Implements unpack. */
  private static int unpack(Lua L) {
    L.checkType(1, Lua.TTABLE);
    LuaTable t = (LuaTable)L.value(1);
    int i = L.optInt(2, 1);
    int e = L.optInt(3, t.getn());
    int n = e - i + 1;  // number of elements
    if (n <= 0) {
      return 0;         // empty range
    }
    // i already initialised to start index, which isn't necessarily 1
    for (; i<=e; ++i) {
      L.push(t.getnum(i));
    }
    return n;
  }

}
