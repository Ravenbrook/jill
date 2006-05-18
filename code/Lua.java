// $Header$

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Stack;
import java.util.Vector;

/**
 * Encapsulates a Lua execution environment.  A lot of Jili's public API
 * manifests as public methods in this class.  A key part of the API is
 * the ability to call Lua functions from Java (ultimately, all Lua code
 * is executed in this manner).
 *
 * The Stack
 *
 * All arguments to Lua functions and all results returned by Lua
 * functions are placed onto a stack.  The stack can be indexed by an
 * integer in the same way as the PUC-Rio implementation.  A positive
 * index is an absolute index and ranges from 1 (the bottom-most
 * element) through to <var>n</var> (the top-most element),
 * where <var>n</var> is the number of elements on the stack.  Negative
 * indexes are relative indexes, -1 is the top-most element, -2 is the
 * element underneath that, and so on.  0 is not used.
 *
 * Note that in Jili the stack is used only for passing arguments and
 * returning results, unlike PUC-Rio.
 *
 * The protocol for calling a function is described in the {@link Lua#call}
 * method.  In brief: push the function onto the stack, then push the
 * arguments to the call.
 *
 * The methods {@link Lua#push}, {@link Lua#pop}, {@link Lua#value},
 * {@link Lua#gettop}, {@link Lua#settop} are used to manipulate the stack.
 */
public final class Lua {
  /** Table of globals (global variables).  Actually shared across all
   * coroutines. */
  private LuaTable global = new LuaTable();
  /** VM data stack. */
  private Vector stack = new Vector();
  private int base = 0;
  private int nCcalls = 0;
  /** Instruction to resume execution at.  Index into code array. */
  private int savedpc = 0;
  /**
   * Vector of CallInfo records.  Actually it's a Stack which is a
   * subclass of Vector, but it mostly the Vector methods that are used.
   */
  Stack civ = new Stack();
  /** CallInfo record for currently active function. */
  CallInfo ci = new CallInfo();
  {
    civ.addElement(ci);
  }

  /**
   * Equivalent of LUA_MULTRET.  Required, by vmPoscall, to be
   * negative.
   */
  public static final int MULTRET = -1;
  /**
   * Lua's nil value.
   */
  public static final Object NIL = null;

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
   * Gets the global environment.  The global environment, where global
   * variables live, is returned as a <code>LuaTable</code>.  Note that
   * modifying this table has exactly the same effect as creating or
   * changing global variables from within Lua.
   * @return  The global environment as a table.
   */
  public LuaTable getGlobals() {
    return global;
  }

  /**
   * Gets the number of elements in the stack.  If the stack is not
   * empty then this is the index of the top-most element.
   */
  public int gettop() {
    return stack.size();
  }

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
   * Removes the top-most <var>n</var> elements from the stack.
   */
  public void pop(int n) {
    if (n < 0) {
      throw new IllegalArgumentException();
    }
    stack.setSize(stack.size() - n);
  }

  /**
   * Pushes a value onto the stack in preparation for calling a
   * function.  See {@link Lua#call} for the protocol to be used for calling
   * functions.
   */
  public void push(Object o) {
    stack.addElement(o);
  }

  /**
   * Gets an element from a table, without using metamethods.
   * @param t  The table to access.
   * @param k  The index (key) into the table.
   * @return The value at the specified index.
   */
  public static Object rawget(Object t, Object k) {
    LuaTable table = (LuaTable)t;
    return table.get(k);
  }

  /**
   * Sets an element in a table, without using metamethods.
   * @param t  The table to modify.
   * @param k  The index into the table.
   * @param v  The new value to be stored at index <var>k</var>.
   */
  public static void rawset(Object t, Object k, Object v) {
    if (k == NIL) {
      throw new NullPointerException();
    }
    LuaTable table = (LuaTable)t;
    table.put(k, v);
  }

  /**
   * Gets a value from the stack.  The value at the specified stack
   * position is returned.
   */
  public Object value(int idx) {
    // :todo: Should we return null for indexes above the TOS?  Compare
    // with lua_pushvalue in PUC-Rio.
    if (idx > 0) {
      return stack.elementAt(base + idx - 1);
    }
    if (idx < 0) {
      return stack.elementAt(stack.size() + idx);
    }
    throw new IllegalArgumentException();
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

  // Instruction decomposition.

  // There follows a series of methods that extract the various fields
  // from a VM instruction.  See lopcodes.h from PUC-Rio.
  // :todo: Consider replacing with m4 macros (or similar).
  // A brief overview of the instruction format:
  // Logically an instruction has an opcode (6 bits), op, and up to three fields
  // using one of three formats:
  // A B C  (8 bits, 9 bits, 9 bits)
  // A Bx   (8 bits, 18 bits)
  // A sBx  (8 bits, 18 bits signed - excess K)
  // Some instructions do not use all the fields (EG OP_UNM only uses A
  // and B).
  // When packed into a word (an int in Jili) the following formats are
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

  // Hardwired values for speed.
  /** Equivalent of macro GET_OPCODE */
  private static int OPCODE(int instruction) {
    // POS_OP == 0 (shift amount)
    // SIZE_OP == 6 (opcode width)
    return instruction & 0x3f;
  }
  /** Equivalent of macro GETARG_A */
  private static int ARGA(int instruction) {
    // POS_A == POS_OP + SIZE_OP == 6 (shift amount)
    // SIZE_A == 8 (operand width)
    return (instruction >>> 6) & 0xff;
  }

  /** Equivalent of macro GETARG_B */
  private static int ARGB(int instruction) {
    // POS_B == POS_OP + SIZE_OP + SIZE_A + SIZE_C == 23 (shift amount)
    // SIZE_B == 9 (operand width)
    /* No mask required as field occupies the most significant bits of a
     * 32-bit int. */
    return (instruction >>> 23);
  }

  /** Equivalent of macro GETARG_C */
  private static int ARGC(int instruction) {
    // POS_C == POS_OP + SIZE_OP + SIZE_A == 14 (shift amount)
    // SIZE_C == 9 (operand width)
    return (instruction >>> 14) & 0x1ff;
  }

  /** Equivalent of macro GETARG_Bx */
  private static int ARGBx(int instruction) {
    // POS_Bx = POS_C == 14
    // SIZE_Bx == SIZE_C + SIZE_B == 18
    /* No mask required as field occupies the most significant bits of a
     * 32 bit int. */
    return (instruction >>> 14);
  }

  /** Equivalent of macro GETARG_sBx */
  private static int ARGsBx(int instruction) {
    // As ARGBx but with (2**17-1) subtracted.
    return (instruction >>> 14) - ((1<<17)-1);
  }

  /**
   * Near equivalent of macros RKB and RKC.  Note: non-static as it
   * requires stack and base instance members.  Stands for "Register or
   * Konstant" by the way, it gets value from either the register file
   * (stack) or the constant array (k).
   */
  private Object RK(Object[] k, int field) {
    // The "is constant" bit position depends on the size of the B and C
    // fields (required to be the same width).
    // SIZE_B == 9
    if (field >= 0x100) {
      return k[field & 0xff];
    }
    return stack.elementAt(base + field);
  }

  // opcode enumeration.
  // Generated by a script:
  // awk -f opcode.awk < lopcodes.h
  // and then pasted into here.

  private static final int OP_MOVE = 0;
  private static final int OP_LOADK = 1;
  private static final int OP_LOADBOOL = 2;
  private static final int OP_LOADNIL = 3;
  private static final int OP_GETUPVAL = 4;
  private static final int OP_GETGLOBAL = 5;
  private static final int OP_GETTABLE = 6;
  private static final int OP_SETGLOBAL = 7;
  private static final int OP_SETUPVAL = 8;
  private static final int OP_SETTABLE = 9;
  private static final int OP_NEWTABLE = 10;
  private static final int OP_SELF = 11;
  private static final int OP_ADD = 12;
  private static final int OP_SUB = 13;
  private static final int OP_MUL = 14;
  private static final int OP_DIV = 15;
  private static final int OP_MOD = 16;
  private static final int OP_POW = 17;
  private static final int OP_UNM = 18;
  private static final int OP_NOT = 19;
  private static final int OP_LEN = 20;
  private static final int OP_CONCAT = 21;
  private static final int OP_JMP = 22;
  private static final int OP_EQ = 23;
  private static final int OP_LT = 24;
  private static final int OP_LE = 25;
  private static final int OP_TEST = 26;
  private static final int OP_TESTSET = 27;
  private static final int OP_CALL = 28;
  private static final int OP_TAILCALL = 29;
  private static final int OP_RETURN = 30;
  private static final int OP_FORLOOP = 31;
  private static final int OP_FORPREP = 32;
  private static final int OP_TFORLOOP = 33;
  private static final int OP_SETLIST = 34;
  private static final int OP_CLOSE = 35;
  private static final int OP_CLOSURE = 36;
  private static final int OP_VARARG = 37;

  // end of instruction decomposition

  /** Equivalent of luaD_call. */
  private void vmCall(int func, int r) {
    ++nCcalls;
    if (vmPrecall(func, r) == PCRLUA) {
      vmExecute(1);
    }
    --nCcalls;
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

  private void vmExecute(int nexeccalls) {
    // This labelled while loop is used to simulate the effect of C's
    // goto.  The end of the while loop is never reached.  The beginning
    // of the while loop is branched to using a "continue reentry;"
    // statement (when a Lua functions is called or returns).
reentry:
    while (true) {
      // assert stack.elementAt[ci.function()] instanceof LuaFunction;
      LuaFunction function = (LuaFunction)stack.elementAt(ci.function());
      Proto proto = function.proto();
      int[] code = proto.code();
      Object[] k = proto.constant();
      int pc = savedpc;

      // :todo: remove code printing loop
      /*
      for (int i=0; i<code.length; ++i) {
        String s = "0000000" + Integer.toHexString(code[i]);
        s = s.substring(s.length()-8);
        System.out.println(s);
      }
      */

      while (true) {      // main loop of interpreter
        int i = code[pc++];       // VM instruction.
        // :todo: count and line hook
        int a = ARGA(i);          // its A field.
        Object rb, rc;

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

          case OP_GETGLOBAL:
            rb = k[ARGBx(i)];
            // :todo: metamethods
            stack.setElementAt(getGlobals().get(rb), base+a);
            continue;

          case OP_ADD:
            rb = RK(k, ARGB(i));
            rc = RK(k, ARGC(i));
            if (isNumber(rb) && isNumber(rc)) {
              double sum = ((Double)rb).doubleValue() +
                  ((Double)rc).doubleValue();
              stack.setElementAt(valueOfNumber(sum), base+a);
            } else {
              // :todo: convert or use metamethod
              throw new IllegalArgumentException();
            }
            continue;
          case OP_SUB:
            rb = RK(k, ARGB(i));
            rc = RK(k, ARGC(i));
            if (isNumber(rb) && isNumber(rc)) {
              double difference = ((Double)rb).doubleValue() +
                  ((Double)rc).doubleValue();
              stack.setElementAt(valueOfNumber(difference), base+a);
            } else {
              // :todo: convert or use metamethod
              throw new IllegalArgumentException();
            }
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

          case OP_RETURN: {
            int b = ARGB(i);
            if (b != 0) {
              int top = a + b - 1;
              stack.setSize(base + top);
            }
            // :todo: close UpVals
            savedpc = pc;
            // adjust replaces aliased 'b' in PUC-Rio code.
            boolean adjust = vmPoscall(a);
            if (--nexeccalls == 0) {
              return;
            }
            if (adjust) {
              stack.setSize(ci.top());
            }
            continue reentry;
          }
        } /* switch */
      } /* while */
    } /* reentry: while */
  }

  /**
   * Equivalent of luaD_poscall.
   * @param firstResult  stack index (relative to base) of the first result
   */
  boolean vmPoscall(int firstResult) {
    firstResult += base;        // Convert to absolute index
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
    stack.setSize(res + i);
    return wanted != MULTRET;
  }

  /**
   * Equivalent of LuaD_precall.  This method expects that the arguments
   * to the function are placed above the function on the stack.
   * @param func  stack index of the function to call.
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

      // :todo: implement vararg convention
      base = func + 1;
      if (stack.size() > base + p.numparams()) {
        // trim stack to the argument list
        stack.setSize(base + p.numparams());
      }
      int top = base + p.maxstacksize();

      CallInfo ci = inc_ci(func, base, top, r);

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

