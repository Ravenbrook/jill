// $Header$

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

/**
 * Encapsulates a Lua execution environment.
 */
public final class Lua {
  /** VM data stack. */
  private Vector stack = new Vector();
  private int base = 0;
  private int nCcalls = 0;
  /** Instruction to resume execution at.  Index into code array. */
  private int savedpc = 0;
  /** Vector of CallInfo records. */
  Vector civ;
  /** CallInfo record for currently active function. */
  CallInfo ci;

  /**
   * Calls a Lua value.  Normally this is called on functions, but the
   * semantics of Lua permit calls on any value as long as its metatable
   * permits it.  In order to call a function its arguments must be
   * {@link push pushed} onto the stack, the first argument is pushed
   * first, then the following arguments are pushed in order (direct
   * order).  The parameter <var>n</var> specifies the number of
   * arguments (which may be 0).
   * 
   * @param f  The function to be invoked.
   * @param n  The number of arguments in this function call.
   * @param r  The number of results required.
   */
  public void call(Object f, int n, int r) {
    if (n < 0 || n + base > stack.size()) {
      throw new IllegalArgumentException();
    }
    this.vmCall(f, n, r);
  }

  /**
   * Gets the global environment.  The global environment, where global
   * variables live, is returned as a <code>LuaTable</code>.  Note that
   * modifying this table has exactly the same effect as creating or
   * changing global variables from within Lua.
   * @return  The global environment as a table.
   */
  public LuaTable getGlobals() { return null; }

  /**
   * Tests that an object is a Lua boolean.  Returns <code>true</code>
   * if the object represents a boolean Lua value (<code>true</code> or
   * <code>false</code> in Lua); return <code>false</code> otherwise.
   */
  public static boolean isBoolean(Object o) {
    return o instanceof Boolean;
  }

  /**
   * Tests that an object is a Lua function implementated in Java.
   * Returns <code>true</code> if so, <code>false</code> otherwise.
   */
  public static boolean isJavaFunction(Object o) {
    return o instanceof LuaJavaCallback;
  }

  /**
   * Tests that an object is a Lua function (implemented in Lua or
   * Java).  Returns <code>true</code> if so, <code>false</code>
   * otherwise.
   */
  public static boolean isFunction(Object o) {
    return o instanceof LuaFunction ||
        o instanceof LuaJavaCallback;
  }

  /**
   * Tests that an object is Lua nil.  Returns <code>true</code> if so,
   * <code>false</code> otherwise.
   */
  public static boolean isNil(Object o) {
    return null == o;
  }

  /**
   * Tests that an object is a Lua number.  Returns <code>true</code> if
   * so, <code>false</code> otherwise.
   */
  public static boolean isNumber(Object o) {
    return o instanceof Double;
  }

  /**
   * Tests that an object is a Lua string.  Returns <code>true</code> if
   * so, <code>false</code> otherwise.
   */
  public static boolean isString(Object o) {
    return o instanceof String;
  }

  /**
   * Tests that an object is a Lua table.  Return <code>true</code> if
   * so, <code>false</code> otherwise.
   */
  public static boolean isTable(Object o) {
    return o instanceof LuaTable;
  }

  /**
   * Tests that an object is a Lua thread.  Return <code>true</code> if
   * so, <code>false</code> otherwise.
   */
  public static boolean isThread(Object o) {
    // :todo: implement me.
    return false;
  }

  /**
   * Tests that an object is a Lua userdata.  Return <code>true</code>
   * if so, <code>false</code> otherwise.
   */
  public static boolean isUserdata(Object o) {
    return o instanceof LuaUserdata;
  }

  /**
   * Tests that an object is a Lua value.  Returns <code>true</code> for
   * an argument that is a Jili representation of a Lua value,
   * <code>false</code> for Java references that are not Lua values.
   * For example <code>isValue(new LuaTable())</code> is
   * <code>true</code>, but <code>isValue(new Object[] { })</code> is
   * <code>false</code> because Java arrays are not a representation of
   * any Lua value.
   * PUC-Rio Lua provides no
   * counterpart for this method because in their implementation it is
   * impossible to get non Lua values on the stack, whereas in Jili it
   * is common to mix Lua values with ordinary, non Lua, Java objects.
   */
  public static boolean isValue(Object o) {
    return o == null ||
        o instanceof Boolean ||
        o instanceof String ||
        o instanceof Double ||
        o instanceof LuaFunction ||
        o instanceof LuaJavaCallback ||
        o instanceof LuaTable ||
        o instanceof LuaUserdata;
  }

  /**
   * Loads a Lua chunk in binary or source form.
   * Comparable to C's lua_load.  If the chunk is determined to be
   * binary then it is loaded directly.  Otherwise the chunk is assumed
   * to be a Lua source chunk and compilation is required first.  The
   * <code>InputStream</code> is used to create a <code>Reader</code>
   * (using the {@link InputStreamReader#InputStreamReader(InputStream)}
   * constructor) and the Lua source is compiled.
   * @param in         The binary chunk as an InputStream, for example from
   *                   {@link Class#getResourceAsStream}.
   * @param chunkname  The name of the chunk.
   * @return           The chunk as a function.
   */
  public LuaFunction load(InputStream in, String chunkname)
      throws IOException {
    // Currently always assumes binary.  :todo: implement source loading.
    Loader l = new Loader(in, chunkname);

    LuaFunction f = new LuaFunction(l.undump(),
        new UpVal[0],
        this.getGlobals());
    return f;
  }

  /**
   * Loads a Lua chunk in source form.
   * Comparable to C's lua_load.  Since this takes a Reader parameter,
   * this method is restricted to loading Lua chunks in source form.
   * @param in         The source chunk as a Reader, for example from
   *                   <code>java.io.InputStreamReader(Class.getResourceAsStream)</code>.
   * @param chunkname  The name of the chunk.
   * @return           The chunk as a function (after having been compiled).
   * @see java.io.InputStreamReader
   */
  public LuaFunction load(Reader in, String chunkname) { return null; }

  /**
   * Pushes a value onto the stack in preparation for calling a
   * function.  See {@link call} for the protocol to be used for calling
   * functions.
   */
  public void push(Object o) {
    stack.addElement(o);
  }

  /** Interned for use by valueOfBoolean.  */
  private static final Boolean FALSE = new Boolean(false);
  /** Interned for use by valueOfBoolean.  */
  private static final Boolean TRUE = new Boolean(true);

  /**
   * Converts primitive boolean into a Lua value.  If CLDC 1.1 had
   * <code>java.lang.Boolean.valueOf(boolean);</code> then I probably
   * wouldn't have written this.  This does have a small advantage:
   * code that used this method does not need to assume that Lua booleans in
   * Jili are represented using Java.lang.Boolean.
   */
  public static Object valueOfBoolean(boolean b) {
    if (b) {
      return TRUE;
    } else {
      return FALSE;
    }
  }
  /**
   * Converts primitive number into a Lua value.
   */
  public static Object valueOfNumber(double d) {
    // :todo: consider interning "common" numbers, like 0, 1, -1, etc.
    return new Double(d);
  }

  /**
   * Provide <code>Reader</code> interface over a <code>String</code>.
   * Equivalent of {@link java.io.StringReader#StringReader} from J2SE.
   * The ability to convert a <code>String</code> to a
   * <code>Reader</code> is required internally,
   * to provide the Lua function <code>loadstring</code>; exposed
   * externally as a convenience.
   */
  public Reader StringReader(String s) { return null; }

  // VM

  private static final int PCRLUA = 0;

  /** Equivalent of luaD_call. */
  private void vmCall(Object f, int n, int r) {
    ++nCcalls;
    if (vmPrecall(f, n, r) == PCRLUA) {
      vmExecute(1);
    }
    --nCcalls;
  }

  private void vmExecute(int nexeccalls) {
    // :todo: implement me.
    return ;
  }

  /** Equivalent of LuaD_precall. */
  private int vmPrecall(Object fArg, int n, int r) {
    // :todo: metamethod for non-function values.
    this.ci.setSavedpc(savedpc);
    if (fArg instanceof LuaFunction) {
      LuaFunction f = (LuaFunction)fArg;
      Proto p = f.proto();
      // :todo: ensure enough stack

      // :todo: implement vararg convention
      base = stack.size() - n;
      if (stack.size() > base + p.numparams()) {
        // trim stack to the argument list
        stack.setSize(base + p.numparams());
      }
      int top = base + p.maxstacksize();

      CallInfo ci = inc_ci(f, base, top, r);

      savedpc = 0;
      // expand stack to the function's max stack size.
      stack.setSize(top);
      // :todo: implement call hook.
      return PCRLUA;
    }
    // :todo: implement calls to Lua Java functions.
    throw new IllegalArgumentException();
  }

  /** Make new CallInfo record. */
  private CallInfo inc_ci(Object f, int base, int top, int nresults) {
    CallInfo ci = new CallInfo(f, base, top, nresults);
    civ.addElement(ci);
    return ci;
  }
}

