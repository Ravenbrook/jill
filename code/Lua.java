/*  $Header$
 *  (c) Copyright 2006, Intuwave Ltd. All Rights Reserved.
 *
 *  Although Intuwave has tested this program and reviewed the documentation,
 *  Intuwave makes no warranty or representation, either expressed or implied,
 *  with respect to this software, its quality, performance, merchantability,
 *  or fitness for a particular purpose. As a result, this software is licensed
 *  "AS-IS", and you are assuming the entire risk as to its quality and
 *  performance.
 *
 *  You are granted license to use this code as a basis for your own
 *  application(s) under the terms of the separate license between you and
 *  Intuwave.
 */


import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;

/**
 * <p>
 * Encapsulates a Lua execution environment.  A lot of Jili's public API
 * manifests as public methods in this class.  A key part of the API is
 * the ability to call Lua functions from Java (ultimately, all Lua code
 * is executed in this manner).
 * </p>
 *
 * <p>
 * The Stack
 * </p>
 *
 * <p>
 * All arguments to Lua functions and all results returned by Lua
 * functions are placed onto a stack.  The stack can be indexed by an
 * integer in the same way as the PUC-Rio implementation.  A positive
 * index is an absolute index and ranges from 1 (the bottom-most
 * element) through to <var>n</var> (the top-most element),
 * where <var>n</var> is the number of elements on the stack.  Negative
 * indexes are relative indexes, -1 is the top-most element, -2 is the
 * element underneath that, and so on.  0 is not used.
 * </p>
 *
 * <p>
 * Note that in Jili the stack is used only for passing arguments and
 * returning results, unlike PUC-Rio.
 * </p>
 *
 * <p>
 * The protocol for calling a function is described in the {@link Lua#call}
 * method.  In brief: push the function onto the stack, then push the
 * arguments to the call.
 * </p>
 *
 * <p>
 * The methods {@link Lua#push}, {@link Lua#pop}, {@link Lua#value},
 * {@link Lua#getTop}, {@link Lua#setTop} are used to manipulate the stack.
 * </p>
 */
public final class Lua {
  /** Version string. */
  public static final String VERSION = "Lua 5.1 (Jili 0.X.Y)";

  /** Table of globals (global variables).  Actually shared across all
   * coroutines. */
  private LuaTable global = new LuaTable();
  /** VM data stack. */
  private Vector stack = new Vector();
  private int base;     // = 0;
  private int nCcalls;  // = 0;
  /** Instruction to resume execution at.  Index into code array. */
  private int savedpc;  // = 0;
  /**
   * Vector of CallInfo records.  Actually it's a Stack which is a
   * subclass of Vector, but it mostly the Vector methods that are used.
   */
  private Stack civ = new Stack();
  /** CallInfo record for currently active function. */
  private CallInfo ci = new CallInfo();
  {
    civ.addElement(ci);
  }
  /** Open Upvalues.  All UpVal objects that reference the VM stack.
   * openupval is a java.util.Vector of UpVal stored in order of stack
   * slot index: higher stack indexes are stored at higher Vector
   * positions.
   */
  private Vector openupval = new Vector();

  /** Number of list items to accumuate before a SETLIST instruction. */
  private static final int LFIELDS_PER_FLUSH = 50;

  /** Limit for table tag-method chains (to avoid loops) */
  private static final int MAXTAGLOOP = 100;

  /**
   * Maximum number of local variables per function.  As per
   * LUAI_MAXVARS from "luaconf.h".  Default access so that {@link
   * FuncState} can see it.
   */
  static final int MAXVARS = 200;
  static final int MAXSTACK = 250;

  /** Used to communicate error status (ERRRUN, etc) from point where
   * error is raised to the code that catches it.
   */
  private int errorStatus;
  /**
   * The current error handler (set by {@link Lua#pcall}).  A Lua
   * function to call.
   */
  private Object errfunc;

  /** Nonce object used by pcall and friends (to detect when an
   * exception is a Lua error). */
  private static final String LUA_ERROR = "";

  /** Metatable for primitive types.  Shared between all coroutines. */
  private LuaTable[] metatable = new LuaTable[NUM_TAGS];

  //////////////////////////////////////////////////////////////////////
  // Public API


  /**
   * Equivalent of LUA_MULTRET.
   */
  // Required, by vmPoscall, to be negative.
  public static final int MULTRET = -1;
  /**
   * The Lua <code>nil</code> value.
   */
  public static final Object NIL = null;

  // Lua type tags, from lua.h
  /** Lua type tag, representing no stack value. */
  public static final int TNONE         = -1;
  /** Lua type tag, representing <code>nil</code>. */
  public static final int TNIL          = 0;
  /** Lua type tag, representing boolean. */
  public static final int TBOOLEAN      = 1;
  // TLIGHTUSERDATA not available.  :todo: make available?
  /** Lua type tag, representing numbers. */
  public static final int TNUMBER       = 3;
  /** Lua type tag, representing strings. */
  public static final int TSTRING       = 4;
  /** Lua type tag, representing tables. */
  public static final int TTABLE        = 5;
  /** Lua type tag, representing functions. */
  public static final int TFUNCTION     = 6;
  /** Lua type tag, representing userdata. */
  public static final int TUSERDATA     = 7;
  /** Lua type tag, representing threads.
  public static final int TTHREAD       = 8;
  /** Number of type tags.  Should correspond to last entry in the list
   * of tags.
   */
  private static final int NUM_TAGS     = 8;
  /** Names for above type tags, starting from {@link Lua#TNIL}.
   * Equivalent to luaT_typenames.
   */
  private static final String[] TYPENAME = {
    "nil", "boolean", "userdata", "number",
    "string", "table", "function", "userdata", "thread"
  };

  /**
   * Minimum stack size that Lua Java functions gets.  May turn out to
   * be silly / redundant.
   */
  public static final int MINSTACK = 20;

  /** Status code, returned from pcall and friends, that indicates the
   * coroutine has yielded.
   */
  public static final int YIELD         = 1;
  /** Status code, returned from pcall and friends, that indicates
   * a runtime error.
   */
  public static final int ERRRUN        = 2;
  /** Status code, returned from pcall and friends, that indicates
   * a syntax error.
   */
  public static final int ERRSYNTAX     = 3;
  /** Status code, returned from pcall and friends, that indicates
   * a memory allocation error.
   */
  public static final int ERRMEM        = 4;
  /** Status code, returned from pcall and friends, that indicates
   * an error whilst running the error handler function.
   */
  public static final int ERRERR        = 5;
  /** Status code, returned from loadFile and friends, that indicates
   * an IO error.
   */
  public static final int ERRFILE       = 6;

  // Enums for gc().
  /** Action, passed to {@link Lua#gc}, that requests the GC to stop. */
  public static final int GCSTOP        = 0;
  /** Action, passed to {@link Lua#gc}, that requests the GC to restart. */
  public static final int GCRESTART     = 1;
  /** Action, passed to {@link Lua#gc}, that requests a full collection. */
  public static final int GCCOLLECT     = 2;
  /** Action, passed to {@link Lua#gc}, that returns amount of memory
   * (in Kibibytes) in use (by the entire Java runtime).
   */
  public static final int GCCOUNT       = 3;
  /** Action, passed to {@link Lua#gc}, that returns the remainder of
   * dividing the amount of memory in use by 1024.
   */
  public static final int GCCOUNTB      = 4;
  /** Action, passed to {@link Lua#gc}, that requests an incremental
   * garbage collection be performed.
   */
  public static final int GCSTEP        = 5;
  /** Action, passed to {@link Lua#gc}, that sets a new value for the
   * <var>pause</var> of the collector.
   */
  public static final int GCSETPAUSE    = 6;
  /** Action, passed to {@link Lua#gc}, that sets a new values for the
   * <var>step multiplier</var> of the collector.
   */
  public static final int GCSETSTEPMUL  = 7;

  /**
   * Calls a Lua value.  Normally this is called on functions, but the
   * semantics of Lua permit calls on any value as long as its metatable
   * permits it.
   *
   * In order to call a function, the function must be
   * pushed onto the stack, then its arguments must be
   * {@link Lua#push pushed} onto the stack; the first argument is pushed
   * directly after the function,
   * then the following arguments are pushed in order (direct
   * order).  The parameter <var>n</var> specifies the number of
   * arguments (which may be 0).
   *
   * When the function returns the function value on the stack and all
   * the arguments are removed from the stack and replaced with the
   * results of the function, adjusted to the number specified by
   * <var>r</var>.  So the first result from the function call will be
   * at the same index where the function was immediately prior to
   * calling this method.
   *
   * @param n  The number of arguments in this function call.
   * @param r  The number of results required.
   */
  public void call(int n, int r) {
    if (n < 0 || n + base > stack.size()) {
      throw new IllegalArgumentException();
    }
    int func = stack.size() - (n + 1);
    this.vmCall(func, r);
  }

  /**
   * Concatenate values (usually strings) on the stack.
   * <var>n</var> values from the top of the stack are concatenated, as
   * strings, and replaced with the resulting string.
   * @param n  the number of values to concatenate.
   */
  public void concat(int n) {
    apiChecknelems(n);
    if (n >= 2) {
      vmConcat(n, (stack.size() - base) - 1);
      pop(n-1);
    } else if (n == 0) {        // push empty string
      push("");
    } // else n == 1; nothing to do
  }

  /**
   * Generates a Lua error using the error message.
   * @param message  the error message.
   * @return never.
   */
  public int error(Object message) {
    return gErrormsg(message);
  }

  /**
   * Control garbage collector.  Note that in Jili most of the options
   * to this function make no sense and they will not do anything.
   * @param what  specifies what GC action to take.
   * @param data  data that may be used by the action.
   * @return varies.
   */
  public int gc(int what, int data) {
    Runtime rt;

    switch (what) {
      case GCSTOP:
        return 0;
      case GCRESTART:
      case GCCOLLECT:
      case GCSTEP:
        System.gc();
        return 0;
      case GCCOUNT:
        rt = Runtime.getRuntime();
        return (int)((rt.totalMemory() - rt.freeMemory()) / 1024);
      case GCCOUNTB:
        rt = Runtime.getRuntime();
        return (int)((rt.totalMemory() - rt.freeMemory()) % 1024);
      case GCSETPAUSE:
      case GCSETSTEPMUL:
        return 0;
    }
    return 0;
  }

  /**
   * Get a field from a table (or other object).
   * @param t      The object whose field to retrieve.
   * @param field  The name of the field.
   * @return  the Lua value
   */
  public Object getField(Object t, String field) {
    return vmGettable(t, field);
  }

  /**
   * Get a global variable.
   * @param name  The name of the global variable.
   * @return  The value of the global variable.
   */
  public Object getGlobal(String name) {
    return vmGettable(global, name);
  }

  /**
   * Gets the global environment.  The global environment, where global
   * variables live, is returned as a <code>LuaTable</code>.  Note that
   * modifying this table has exactly the same effect as creating or
   * changing global variables from within Lua.
   * @return  The global environment as a table.
   */
  public LuaTable getGlobals() {
    return global;
  }

  /** Get metatable.
   * @param o  the Lua value whose metatable to retrieve.
   * @return The metatable, or null if there is no metatable.
   */
  public LuaTable getMetatable(Object o) {
    LuaTable mt;

    if (o instanceof LuaTable) {
      LuaTable t = (LuaTable)o;
      mt = t.getMetatable();
    } else if (o instanceof LuaUserdata) {
      LuaUserdata u = (LuaUserdata)o;
      mt = u.getMetatable();
    } else {
      mt = metatable[type(o)];
    }
    return mt;
  }

  /**
   * Gets the number of elements in the stack.  If the stack is not
   * empty then this is the index of the top-most element.
   * @return number of stack elements.
   */
  public int getTop() {
    return stack.size() - base;
  }

  /**
   * Insert Lua value into stack immediately at specified index.  Values
   * in stack at that index and higher get pushed up.
   * @param o    the Lua value to insert into the stack.
   * @param idx  the stack index at which to insert.
   */
  public void insert(Object o, int idx) {
    idx = absIndex(idx);
    stack.insertElementAt(o, idx);
  }

  /**
   * Tests that an object is a Lua boolean.
   * @param o  the Object to test.
   * @return true if and only if the object is a Lua boolean.
   */
  public static boolean isBoolean(Object o) {
    return o instanceof Boolean;
  }

  /**
   * Tests that an object is a Lua function implementated in Java (a Lua
   * Java Function).
   * @param o  the Object to test.
   * @return true if and only if the object is a Lua Java Function.
   */
  public static boolean isJavaFunction(Object o) {
    return o instanceof LuaJavaCallback;
  }

  /**
   * Tests that an object is a Lua function (implemented in Lua or
   * Java).
   * @param o  the Object to test.
   * @return true if and only if the object is a function.
   */
  public static boolean isFunction(Object o) {
    return o instanceof LuaFunction ||
        o instanceof LuaJavaCallback;
  }

  /**
   * Tests that an object is Lua <code>nil</code>.
   * @param o  the Object to test.
   * @return true if and only if the object is Lua <code>nil</code>.
   */
  public static boolean isNil(Object o) {
    return null == o;
  }

  /**
   * Tests that an object is a Lua number or a string convertible to a
   * number.
   * @param o  the Object to test.
   * @return true if and only if the object is a number or a convertible string.
   */
  public static boolean isNumber(Object o) {
    return tonumber(o, NUMOP);
  }

  /**
   * Tests that an object is a Lua string or a number (which is always
   * convertible to a string).
   * @param o  the Object to test.
   * @return true if and only if object is a string or number.
   */
  public static boolean isString(Object o) {
    return o instanceof String;
  }

  /**
   * Tests that an object is a Lua table.
   * @param o  the Object to test.
   * @return <code>true</code> if and only if the object is a Lua table.
   */
  public static boolean isTable(Object o) {
    return o instanceof LuaTable;
  }

  /**
   * Tests that an object is a Lua thread.
   * @param o  the Object to test.
   * @return <code>true</code> if and only if the object is a Lua thread.
   */
  public static boolean isThread(Object o) {
    // :todo: implement me.
    return false;
  }

  /**
   * Tests that an object is a Lua userdata.
   * @param o  the Object to test.
   * @return true if and only if the object is a Lua userdata.
   */
  public static boolean isUserdata(Object o) {
    return o instanceof LuaUserdata;
  }

  /**
   * <p>
   * Tests that an object is a Lua value.  Returns <code>true</code> for
   * an argument that is a Jili representation of a Lua value,
   * <code>false</code> for Java references that are not Lua values.
   * For example <code>isValue(new LuaTable())</code> is
   * <code>true</code>, but <code>isValue(new Object[] { })</code> is
   * <code>false</code> because Java arrays are not a representation of
   * any Lua value.
   * </p>
   * <p>
   * PUC-Rio Lua provides no
   * counterpart for this method because in their implementation it is
   * impossible to get non Lua values on the stack, whereas in Jili it
   * is common to mix Lua values with ordinary, non Lua, Java objects.
   * </p>
   * @param o  the Object to test.
   * @return true if and if it represents a Lua value.
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
   * <p>
   * Loads a Lua chunk in binary or source form.
   * Comparable to C's lua_load.  If the chunk is determined to be
   * binary then it is loaded directly.  Otherwise the chunk is assumed
   * to be a Lua source chunk and compilation is required first; the
   * <code>InputStream</code> is used to create a <code>Reader</code>
   * (using the
   * {@link java.io.InputStreamReader#InputStreamReader(InputStream)}
   * constructor) and the Lua source is compiled.
   * </p>
   * <p>
   * If successful, The compiled chunk, a Lua function, is pushed onto
   * the stack and a zero status code is returned.  Otherwise a non-zero
   * status code is returned to indicate an error and the error message
   * is pushed onto the stack.
   * </p>
   * @param in         The binary chunk as an InputStream, for example from
   *                   {@link Class#getResourceAsStream}.
   * @param chunkname  The name of the chunk.
   * @return           A status code.
   */
  public int load(InputStream in, String chunkname) {
    push(new LuaInternal(in, chunkname));
    return pcall(0, 1, null);
  }

  /**
   * Loads a Lua chunk in source form.
   * Comparable to C's lua_load.  Since this takes a {@link
   * java.io.Reader} parameter,
   * this method is restricted to loading Lua chunks in source form.
   * In every other respect this method is just like {@link
   * Lua#load(InputStream, String)}.
   * @param in         The source chunk as a Reader, for example from
   *                   <code>java.io.InputStreamReader(Class.getResourceAsStream())</code>.
   * @param chunkname  The name of the chunk.
   * @return           A status code.
   * @see java.io.InputStreamReader
   */
  public int load(Reader in, String chunkname) {
    push(new LuaInternal(in, chunkname));
    return pcall(0, 1, null);
  }

  /**
   * Slowly get the next key from a table.  Unlike most other functions
   * in the API this one uses the stack.  The top-of-stack is popped and
   * used to find the next key in the table at the position specified by
   * index.  If there is a next key then the key and its value are
   * pushed onto the stack and <code>true</code> is returned.
   * Otherwise (the end of the table has been reached)
   * <code>false</code> is returned.
   * @param idx  stack index of table.
   * @return  true if and only if there are more keys in the table.
   * @deprecated Use :todo: iterator protocol instead.
   */
  public boolean next(int idx) {
    Object o = value(idx);
    // :todo: api check
    LuaTable t = (LuaTable)o;
    Object key = value(-1);
    pop(1);
    Enumeration e = t.keys();
    if (key == NIL) {
      if (e.hasMoreElements()) {
        key = e.nextElement();
        push(key);
        push(t.get(key));
        return true;
      }
      return false;
    }
    while (e.hasMoreElements()) {
      Object k = e.nextElement();
      if (k == key) {
        if (e.hasMoreElements()) {
          key = e.nextElement();
          push(key);
          push(t.get(key));
          return true;
        }
        return false;
      }
    }
    // protocol error which we could potentially diagnose.
    return false;
  }

  /**
   * Protected {@link Lua#call}.
   * @param nargs     number of arguments.
   * @param nresults  number of result required.
   * @param ef        error function to call in case of error.
   * @return status code
   */
  public int pcall(int nargs, int nresults, Object ef) {
    apiChecknelems(nargs+1);
    int restoreStack = stack.size() - (nargs + 1);
    // Most of this code comes from luaD_pcall
    int restoreCi = civ.size();
    int oldnCcalls = nCcalls;
    Object old_errfunc = errfunc;
    errfunc = ef;
    // :todo: save and restore allowhooks
    try  {
      errorStatus = 0;
      call(nargs, nresults);
    } catch (RuntimeException e) {
      if (e.getMessage() == LUA_ERROR) {
        fClose(restoreStack);   // close eventual pending closures
        // copy error object (usually a string) down
        stack.setElementAt(stack.lastElement(), restoreStack);
        stack.setSize(restoreStack+1);
        nCcalls = oldnCcalls;
        civ.setSize(restoreCi);
        ci = (CallInfo)civ.lastElement();
        base = ci.base();
        savedpc = ci.savedpc();
      } else {
        throw e;
      }
    }
    errfunc = old_errfunc;
    return errorStatus;
  }

  /**
   * Removes (and discards) the top-most <var>n</var> elements from the stack.
   * @param n  the number of elements to remove.
   */
  public void pop(int n) {
    if (n < 0) {
      throw new IllegalArgumentException();
    }
    stack.setSize(stack.size() - n);
  }

  /**
   * Pushes a value onto the stack in preparation for calling a
   * function (or returning from one).  See {@link Lua#call} for
   * the protocol to be used for calling functions.  See {@link
   * Lua#pushNumber} for pushing numbers, and {@link Lua#pushValue} for
   * pushing a value that is already on the stack.
   * @param o  the Lua value to push.
   */
  public void push(Object o) {
    stack.addElement(o);
  }

  /**
   * Push boolean onto the stack.
   * @param b  the boolean to push.
   */
  public void pushBoolean(boolean b) {
    push(valueOfBoolean(b));
  }

  /**
   * Push literal string onto the stack.
   * @param s  the string to push.
   */
  public void pushLiteral(String s) {
    push(s);
  }

  /** Push nil onto the stack. */
  public void pushNil() {
    push(NIL);
  }

  /**
   * Pushes a number onto the stack.  See also {@link Lua#push}.
   * @param d  the number to push.
   */
  public void pushNumber(double d) {
    push(new Double(d));
  }

  /**
   * Copies a stack element onto the top of the stack.
   * Equivalent to <code>L.push(L.value(idx))</code>.
   * @param idx  stack index of value to push.
   */
  public void pushValue(int idx) {
    push(value(idx));
  }

  /**
   * Implements equality without metamethods.
   * @param o1  the first Lua value to compare.
   * @param o2  the other Lua value.
   * @return  true if and only if they compare equal.
   */
  public static boolean rawEqual(Object o1, Object o2) {
    return oRawequal(o1, o2);
  }

  /**
   * Gets an element from a table, without using metamethods.
   * @param t  The table to access.
   * @param k  The index (key) into the table.
   * @return The value at the specified index.
   */
  public static Object rawGet(Object t, Object k) {
    LuaTable table = (LuaTable)t;
    return table.get(k);
  }

  /**
   * Gets an element from an array, without using metamethods.
   * @param t  the array (table).
   * @param i  the index of the element to retrieve.
   * @return  the value at the specified index.
   */
  public static Object rawGetI(Object t, int i) {
    LuaTable table = (LuaTable)t;
    return table.getnum(i);
  }

  /**
   * Sets an element in a table, without using metamethods.
   * @param t  The table to modify.
   * @param k  The index into the table.
   * @param v  The new value to be stored at index <var>k</var>.
   */
  public static void rawSet(Object t, Object k, Object v) {
    if (k == NIL) {
      throw new NullPointerException();
    }
    LuaTable table = (LuaTable)t;
    table.put(k, v);
  }

  /**
   * Set the environment for a function, thread, or userdata.
   * @param o      Object whose environment will be set.
   * @param table  Environment table to use.
   * @return true if the object had its environment set, false otherwise.
   */
  public boolean setFenv(Object o, Object table) {
    // :todo: consider implementing common env interface for
    // LuaFunction, LuaJavaCallback, LuaUserdata, Lua.  One cast to an
    // interface and an interface method call may be shorter
    // than this mess.
    LuaTable t = (LuaTable)table;

    if (o instanceof LuaFunction) {
      LuaFunction f = (LuaFunction)o;
      f.setEnv(t);
      return true;
    }
    if (o instanceof LuaJavaCallback) {
      LuaJavaCallback f = (LuaJavaCallback)o;
      // :todo: implement this case.
      return false;
    }

    if (o instanceof LuaUserdata) {
      LuaUserdata u = (LuaUserdata)o;
      u.setEnv(t);
      return true;
    }

    if (false) {
      // :todo: implement TTHREAD case;
      return false;
    }
    return false;
  }

  /**
   * Set a field in a Lua value.
   * @param t     Lua value of which to set a field.
   * @param name  Name of field to set.
   * @param v     new Lua value for field.
   */
  public void setField(Object t, String name, Object v) {
    vmSettable(t, name, v);
  }

  /**
   * Sets the metatable for a Lua value.
   * @param o   Lua value of which to set metatable.
   * @param mt  The new metatable.
   */
  public void setMetatable(Object o, Object mt) {
    if (isNil(mt)) {
      mt = null;
    } else {
      apiCheck(mt instanceof LuaTable);
    }
    LuaTable mtt = (LuaTable)mt;
    if (o instanceof LuaTable) {
      LuaTable t = (LuaTable)o;
      t.setMetatable(mtt);
    } else if (o instanceof LuaUserdata) {
      LuaUserdata u = (LuaUserdata)o;
      u.setMetatable(mtt);
    } else {
      metatable[type(o)] = mtt;
    }
  }

  /**
   * Set a global variable.
   * @param name   name of the global variable to set.
   * @param value  desired new value for the variable.
   */
  public void setGlobal(String name, Object value) {
    vmSettable(global, name, value);
  }

  /**
   * Set the stack top.
   * @param n  the desired size of the stack (in elements).
   */
  public void setTop(int n) {
    if (n < 0) {
      throw new IllegalArgumentException();
    }
    stack.setSize(base+n);
  }

  /**
   * Convert to boolean.
   * @param o  Lua value to convert.
   * @return  the resulting primitive boolean.
   */
  public boolean toBoolean(Object o) {
    return !(o == NIL || Boolean.FALSE.equals(o));
  }

  /**
   * Convert to integer and return it.  Returns 0 if cannot be
   * converted.
   * @param o  Lua value to convert.
   * @return  the resulting int.
   */
  public int toInteger(Object o) {
    if (tonumber(o, NUMOP)) {
      return (int)NUMOP[0];
    }
    return 0;
  }

  /**
   * Convert to number and return it.  Returns 0 if cannot be
   * converted.
   * @param o  Lua value to convert.
   * @return  The resulting number.
   */
  public double toNumber(Object o) {
    if (tonumber(o, NUMOP)) {
      return NUMOP[0];
    }
    return 0;
  }

  /**
   * Convert to string and return it.  If value cannot be converted then
   * null is returned.  Note that unlike <code>lua_tostring</code> this
   * does not modify the Lua value.
   * @param o  Lua value to convert.
   * @return  The resulting string.
   */
  public String toString(Object o) {
    return vmTostring(o);
  }

  /**
   * Type of the Lua value at the specified stack index.
   * @param idx  stack index to type.
   * @return  the type, or {@link Lua#TNONE} if there is no value at <var>idx</var>
   */
  public int type(int idx) {
    idx = absIndex(idx);
    if (idx < 0) {
      return TNONE;
    }
    Object o = stack.elementAt(idx);
    return type(o);
  }

  /**
   * Type of a Lua value.
   * @param o  the Lua value whose type to return.
   * @return  the Lua type from an enumeration.
   */
  public int type(Object o) {
    if (o == NIL) {
      return TNIL;
    } else if (o instanceof Double) {
      return TNUMBER;
    } else if (o instanceof Boolean) {
      return TBOOLEAN;
    } else if (o instanceof String) {
      return TSTRING;
    } else if (o instanceof LuaTable) {
      return TTABLE;
    } else if (o instanceof LuaFunction || o instanceof LuaJavaCallback) {
      return TFUNCTION;
    } else if (o instanceof LuaUserdata) {
      return TUSERDATA;
    }
    // :todo: thread
    return TNONE;
  }

  /**
   * Name of type.
   * @param type  a Lua type from, for example, {@link Lua#type}.
   * @return  the type's name.
   */
  public String typeName(int type) {
    if (TNONE == type) {
      return "no value";
    }
    return TYPENAME[type];
  }

  /**
   * Gets a value from the stack.
   * If <var>idx</var> is positive and exceeds
   * the size of the stack, {@link Lua#NIL} is returned.
   * @param idx  the stack index of the value to retrieve.
   * @return  the Lua value from the stack.
   */
  public Object value(int idx) {
    idx = absIndex(idx);
    if (idx < 0) {
      return NIL;
    }
    return stack.elementAt(idx);
  }

  /**
   * Converts primitive boolean into a Lua value.
   * @param b  the boolean to convert.
   * @return  the resulting Lua value.
   */
  public static Object valueOfBoolean(boolean b) {
     // If CLDC 1.1 had
     // <code>java.lang.Boolean.valueOf(boolean);</code> then I probably
     // wouldn't have written this.  This does have a small advantage:
     // code that uses this method does not need to assume that Lua booleans in
     // Jili are represented using Java.lang.Boolean.
    if (b) {
      return Boolean.TRUE;
    } else {
      return Boolean.FALSE;
    }
  }

  /**
   * Converts primitive number into a Lua value.
   * @param d  the number to convert.
   * @return  the resulting Lua value.
   */
  public static Object valueOfNumber(double d) {
    // :todo: consider interning "common" numbers, like 0, 1, -1, etc.
    return new Double(d);
  }

  // Miscellaneous private functions.

  /** Convert from Java API stack index to absolute index.
   * @return an index into <code>this.stack</code> or -1 if out of range.
   */
  int absIndex(int idx) {
    int s = stack.size();

    if (idx == 0) {
      return -1;
    }
    if (idx > 0) {
      if (idx + base > s) {
        return -1;
      }
      return base + idx - 1;
    }
    // idx < 0
    if (s + idx < base) {
      return -1;
    }
    return s + idx;
  }


  //////////////////////////////////////////////////////////////////////
  // Auxiliary API

  // :todo: consider placing in separate class (or macroised) so that we
  // can change its definition (to remove the check for example).
  private void apiCheck(boolean cond) {
    if (!cond) {
      throw new IllegalArgumentException();
    }
  }

  private void apiChecknelems(int n) {
    apiCheck(n <= stack.size() - base);
  }

  /**
   * Checks a general condition and raises error if false.
   * @param cond      the (evaluated) condition to check.
   * @param numarg    argument index.
   * @param extramsg  extra error message to append.
   */
  public void argCheck(boolean cond, int numarg, String extramsg) {
    if (cond) {
      return;
    }
    argError(numarg, extramsg);
  }

  /**
   * Raise a general error for an argument.
   * @param narg      argument index.
   * @param extramsg  extra message string to append.
   * @return never (used idiomatically in <code>return argError(...)</code>)
   */
  public int argError(int narg, String extramsg) {
    // :todo: use debug API as per PUC-Rio
    if (true) {
      return error("bad argument " + narg + " (" + extramsg + ")");
    }
    return 0;
  }

  /**
   * Calls a metamethod.  Pushes 1 result onto stack if method called.
   * @param obj    stack index of object whose metamethod to call
   * @param event  metamethod (event) name.
   * @return  true if and only if metamethod was found and called.
   */
  public boolean callMeta(int obj, String event) {
    Object o = value(obj);
    Object ev = getMetafield(o, event);
    if (ev == null) {
      return false;
    }
    push(ev);
    push(o);
    call(1, 1);
    return true;
  }

  /**
   * Checks that an argument is present (can be anything).
   * Raises error if not.
   * @param narg  argument index.
   */
  public void checkAny(int narg) {
    if (type(narg) == TNONE) {
      argError(narg, "value expected");
    }
  }

  /**
   * Checks is a number and returns it as an integer.  Raises error if
   * not a number.
   * @param narg  argument index.
   * @return  the argument as an int.
   */
  public int checkInt(int narg) {
    Object o = value(narg);
    int d = toInteger(o);
    if (d == 0 && !isNumber(o)) {
      tagError(narg, TNUMBER);
    }
    return d;
  }

  /**
   * Checks that an optional string argument is an element from a set of
   * strings.  Raises error if not.
   * @param narg  argument index.
   * @param def   default string to use if argument not present.
   * @param lst   the set of strings to match against.
   * @return an index into <var>lst</var> specifying the matching string.
   */
  public int checkOption(int narg, String def, String[] lst) {
    String name;

    if (def == null) {
      name = checkString(narg);
    } else {
      name = optString(narg, def);
    }
    for (int i=0; i<lst.length; ++i) {
      if (lst[i].equals(name)) {
        return i;
      }
    }
    return argError(narg, "invalid option '" + name + "'");
  }

  /**
   * Checks argument is a string and returns it.  Raises error if not a
   * string.
   * @param narg  argument index.
   * @return  the argument as a string.
   */
  public String checkString(int narg) {
    String s = toString(value(narg));
    if (s == null) {
      tagError(narg, TSTRING);
    }
    return s;
  }

  /**
   * Checks the type of an argument, raises error if not matching.
   * @param narg  argument index.
   * @param t     typecode (from {@link Lua#type} for example).
   */
  public void checkType(int narg, int t) {
    if (type(narg) != t) {
      tagError(narg, t);
    }
  }

  /**
   * Loads and runs the given string.
   * @param s  the string to run.
   * @return  a status code, as per {@link Lua#load}.
   */
  public int doString(String s) {
    int status = load(Lua.stringReader(s), s);
    if (status == 0) {
      status = pcall(0, MULTRET, null);
    }
    return status;
  }

  private int errfile(String what, String fname, Exception e) {
    push("cannot " + what + " " + fname + ": " + e.toString());
    return ERRFILE;
  }

  /**
   * Get a field (event) from an Lua value's metatable.  Returns null
   * if there is no field nor metatable.
   * @param o           Lua value to get metafield for.
   * @param event       name of metafield (event).
   * @return            the field from the metatable, or null.
   */
  public Object getMetafield(Object o, String event) {
    LuaTable mt = getMetatable(o);
    if (mt == null) {
      return null;
    }
    return mt.get(event);
  }

  private boolean isnoneornil(int narg) {
    return type(narg) <= TNIL;
  }

  /**
   * Loads a Lua chunk from a file.  The <var>filename</var> argument is
   * used in a call to {@link Class#getResourceAsStream} where
   * <code>this</code> is the {@link Lua} instance, thus relative
   * pathnames will be relative to the location of the
   * <code>Lua.class</code> file.  Pushes compiled chunk, or error
   * message, onto stack.
   * @param filename  location of file.
   * @return status code, as per {@link Lua#load}.
   */
  public int loadFile(String filename) {
    if (filename == null) {
      throw new NullPointerException();
    }
    InputStream in = getClass().getResourceAsStream(filename);
    if (in == null) {
      return errfile("open", filename, new IOException());
    }
    int status = 0;
    try {
      in.mark(1);
      int c = in.read();
      if (c == '#') {     // Unix exec. file?
        // :todo: handle this case
      }
      in.reset();
      status = load(in, "@" + filename);
    } catch (IOException e) {
      return errfile("read", filename, e);
    }
    return status;
  }

  /**
   * Loads a Lua chunk from a string.  Pushes compiled chunk, or error
   * message, onto stack.
   * @param s           the string to load.
   * @param chunkname   the name of the chunk.
   * @return status code, as per {@link Lua#load}.
   */
  public int loadString(String s, String chunkname) {
    return load(stringReader(s), chunkname);
  }

  /**
   * Get optional integer argument.  Raises error if non-number
   * supplied.
   * @param narg  argument index.
   * @param def   default value for integer.
   * @return an int.
   */
  public int optInt(int narg, int def) {
    if (isnoneornil(narg)) {
      return def;
    }
    return checkInt(narg);
  }

  /**
   * Get optional string argument.  Raises error if non-string supplied.
   * @param narg  argument index.
   * @param def   default value for string.
   * @return a string.
   */
  public String optString(int narg, String def) {
    if (isnoneornil(narg)) {
      return def;
    }
    return checkString(narg);
  }

  private void tagError(int narg, int tag) {
    typerror(narg, typeName(tag));
  }

  /**
   * Name of type of value at <var>idx</var>.
   * @param idx  stack index.
   * @return  the name of the value's type.
   */
  public String typeNameOfIndex(int idx) {
    return TYPENAME[type(idx)];
  }

  /**
   * Declare type error in argument.
   * @param narg   Index of argument.
   * @param tname  Name of type expected.
   */
  public void typerror(int narg, String tname) {
    argError(narg, tname + " expected, got " + typeNameOfIndex(narg));
  }

  /**
   * Return string identifying current position of the control at level
   * <var>level</var>.
   * @param level  specifies the call-stack level.
   * @return a description for that level.
   */
  public String where(int level) {
    Debug ar = getStack(level);         // check function at level
    if (ar != null) {
      getInfo("Sl", ar);                // get info about it
      if (ar.currentline() > 0) {       // is there info?
        return ar.short_src() + ":" + ar.currentline() + ": ";
      }
    }
    return "";  // else, no information available...
  }

  /**
   * Provide {@link java.ioReader} interface over a <code>String</code>.
   * Equivalent of {@link java.io.StringReader#StringReader} from J2SE.
   * The ability to convert a <code>String</code> to a
   * <code>Reader</code> is required internally,
   * to provide the Lua function <code>loadstring</code>; exposed
   * externally as a convenience.
   * @param s  the string from which to read.
   * @return a {@link java.ioReader} that reads successive chars from <var>s</var>.
   */
  public static Reader stringReader(String s) {
    return new StringReader(s);
  }

  //////////////////////////////////////////////////////////////////////
  // Debug

  // Methods equivalent to debug API.  In PUC-Rio most of these are in
  // ldebug.c

  private boolean getInfo(String what, Debug ar) {
    Object f = null;
    CallInfo callinfo = null;
    // :todo: complete me
    if (ar.i_ci() > 0) {        // no tail call?
      callinfo = (CallInfo)civ.elementAt(ar.i_ci());
      f = stack.elementAt(callinfo.function());
      // assert isFunction(f);
    }
    return auxgetinfo(what, ar, f, callinfo);
  }

  /**
   * Locates function activation at specified call level and returns a
   * {@link Debug}
   * record for it, or <code>null</code> if level is too high.
   * May become public.
   * @param level  the call level.
   * @return a {@link Debug} instance describing the activation record.
   */
  private Debug getStack(int level) {
    int ici;    // Index of CallInfo

    for (ici=civ.size()-1; level > 0 && ici > 0; --ici) {
      CallInfo ci = (CallInfo)civ.elementAt(ici);
      --level;
      if (isLua(ci)) {                  // Lua function?
        level -= ci.tailcalls();        // skip lost tail calls
      }
    }
    if (level == 0 && ici > 0) {        // level found?
      return new Debug(ici);
    } else if (level < 0) {     // level is of a lost tail call?
      return new Debug(0);
    }
    return null;
  }

  /**
   * @return true is okay, false otherwise (for example, error).
   */
  private boolean auxgetinfo(String what, Debug ar, Object f, CallInfo ci) {
    boolean status = true;
    if (f == null) {
      // :todo: implement me
      return status;
    }
    for (int i=0; i<what.length(); ++i) {
      switch (what.charAt(i)) {
        case 'S':
          funcinfo(ar, f);
          break;
        case 'l':
          ar.setCurrentline((ci != null) ? currentline(ci) : -1);
          break;
        // :todo: more cases.
        default:
          status = false;
      }
    }
    return status;
  }

  private int currentline(CallInfo ci) {
    int pc = currentpc(ci);
    if (pc < 0) {
      return -1;        // only active Lua functions have current-line info
    } else {
      Object faso = stack.elementAt(ci.function());
      LuaFunction f = (LuaFunction)faso;
      return f.proto().getline(pc);
    }
  }

  private int currentpc(CallInfo ci) {
    if (!isLua(ci)) {   // function is not a Lua function?
      return -1;
    }
    if (ci == this.ci) {
      ci.setSavedpc(savedpc);
    }
    return pcRel(ci.savedpc());
  }

  private void funcinfo(Debug ar, Object cl) {
    if (cl instanceof LuaJavaCallback) {
      ar.setSource("=[Java]");
      ar.setLinedefined(-1);
      ar.setLastlinedefined(-1);
      ar.setWhat("Java");
    } else {
      Proto p = ((LuaFunction)cl).proto();
      ar.setSource(p.source());
      ar.setLinedefined(p.linedefined());
      ar.setLastlinedefined(p.lastlinedefined());
      ar.setWhat(ar.linedefined() == 0 ? "main" : "Lua");
    }
  }

  /** Equivalent to macro isLua _and_ f_isLua from lstate.h. */
  private boolean isLua(CallInfo callinfo) {
    Object f = stack.elementAt(callinfo.function());
    return f instanceof LuaFunction;
  }

  private static int pcRel(int pc) {
    return pc - 1;
  }

  //////////////////////////////////////////////////////////////////////
  // Do

  // Methods equivalent to the file ldo.c.  Prefixed with d.
  // Some of these are in vm* instead.

  void dThrow(int status) {
    errorStatus = status;
    throw new RuntimeException(LUA_ERROR);
  }


  //////////////////////////////////////////////////////////////////////
  // Func

  // Methods equivalent to the file lfunc.c.  Prefixed with f.

  /** Equivalent of luaF_close.  All open upvalues referencing stack
   * slots level or higher are closed.
   * @param level  Absolute stack index.
   */
  void fClose(int level) {
    int i = openupval.size();
    while (--i >= 0) {
      UpVal uv = (UpVal)openupval.elementAt(i);
      if (uv.offset() < level) {
        break;
      }
      uv.close();
    }
    openupval.setSize(i+1);
    return;
  }

  UpVal fFindupval(int idx) {
    /*
     * We search from the end of the Vector towards the beginning,
     * looking for an UpVal for the required stack-slot.
     */
    int i = openupval.size();
    while (--i >= 0) {
      UpVal uv = (UpVal)openupval.elementAt(i);
      if (uv.offset() == idx) {
        return uv;
      }
      if (uv.offset() < idx) {
        break;
      }
    }
    // i points to be position _after_ which we want to insert a new
    // UpVal (it's -1 when we want to insert at the beginning).
    UpVal uv = new UpVal(stack, idx);
    openupval.insertElementAt(uv, i+1);
    return uv;
  }


  //////////////////////////////////////////////////////////////////////
  // Debug

  // Methods equivalent to the file ldebug.c.  Prefixed with g.

  /** p1 and p2 are absolute stack indexes. */
  private void gConcaterror(int p1, int p2) {
    if (stack.elementAt(p1) instanceof String) {
      p1 = p2;
    }
    // assert !(p1 instanceof String);
    gTypeerror(stack.elementAt(p1), "concatenate");
  }

  static void gCheckcode(Proto p) {
    // :todo: implement me.
  }

  private int gErrormsg(Object message) {
    push(message);
    if (errfunc != null) {      // is there an error handling function
      if (!isFunction(errfunc)) {
        dThrow(ERRERR);
      }
      insert(errfunc, getTop());        // push function (under error arg)
      vmCall(stack.size()-2, 1);        // call it
    }
    dThrow(ERRRUN);
    // NOTREACHED
    return 0;
  }

  private void gRunerror(String s) {
    gErrormsg(s);
  }

  private void gTypeerror(Object o, String op) {
    // :todo: PUC-Rio searches the stack to see if the value (which may
    // be a reference to stack cell) is a local variable.  Jili can't do
    // that so easily.  Consider changing interface.
    String t = typeName(type(o));
    gRunerror("attempt to " + op + " a " + t + " value");
  }


  //////////////////////////////////////////////////////////////////////
  // Object

  // Methods equivalent to the file lobject.c.  Prefixed with o.

  private static final int IDSIZE = 60;
  /**
   * @return a string no longer than IDSIZE.
   */
  static String oChunkid(String source) {
    int len = IDSIZE;
    if (source.startsWith("=")) {
      if(source.length() < IDSIZE+1) {
        return source.substring(1);
      } else {
        return source.substring(1, 1+len);
      }
    }
    // else  "source" or "...source"
    if (source.startsWith("@")) {
      len -= " '...' ".length();
      int l = source.length();
      if (l > len) {
        return "..." +  // get last part of file name
            source.substring(source.length()-len, source.length());
      }
      return source;
    }
    // else  [string "string"]
    int l = source.indexOf('\n');
    if (l == -1) {
      l = source.length();
    }
    len -= " [string \"...\"] ".length();
    if (l > len) {
      l = len;
    }
    StringBuffer buf = new StringBuffer();
    buf.append("[string \"");
    buf.append(source.substring(0, l));
    if (source.length() > l) {  // must truncate
      buf.append("...");
    }
    buf.append("\"]");
    return buf.toString();
  }

  /** Equivalent to luaO_rawequalObj. */
  private static boolean oRawequal(Object a, Object b) {
    // see also vmEqual
    if (NIL == a) {
      return NIL == b;
    }
    // Now a is not null, so a.equals() is a valid call.
    // Numbers (Doubles), Booleans, Strings all get compared by value,
    // as they should; tables, functions, get compared by identity as
    // they should.
    return a.equals(b);
  }

  /** Equivalent to luaO_str2d. */
  private static boolean oStr2d(String s, double[] out) {
    // :todo: using try/catch may be too slow.  In which case we'll have
    // to recognise the valid formats first.
    try {
      out[0] = Double.parseDouble(s);
      return true;
    } catch (NumberFormatException e0_) {
      try {
        // Attempt hexadecimal conversion.
        // :todo: using String.trim is not strictly accurate, because it
        // trims other ASCII control characters as well as whitespace.
        s = s.trim().toUpperCase();
        if (s.startsWith("0X")) {
          s = s.substring(2);
        } else if (s.startsWith("-0X")) {
          s = "-" + s.substring(3);
        }
        out[0] = Integer.parseInt(s, 16);
        return true;
      } catch (NumberFormatException e1_) {
        return false;
      }
    }
  }


  ////////////////////////////////////////////////////////////////////////
  // VM

  // Most of the methods in this section are equivalent to the files
  // lvm.c and ldo.c from PUC-Rio.  They're mostly prefixed with vm as
  // well.

  private static final int PCRLUA =     0;
  private static final int PCRJ =       1;
  private static final int PCRYIELD =   2;

  // Instruction decomposition.

  // There follows a series of methods that extract the various fields
  // from a VM instruction.  See lopcodes.h from PUC-Rio.
  // :todo: Consider replacing with m4 macros (or similar).
  // A brief overview of the instruction format:
  // Logically an instruction has an opcode (6 bits), op, and up to
  // three fields using one of three formats:
  // A B C  (8 bits, 9 bits, 9 bits)
  // A Bx   (8 bits, 18 bits)
  // A sBx  (8 bits, 18 bits signed - excess K)
  // Some instructions do not use all the fields (EG OP_UNM only uses A
  // and B).
  // When packed into a word (an int in Jili) the following layouts are
  // used:
  //  31 (MSB)    23 22          14 13         6 5      0 (LSB)
  // +--------------+--------------+------------+--------+
  // | B            | C            | A          | OPCODE |
  // +--------------+--------------+------------+--------+
  //
  // +--------------+--------------+------------+--------+
  // | Bx                          | A          | OPCODE |
  // +--------------+--------------+------------+--------+
  //
  // +--------------+--------------+------------+--------+
  // | sBx                         | A          | OPCODE |
  // +--------------+--------------+------------+--------+

  static final int NO_REG = 0xff;       // SIZE_A == 8, (1 << 8)-1

  // Hardwired values for speed.
  /** Equivalent of macro GET_OPCODE */
  static int OPCODE(int instruction) {
    // POS_OP == 0 (shift amount)
    // SIZE_OP == 6 (opcode width)
    return instruction & 0x3f;
  }
  /** Equivalent of macro GETARG_A */
  static int ARGA(int instruction) {
    // POS_A == POS_OP + SIZE_OP == 6 (shift amount)
    // SIZE_A == 8 (operand width)
    return (instruction >>> 6) & 0xff;
  }

  static int SETARG_A(int i, int u) {
    return (i & ~(0xff << 6)) | ((u & 0xff) << 6);
  }

  /** Equivalent of macro GETARG_B */
  static int ARGB(int instruction) {
    // POS_B == POS_OP + SIZE_OP + SIZE_A + SIZE_C == 23 (shift amount)
    // SIZE_B == 9 (operand width)
    /* No mask required as field occupies the most significant bits of a
     * 32-bit int. */
    return (instruction >>> 23);
  }

  static int SETARG_B(int i, int b) {
    return (i & ~(0x1ff << 23)) | ((b & 0x1ff) << 23);
  }

  /** Equivalent of macro GETARG_C */
  static int ARGC(int instruction) {
    // POS_C == POS_OP + SIZE_OP + SIZE_A == 14 (shift amount)
    // SIZE_C == 9 (operand width)
    return (instruction >>> 14) & 0x1ff;
  }

  static int SETARG_C(int i, int c) {
    return (i & ~(0x1ff << 14)) | ((c & 0x1ff) << 14);
  }

  /** Equivalent of macro GETARG_Bx */
  static int ARGBx(int instruction) {
    // POS_Bx = POS_C == 14
    // SIZE_Bx == SIZE_C + SIZE_B == 18
    /* No mask required as field occupies the most significant bits of a
     * 32 bit int. */
    return (instruction >>> 14);
  }

  static int SETARG_Bx(int i, int bx) {
    return (i & 0x3fff) | (bx << 14) ;
  }


  /** Equivalent of macro GETARG_sBx */
  static int ARGsBx(int instruction) {
    // As ARGBx but with (2**17-1) subtracted.
    return (instruction >>> 14) - ((1<<17)-1);
  }

  static int SETARG_sBx(int i, int bx) {
    return (i & 0x3fff) | (bx << 14) ;  // CHECK THIS IS RIGHT
  }

  static boolean ISK(int field) {
    // The "is constant" bit position depends on the size of the B and C
    // fields (required to be the same width).
    // SIZE_B == 9
    return field >= 0x100;
  }

  /**
   * Near equivalent of macros RKB and RKC.  Note: non-static as it
   * requires stack and base instance members.  Stands for "Register or
   * Konstant" by the way, it gets value from either the register file
   * (stack) or the constant array (k).
   */
  private Object RK(Object[] k, int field) {
    if (ISK(field)) {
      return k[field & 0xff];
    }
    return stack.elementAt(base + field);
  }

  // CREATE functions are required by FuncState, so default access.
  static int CREATE_ABC(int o, int a, int b, int c) {
    // POS_OP == 0
    // POS_A == 6
    // POS_B == 23
    // POS_C == 14
    return o | (a << 6) | (b << 23) | (c << 14);
  }

  static int CREATE_ABx(int o, int a, int bc) {
    // POS_OP == 0
    // POS_A == 6
    // POS_Bx == POS_C == 14
    return o | (a << 6) | (bc << 14);
  }

  // opcode enumeration.
  // Generated by a script:
  // awk -f opcode.awk < lopcodes.h
  // and then pasted into here.
  // Made default access so that code generation, in FuncState, can see
  // the enumeration as well.

  static final int OP_MOVE = 0;
  static final int OP_LOADK = 1;
  static final int OP_LOADBOOL = 2;
  static final int OP_LOADNIL = 3;
  static final int OP_GETUPVAL = 4;
  static final int OP_GETGLOBAL = 5;
  static final int OP_GETTABLE = 6;
  static final int OP_SETGLOBAL = 7;
  static final int OP_SETUPVAL = 8;
  static final int OP_SETTABLE = 9;
  static final int OP_NEWTABLE = 10;
  static final int OP_SELF = 11;
  static final int OP_ADD = 12;
  static final int OP_SUB = 13;
  static final int OP_MUL = 14;
  static final int OP_DIV = 15;
  static final int OP_MOD = 16;
  static final int OP_POW = 17;
  static final int OP_UNM = 18;
  static final int OP_NOT = 19;
  static final int OP_LEN = 20;
  static final int OP_CONCAT = 21;
  static final int OP_JMP = 22;
  static final int OP_EQ = 23;
  static final int OP_LT = 24;
  static final int OP_LE = 25;
  static final int OP_TEST = 26;
  static final int OP_TESTSET = 27;
  static final int OP_CALL = 28;
  static final int OP_TAILCALL = 29;
  static final int OP_RETURN = 30;
  static final int OP_FORLOOP = 31;
  static final int OP_FORPREP = 32;
  static final int OP_TFORLOOP = 33;
  static final int OP_SETLIST = 34;
  static final int OP_CLOSE = 35;
  static final int OP_CLOSURE = 36;
  static final int OP_VARARG = 37;

  // end of instruction decomposition

  static final int SIZE_C = 9;
  static final int SIZE_B = 9;
  static final int SIZE_Bx = SIZE_C + SIZE_B;
  static final int SIZE_A = 8;

  static final int SIZE_OP = 6;

  static final int POS_OP = 0;
  static final int POS_A = POS_OP + SIZE_OP;
  static final int POS_C = POS_A + SIZE_A;
  static final int POS_B = POS_C + SIZE_C;
  static final int POS_Bx = POS_C;

  static final int MAXARG_Bx = (1<<SIZE_Bx)-1;
  static final int MAXARG_sBx = MAXARG_Bx>>1;    // `sBx' is signed


  static final int MAXARG_A = (1<<SIZE_A)-1;
  static final int MAXARG_B = (1<<SIZE_B)-1;
  static final int MAXARG_C = (1<<SIZE_C)-1;

  /* this bit 1 means constant (0 means register) */
  static final int BITRK = 1 << (SIZE_B - 1) ;
  static final int MAXINDEXRK = BITRK - 1 ;



  /**
   * Equivalent of luaD_call.
   * @param func  absolute stack index of function to call.
   * @param r     number of required results.
   */
  private void vmCall(int func, int r) {
    ++nCcalls;
    if (vmPrecall(func, r) == PCRLUA) {
      vmExecute(1);
    }
    --nCcalls;
  }

  /** Equivalent of luaV_concat. */
  private void vmConcat(int total, int last) {
    do {
      int top = base + last + 1;
      int n = 2;  // number of elements handled in this pass (at least 2)
      if (!tostring(top-2)|| !tostring(top-1)) {
        // :todo: try metamethods
        gConcaterror(top-2, top-1);
        throw new IllegalArgumentException();
      } else if (((String)stack.elementAt(top-1)).length() > 0) {
        int tl = ((String)stack.elementAt(top-1)).length();
        for (n = 1; n < total && tostring(top-n-1); ++n) {
          tl += ((String)stack.elementAt(top-n-1)).length();
          if (tl < 0) {
            gRunerror("string length overflow");
          }
        }
        StringBuffer buffer = new StringBuffer(tl);
        for (int i=n; i>0; i--) {       // concat all strings
          buffer.append(stack.elementAt(top-i));
        }
        stack.setElementAt(buffer.toString(), top-n);
      }
      total -= n-1;     // got n strings to create 1 new
      last -= n-1;
    } while (total > 1); // repeat until only 1 result left
  }

  /**
   * Primitive for testing Lua equality of two values.  Equivalent of
   * PUC-Rio's equalobj macro.  Note that using null to model nil
   * complicates this test, maybe we should use a nonce object.
   */
  private boolean vmEqual(Object a, Object b) {
    if (NIL == a) {
      return NIL == b;
    }
    // Now a is not null, so a.equals() is a valid call.
    if (a.equals(b)) {
      return true;
    }
    if (NIL == b) {
      return false;
    }
    // Now b is not null, so b.getClass() is a valid call.
    if (a.getClass() != b.getClass()) {
      return false;
    }
    // Same class, but different objects.  Resort to metamethods.
    // :todo: metamethods.
    return false;
  }

  /**
   * Array of numeric operands.  Used when converting strings to numbers
   * by an arithmetic opcode (ADD, SUB, MUL, DIV, MOD, POW, UNM).
   */
  private static final double[] NUMOP = new double[2];

  /** The core VM execution engine. */
  private void vmExecute(int nexeccalls) {
    // This labelled while loop is used to simulate the effect of C's
    // goto.  The end of the while loop is never reached.  The beginning
    // of the while loop is branched to using a "continue reentry;"
    // statement (when a Lua function is called or returns).
reentry:
    while (true) {
      // assert stack.elementAt[ci.function()] instanceof LuaFunction;
      LuaFunction function = (LuaFunction)stack.elementAt(ci.function());
      Proto proto = function.proto();
      int[] code = proto.code();
      Object[] k = proto.constant();
      int pc = savedpc;

      while (true) {      // main loop of interpreter

        // Where the PUC-Rio code used the Protect macro, this has been
        // replaced with "savedpc = pc" and a "// Protect" comment.

        // Where the PUC-Rio code used the dojump macro, this has been
        // replaced with the equivalent increment of the pc and a
        // "//dojump" comment.

        int i = code[pc++];       // VM instruction.
        // :todo: count and line hook
        int a = ARGA(i);          // its A field.
        Object rb;
        Object rc;

        switch (OPCODE(i)) {
          case OP_MOVE:
            stack.setElementAt(stack.elementAt(base+ARGB(i)), base+a);
            continue;
          case OP_LOADK:
            stack.setElementAt(k[ARGBx(i)], base+a);
            continue;
          case OP_LOADBOOL:
            stack.setElementAt(valueOfBoolean(ARGB(i) != 0), base+a);
            if (ARGC(i) != 0) {
              ++pc;
            }
            continue;
          case OP_LOADNIL: {
            int b = base + ARGB(i);
            do {
              stack.setElementAt(NIL, b--);
            } while (b >= base + a);
            continue;
          }
          case OP_GETUPVAL: {
            int b = ARGB(i);
            stack.setElementAt(function.upVal(b).getValue(), base+a);
            continue;
          }
          case OP_GETGLOBAL:
            rb = k[ARGBx(i)];
            // assert rb instance of String;
            savedpc = pc; // Protect
            stack.setElementAt(vmGettable(function.getEnv(), rb), base+a);
            continue;
          case OP_GETTABLE: {
            savedpc = pc; // Protect
            Object t = stack.elementAt(base+ARGB(i));
            stack.setElementAt(vmGettable(t, RK(k, ARGC(i))), base+a);
            continue;
          }
          case OP_SETUPVAL: {
            UpVal uv = function.upVal(ARGB(i));
            uv.setValue(stack.elementAt(base+a));
            continue;
          }
          case OP_SETGLOBAL:
            savedpc = pc; // Protect
            vmSettable(function.getEnv(), k[ARGBx(i)],
                stack.elementAt(base+a));
            continue;
          case OP_SETTABLE: {
            savedpc = pc; // Protect
            Object t = stack.elementAt(base+a);
            vmSettable(t, RK(k, ARGB(i)), RK(k, ARGC(i)));
            continue;
          }
          case OP_NEWTABLE: {
            // :todo: use the b and c hints, currently ignored.
            int b = ARGB(i);
            int c = ARGC(i);
            stack.setElementAt(new LuaTable(), base+a);
            continue;
          }
          case OP_SELF: {
            int b = ARGB(i);
            rb = stack.elementAt(base+b);
            stack.setElementAt(rb, base+a+1);
            savedpc = pc; // Protect
            stack.setElementAt(vmGettable(rb, RK(k, ARGC(i))), base+a);
            continue;
          }
          case OP_ADD:
            rb = RK(k, ARGB(i));
            rc = RK(k, ARGC(i));
            if (rb instanceof Double && rc instanceof Double) {
              double sum = ((Double)rb).doubleValue() +
                  ((Double)rc).doubleValue();
              stack.setElementAt(valueOfNumber(sum), base+a);
            } else if (toNumberPair(rb, rc, NUMOP)) {
              double sum = NUMOP[0] + NUMOP[1];
              stack.setElementAt(valueOfNumber(sum), base+a);
            } else {
              // :todo: use metamethod
              throw new IllegalArgumentException();
            }
            continue;
          case OP_SUB:
            rb = RK(k, ARGB(i));
            rc = RK(k, ARGC(i));
            if (rb instanceof Double && rc instanceof Double) {
              double difference = ((Double)rb).doubleValue() -
                  ((Double)rc).doubleValue();
              stack.setElementAt(valueOfNumber(difference), base+a);
            } else if (toNumberPair(rb, rc, NUMOP)) {
              double difference = NUMOP[0] - NUMOP[1];
              stack.setElementAt(valueOfNumber(difference), base+a);
            } else {
              // :todo: use metamethod
              throw new IllegalArgumentException();
            }
            continue;
          case OP_MUL:
            rb = RK(k, ARGB(i));
            rc = RK(k, ARGC(i));
            if (rb instanceof Double && rc instanceof Double) {
              double product = ((Double)rb).doubleValue() *
                ((Double)rc).doubleValue();
              stack.setElementAt(valueOfNumber(product), base+a);
            } else if (toNumberPair(rb, rc, NUMOP)) {
              double product = NUMOP[0] * NUMOP[1];
              stack.setElementAt(valueOfNumber(product), base+a);
            } else {
              // :todo: use metamethod
              throw new IllegalArgumentException();
            }
            continue;
          case OP_DIV:
            rb = RK(k, ARGB(i));
            rc = RK(k, ARGC(i));
            if (rb instanceof Double && rc instanceof Double) {
              double quotient = ((Double)rb).doubleValue() /
                ((Double)rc).doubleValue();
              stack.setElementAt(valueOfNumber(quotient), base+a);
            } else if (toNumberPair(rb, rc, NUMOP)) {
              double quotient = NUMOP[0] / NUMOP[1];
              stack.setElementAt(valueOfNumber(quotient), base+a);
            } else {
              // :todo: use metamethod
              throw new IllegalArgumentException();
            }
            continue;
          case OP_MOD:
            rb = RK(k, ARGB(i));
            rc = RK(k, ARGC(i));
            if (rb instanceof Double && rc instanceof Double) {
              double db = ((Double)rb).doubleValue();
              double dc = ((Double)rc).doubleValue();
              double modulus = modulus(db, dc);
              stack.setElementAt(valueOfNumber(modulus), base+a);
            } else if (toNumberPair(rb, rc, NUMOP)) {
              double modulus = modulus(NUMOP[0], NUMOP[1]);
              stack.setElementAt(valueOfNumber(modulus), base+a);
            } else {
              // :todo: use metamethod
              throw new IllegalArgumentException();
            }
            continue;
          case OP_POW:
            // There's no Math.pow.  :todo: consider implementing.
            throw new IllegalArgumentException();
          case OP_UNM: {
            rb = stack.elementAt(base+ARGB(i));
            if (rb instanceof Double) {
              double db = ((Double)rb).doubleValue();
              stack.setElementAt(valueOfNumber(-db), base+a);
            } else if (tonumber(rb, NUMOP)) {
              stack.setElementAt(valueOfNumber(-NUMOP[0]), base+a);
            } else {
              // :todo: metamethod
              throw new IllegalArgumentException();
            }
            continue;
          }
          case OP_NOT: {
            Object ra = stack.elementAt(base+ARGB(i));
            stack.setElementAt(valueOfBoolean(isFalse(ra)), base+a);
            continue;
          }
          case OP_LEN:
            rb = stack.elementAt(base+ARGB(i));
            if (rb instanceof LuaTable) {
              LuaTable t = (LuaTable)rb;
              stack.setElementAt(new Double(t.getn()), base+a);
              continue;
            } else if (rb instanceof String) {
              String s = (String)rb;
              stack.setElementAt(new Double(s.length()), base+a);
              continue;
            }
            // :todo: metamethod
            throw new IllegalArgumentException();
          case OP_CONCAT: {
            int b = ARGB(i);
            int c = ARGC(i);
            savedpc = pc; // Protect
            // :todo: It's possible that the compiler assumes that all
            // stack locations _above_ b end up with junk in them.  In
            // which case we can improve the speed of vmConcat (by not
            // converting each stack slot, but simply using
            // StringBuffer.append on whatever is there).
            vmConcat(c - b + 1, c);
            stack.setElementAt(stack.elementAt(base+b), base+a);
            continue;
          }
          case OP_JMP:
            // dojump
            pc += ARGsBx(i);
            continue;
          case OP_EQ:
            rb = RK(k, ARGB(i));
            rc = RK(k, ARGC(i));
            if (vmEqual(rb, rc) == (a != 0)) {
              // dojump
              pc += ARGsBx(code[pc]);
            }
            ++pc;
            continue;
          case OP_LT:
            savedpc = pc; // Protect
            if (vmLessthan(RK(k, ARGB(i)), RK(k, ARGC(i))) == (a != 0)) {
              // dojump
              pc += ARGsBx(code[pc]);
            }
            ++pc;
            continue;
          case OP_LE:
            savedpc = pc; // Protect
            if (vmLessequal(RK(k, ARGB(i)), RK(k, ARGC(i))) == (a != 0)) {
              // dojump
              pc += ARGsBx(code[pc]);
            }
            ++pc;
            continue;
          case OP_TEST:
            if (isFalse(stack.elementAt(base+a)) != (ARGC(i) != 0)) {
              // dojump
              pc += ARGsBx(code[pc]);
            }
            ++pc;
            continue;
          case OP_TESTSET:
            rb = stack.elementAt(base+ARGB(i));
            if (isFalse(rb) != (ARGC(i) != 0)) {
              stack.setElementAt(rb, base+a);
              // dojump
              pc += ARGsBx(code[pc]);
            }
            ++pc;
            continue;
          case OP_CALL: {
            int b = ARGB(i);
            int nresults = ARGC(i) - 1;
            if (b != 0) {
              stack.setSize(base+a+b);
            }
            savedpc = pc;
            switch (vmPrecall(base+a, nresults)) {
              case PCRLUA:
                nexeccalls++;
                continue reentry;
              case PCRJ:
                // Was Java function called by precall, adjust result
                if (nresults >= 0) {
                  stack.setSize(ci.top());
                }
                continue;
              default:
                return; // yield
            }
          }
          case OP_TAILCALL: {
            int b = ARGB(i);
            if (b != 0) {
              stack.setSize(base+a+b);
            }
            savedpc = pc;
            // assert ARGC(i) - 1 == MULTRET
            switch (vmPrecall(base+a, MULTRET)) {
              case PCRLUA: {
                // tail call: put new frame in place of previous one.
                CallInfo ci = (CallInfo)civ.elementAt(civ.size()-2);
                int func = ci.function();
                int pfunc = this.ci.function();
                fClose(base);
                base = func + (this.ci.base() - pfunc);
                int aux;        // loop index is used after loop ends
                for (aux=0; pfunc+aux < stack.size(); ++aux) {
                  // move frame down
                  stack.setElementAt(stack.elementAt(pfunc+aux), func+aux);
                }
                stack.setSize(func+aux);        // correct top
                // assert stack.size() == base + ((LuaFunction)stack.elementAt(func)).proto().maxstacksize();
                ci.tailcall(base, stack.size());
                dec_ci();       // remove new frame.
                continue reentry;
              }
              case PCRJ: {      // It was a Java function
                continue;
              }
              default: {
                return; // yield
              }
            }
          }
          case OP_RETURN: {
            int b = ARGB(i);
            if (b != 0) {
              int top = a + b - 1;
              stack.setSize(base + top);
            }
            fClose(base);
            savedpc = pc;
            // 'adjust' replaces aliased 'b' in PUC-Rio code.
            boolean adjust = vmPoscall(base+a);
            if (--nexeccalls == 0) {
              return;
            }
            if (adjust) {
              stack.setSize(ci.top());
            }
            continue reentry;
          }
          case OP_FORLOOP: {
            double step =
                ((Double)stack.elementAt(base+a+2)).doubleValue();
            double idx =
                ((Double)stack.elementAt(base+a)).doubleValue() + step;
            double limit =
                ((Double)stack.elementAt(base+a+1)).doubleValue();
            if ((0 < step && idx <= limit) ||
                (step <= 0 && limit <= idx)) {
              // dojump
              pc += ARGsBx(i);
              Object d = valueOfNumber(idx);
              stack.setElementAt(d, base+a);    // internal index
              stack.setElementAt(d, base+a+3);  // external index
            }
            continue;
          }
          case OP_FORPREP: {
            int init = base+a;
            int plimit = base+a+1;
            int pstep = base+a+2;
            savedpc = pc;       // next steps may throw errors
            if (!tonumber(init)) {
              gRunerror("'for' initial value must be a number");
            } else if (!tonumber(plimit)) {
              gRunerror("'for' limit must be a number");
            } else if (!tonumber(pstep)) {
              gRunerror("'for' step must be a number");
            }
            double step =
                ((Double)stack.elementAt(pstep)).doubleValue();
            double idx =
                ((Double)stack.elementAt(init)).doubleValue() - step;
            stack.setElementAt(new Double(idx), init);
            // dojump
            pc += ARGsBx(i);
            continue;
          }
          case OP_TFORLOOP: {
            int cb = base+a+3;  // call base
            stack.setElementAt(stack.elementAt(base+a+2), cb+2);
            stack.setElementAt(stack.elementAt(base+a+1), cb+1);
            stack.setElementAt(stack.elementAt(base+a), cb);
            stack.setSize(cb+3);
            savedpc = pc; // Protect
            vmCall(cb, ARGC(i));
            stack.setSize(ci.top());
            if (NIL != stack.elementAt(cb)) {   // continue loop
              stack.setElementAt(stack.elementAt(cb), cb-1);
              // dojump
              pc += ARGsBx(code[pc]);
            }
            ++pc;
            continue;
          }
          case OP_SETLIST: {
            int n = ARGB(i);
            int c = ARGC(i);
            int last;
            LuaTable t;
            if (0 == n) {
              n = (stack.size() - a) - 1;
              // :todo: check this against PUC-Rio
              // stack.setSize ??
            }
            if (0 == c) {
              c = code[pc++];
            }
            t = (LuaTable)stack.elementAt(base+a);
            last = ((c-1)*LFIELDS_PER_FLUSH) + n;
            // :todo: consider expanding space in table
            for (; n > 0; n--) {
              Object val = stack.elementAt(base+a+n);
              t.putnum(last--, val);
            }
            continue;
          }
          case OP_CLOSE:
            fClose(base+a);
            continue;
          case OP_CLOSURE: {
            Proto p = function.proto().proto()[ARGBx(i)];
            int nup = p.nups();
            UpVal[] up = new UpVal[nup];
            for (int j=0; j<nup; j++, pc++) {
              int in = code[pc];
              if (OPCODE(in) == OP_GETUPVAL) {
                up[j] = function.upVal(ARGB(in));
              } else {
                // assert OPCODE(in) == OP_MOVE;
                up[j] = fFindupval(base + ARGB(in));
              }
            }
            LuaFunction nf = new LuaFunction(p, up, function.getEnv());
            stack.setElementAt(nf, base+a);
            continue;
          }
          case OP_VARARG: {
            int b = ARGB(i)-1;
            int n = (base - ci.function()) -
                function.proto().numparams() - 1;
            if (b == MULTRET) {
              // :todo: Protect
              // :todo: check stack
              b = n;
              stack.setSize(base+a+n);
            }
            for (int j=0; j<b; ++j) {
              if (j < n) {
                Object src = stack.elementAt(base - n + j);
                stack.setElementAt(src, base+a+j);
              } else {
                stack.setElementAt(NIL, base+a+j);
              }
            }
            continue;
          }
        } /* switch */
      } /* while */
    } /* reentry: while */
  }

  /** Equivalent of luaV_gettable. */
  private Object vmGettable(Object t, Object key) {
    // :todo: metamethods
    Object tm;
    for (int loop = 0; loop < MAXTAGLOOP; ++loop) {
      if (t instanceof LuaTable) {      // 't' is a table?
        LuaTable h = (LuaTable)t;
        Object res = h.get(key);
        if (!isNil(res) || ((tm = tagmethod(h, "__index")) == null)) {
          return res;
        } // else will try the tag method
      } else if ((tm = tagmethod(t, "__index")) == null) {
        gTypeerror(t, "index");
      }
      if (isFunction(tm)) {
        return callTMres(tm, t, key);
      }
      t = tm;     // else repeat with 'tm'
    }
    gRunerror("loop in gettable");
    // NOTREACHED
    return null;
  }

  /** Equivalent of luaV_lessthan. */
  private boolean vmLessthan(Object l, Object r) {
    // :todo: currently goes wrong when comparing nil.  Fix it.
    if (l.getClass() != r.getClass()) {
      // :todo: Make Lua error
      throw new IllegalArgumentException();
    } else if (l instanceof Double) {
      return ((Double)l).doubleValue() < ((Double)r).doubleValue();
    } else if (l instanceof String) {
      // :todo: PUC-Rio use strcoll, maybe we should use something
      // equivalent.
      return ((String)l).compareTo((String)r) < 0;
    }
    // :todo: metamethods
    throw new IllegalArgumentException();
  }
  /** Equivalent of luaV_lessequal. */
  private boolean vmLessequal(Object l, Object r) {
    // :todo: currently goes wrong when comparing nil.  Fix it.
    if (l.getClass() != r.getClass()) {
      // :todo: Make Lua error
      throw new IllegalArgumentException();
    } else if (l instanceof Double) {
      return ((Double)l).doubleValue() <= ((Double)r).doubleValue();
    } else if (l instanceof String) {
      return ((String)l).compareTo((String)r) <= 0;
    }
    // :todo: metamethods
    throw new IllegalArgumentException();
  }

  /**
   * Equivalent of luaD_poscall.
   * @param firstResult  stack index (absolute) of the first result
   */
  private boolean vmPoscall(int firstResult) {
    // :todo: call hook
    CallInfo lci; // local copy, for faster access
    lci = dec_ci();
    // Now (as a result of the dec_ci call), lci is the CallInfo record
    // for the current function (the function executing an OP_RETURN
    // instruction), and this.ci is the CallInfo record for the function
    // we are returning to.
    int res = lci.res();
    int wanted = lci.nresults();        // Caution: wanted could be == MULTRET
    base = this.ci.base();
    savedpc = this.ci.savedpc();
    // Move results (and pad with nils to required number if necessary)
    int i = wanted;
    int top = stack.size();
    // The movement is always downwards, so copying from the top-most
    // result first is always correct.
    while (i != 0 && firstResult < top) {
      stack.setElementAt(stack.elementAt(firstResult++), res++);
      i--;
    }
    if (i > 0) {
      stack.setSize(res+i);
    }
    // :todo: consider using two stack.setSize calls to nil out
    // remaining required results.
    // This trick only works if Lua.NIL == null, whereas the current
    // code works regardless of what Lua.NIL is.
    while (i-- > 0) {
      stack.setElementAt(NIL, res++);
    }
    stack.setSize(res);
    return wanted != MULTRET;
  }

  /**
   * Equivalent of LuaD_precall.  This method expects that the arguments
   * to the function are placed above the function on the stack.
   * @param func  absolute stack index of the function to call.
   * @param r     number of results expected.
   */
  private int vmPrecall(int func, int r) {
    // :todo: metamethod for non-function values.
    this.ci.setSavedpc(savedpc);
    Object faso;        // Function AS Object
    faso = stack.elementAt(func);
    if (faso instanceof LuaFunction) {
      LuaFunction f = (LuaFunction)faso;
      Proto p = f.proto();
      // :todo: ensure enough stack

      if (!p.vararg()) {
        base = func + 1;
        if (stack.size() > base + p.numparams()) {
          // trim stack to the argument list
          stack.setSize(base + p.numparams());
        }
      } else {
        int nargs = (stack.size() - func) - 1;
        base = adjust_varargs(p, nargs);
      }

      int top = base + p.maxstacksize();
      inc_ci(func, base, top, r);

      savedpc = 0;
      // expand stack to the function's max stack size.
      stack.setSize(top);
      // :todo: implement call hook.
      return PCRLUA;
    } else if (faso instanceof LuaJavaCallback) {
      LuaJavaCallback fj = (LuaJavaCallback)faso;
      // :todo: checkstack (not sure it's necessary)
      base = func + 1;
      inc_ci(func, base, stack.size()+MINSTACK, r);
      // :todo: call hook
      int n = fj.luaFunction(this);
      if (n < 0) {      // yielding?
        return PCRYIELD;
      } else {
        vmPoscall(stack.size() - n);
        return PCRJ;
      }
    }

    throw new IllegalArgumentException();
  }

  /** Equivalent of luaV_settable. */
  private void vmSettable(Object t, Object key, Object val) {
    // :todo: metamethods
    LuaTable h = (LuaTable)t;
    h.put(key, val);
  }

  private String vmTostring(Object o) {
    if (o instanceof String) {
      return (String)o;
    }
    if (!(o instanceof Double)) {
      return null;
    }
    // Convert number to string.  PUC-Rio abstracts this operation into
    // a macro, lua_number2str.  The macro is only invoked from their
    // equivalent of this code.
    Double d = (Double)o;
    String repr = d.toString();
    // Note: A naive conversion results in 3..4 == "3.04.0" which isn't
    // good.  We special case the integers.
    if (repr.endsWith(".0")) {
      repr = repr.substring(0, repr.length()-2);
    }
    return repr;
  }

  /** Equivalent of adjust_varargs in "ldo.c". */
  private int adjust_varargs(Proto p, int actual) {
    int nfixargs = p.numparams();
    for (; actual < nfixargs; ++actual) {
      stack.addElement(NIL);
    }
    // PUC-Rio's LUA_COMPAT_VARARG is not supported here.

    // Move fixed parameters to final position
    int fixed = stack.size() - actual;  // first fixed argument
    int base = stack.size();    // final position of first argument
    for (int i=0; i<nfixargs; ++i) {
      stack.addElement(stack.elementAt(fixed+i));
      stack.setElementAt(NIL, fixed+i);
    }
    return base;
  }

  private Object callTMres(Object f, Object p1, Object p2) {
    push(f);
    push(p1);
    push(p2);
    vmCall(stack.size()-3, 1);

    Object res = stack.lastElement();
    pop(1);
    return res;
  }

  /**
   * Gets tagmethod for object.
   */
  private Object tagmethod(Object o, String event) {
    LuaTable mt;

    mt = getMetatable(o);
    if (mt == null) {
      return null;
    }
    return mt.get(event);
  }

  /**
   * Computes the result of Lua's modules operator (%).  Note that this
   * modulus operator does not match Java's %.
   */
  private static double modulus(double x, double y) {
    return x - Math.floor(x/y)*y;
  }

  /**
   * Convert to number.  Returns true if the argument o was converted to
   * a number.  Converted number is placed in <var>out[0]</var>.  Returns
   * false if the argument <var>o</var> could not be converted to a number.
   */
  private static boolean tonumber(Object o, double[] out) {
    if (o instanceof Double) {
      out[0] = ((Double)o).doubleValue();
      return true;
    }
    if (!(o instanceof String)) {
      return false;
    }
    if (oStr2d((String)o, out)) {
      return true;
    }
    return false;
  }

  /**
   * Converts a stack slot to number.  Returns true if the element at
   * the specified stack slot was converted to a number.  False
   * otherwise.  Note that this actually modifies the element stored at
   * <var>idx</var> in the stack (in faithful emulation of the PUC-Rio
   * code).  Corrupts <code>NUMOP[0]</code>.
   * @param idx  absolute stack slot.
   */
  private boolean tonumber(int idx) {
    if (tonumber(stack.elementAt(idx), NUMOP)) {
      stack.setElementAt(new Double(NUMOP[0]), idx);
      return true;
    }
    return false;
  }

  /** Convert a pair of operands for an arithmetic opcode. */
  private static boolean toNumberPair(Object x, Object y, double[] out) {
    if (tonumber(y, out)) {
      out[1] = out[0];
      if (tonumber(x, out)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Convert to string.  Returns true if element was number or string
   * (the number will have been converted to a string), false otherwise.
   * Note this actually modifies the element stored at <var>idx</var> in
   * the stack (in faithful emulation of the PUC-Rio code).
   */
  private boolean tostring(int idx) {
    Object o = stack.elementAt(idx);
    String s = vmTostring(o);
    if (s == null) {
      return false;
    }
    stack.setElementAt(s, idx);
    return true;
  }

  /** Lua's is False predicate. */
  private boolean isFalse(Object o) {
    return o == NIL || Boolean.FALSE.equals(o);
  }

  /** Make new CallInfo record. */
  private CallInfo inc_ci(int func, int base, int top, int nresults) {
    ci = new CallInfo(func, base, top, nresults);
    civ.addElement(ci);
    return ci;
  }

  /** Pop topmost CallInfo record and return it. */
  private CallInfo dec_ci() {
    CallInfo ci = (CallInfo)civ.pop();
    this.ci = (CallInfo)civ.lastElement();
    return ci;
  }
}

