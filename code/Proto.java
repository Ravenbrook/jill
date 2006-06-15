// $Header$

/**
 * Models a function prototype.  This class is internal to Jili and
 * should not be used by clients.  This is the analogue of the PUC-Rio
 * type <code>Proto</code>, hence the name.
 * A function prototype represents the constant part of a function, that
 * is, a function without closures (upvalues) and without an
 * environment.  It's a handle for a block of VM instructions and
 * ancillary constants.
 *
 * For convenience some private arrays are exposed.  Modifying these
 * arrays is punishable by death. (Java has no convenient constant
 * array datatype)
 */
final class Proto {
  /** Interned 0-element array. */
  private static final int[] intArrayZero = new int[0];
  private static final LocVar[] locvarArrayZero = new LocVar[0];
  private static final Object[] objectArrayZero = new Object[0];
  private static final Proto[] protoArrayZero = new Proto[0];
  private static final String[] stringArrayZero = new String[0];

  // Generally the fields are named following the PUC-Rio implementation
  // and so are unusually terse.
  /** Array of constants. */
  private Object[] k;
  /** Array of VM instructions. */
  private int[] code;
  /** Array of Proto objects. */
  private Proto[] p;
  /**
   * Number of upvalues used by this prototype (and so by all the
   * functions created from this Proto).
   */
  private int nups;
  /**
   * Number of formal parameters used by this prototype, and so the
   * number of argument received by a function created from this Proto.
   * In a function defined to be variadic then this is the number of
   * fixed parameters, the number appearing before '...' in the parameter
   * list.
   */
  private int numparams;
  /**
   * <code>true</code> if and only if the function is variadic, that is,
   * defined with '...' in its parameter list.
   */
  private boolean vararg;
  private int maxstacksize;
  // Debug info
  /** Map from PC to line number. */
  private int[] lineinfo;
  private LocVar[] locvar;
  private String[] upvalue;
  private String source;

  /**
   * Proto synthesized by {@link Loader}.
   * All the arrays that are passed to the constructor are
   * referenced by the instance.  Avoid unintentional sharing.  All
   * arrays must be non-null and all int parameters must not be
   * negative.  Generally, this constructor is used by {@link Loader}
   * since that has all the relevant arrays already constructed (as
   * opposed to the compiler).
   * @param constant   array of constants.
   * @param code       array of VM instructions.
   * @param nups       number of upvalues (used by this function).
   * @param numparams  number of fixed formal parameters.
   * @param vararg     whether '...' is used.
   * @param maxstacksize  number of stack slots required when invoking.
   * @throws NullPointerException if any array arguments are null.
   * @throws IllegalArgumentException if nups or numparams is negative.
   */
  Proto(Object[] constant,
      int[] code,
      Proto[] proto,
      int nups,
      int numparams,
      boolean vararg,
      int maxstacksize) {
    if (null == constant || null == code || null == proto) {
      throw new NullPointerException();
    }
    if (nups < 0 || numparams < 0 || maxstacksize < 0) {
      throw new IllegalArgumentException();
    }
    this.k = constant;
    this.code = code;
    this.p = proto;
    this.nups = nups;
    this.numparams = numparams;
    this.vararg = vararg;
    this.maxstacksize = maxstacksize;
  }

  /**
   * Blank Proto in preparation for compilation.
   */
  Proto(String source) {
    maxstacksize = 2;   // register 0/1 are always valid.
    this.source = source;
    this.k = objectArrayZero;
    this.code = intArrayZero;
    this.p = protoArrayZero;
    this.lineinfo = intArrayZero;
    this.locvar = locvarArrayZero;
    this.upvalue = stringArrayZero;
  }

  /**
   * Augment with debug info.  All the arguments are referenced by the
   * instance after the method has returned, so try not to share them.
   */
  void debug(int[] lineinfo, LocVar[] locvar, String[] upvalue) {
    this.lineinfo = lineinfo;
    this.locvar = locvar;
    this.upvalue = upvalue;
  }

  /** Gets Number of Upvalues */
  int nups() {
    return nups;
  }

  /** Number of Parameters. */
  int numparams() {
    return numparams;
  }

  /** Maximum Stack Size. */
  int maxstacksize() {
    return maxstacksize;
  }

  /** Setter for maximum stack size. */
  void setMaxstacksize(int m) {
    maxstacksize = m;
  }

  /** Instruction block (do not modify). */
  int[] code() {
    return code;
  }

  /** Append instruction. */
  void codeAppend(int pc, int instruction, int line) {
    if (pc >= code.length) {
      int[] newCode = new int[code.length*2+1];
      System.arraycopy(code, 0, newCode, 0, code.length);
      code = newCode;
    }
    code[pc] = instruction;

    if (pc >= lineinfo.length) {
      int[] newLineinfo = new int[lineinfo.length*2+1];
      System.arraycopy(lineinfo, 0, newLineinfo, 0, lineinfo.length);
      lineinfo = newLineinfo;
    }
    lineinfo[pc] = line;
  }

  /** Set lineinfo record. */
  void setLineinfo(int pc, int line) {
    lineinfo[pc] = line;
  }

  /** Array of inner protos (do not modify). */
  Proto[] proto() {
    return p;
  }

  /** Constant array (do not modify). */
  Object[] constant() {
    return k;
  }

  /** Append constant. */
  void constantAppend(int idx, Object o) {
    if (idx >= k.length) {
      Object[] newK = new Object[k.length*2+1];
      System.arraycopy(k, 0, newK, 0, k.length);
      k = newK;
    }
    k[idx] = o;
  }

  /** Predicate for whether function uses ... in its parameter list. */
  boolean vararg() {
    return vararg;
  }

  /** "Setter" for vararg.  Sets it to true. */
  void setVararg() {
    vararg = true;
  }

  /** LocVar array (do not modify). */
  LocVar[] locvar() { 
    return locvar;
  }

  // All the trim functions, below, check for the redundant case of
  // trimming to the length that they already are.  Because they are
  // initially allocated as interned zero-length arrays this also means
  // that no unnecesary zero-length array objects are allocated.

  /**
   * Trim an int array to specified size.
   * @return the trimmed array.
   */
  private int[] trimInt(int[] old, int n) {
    if (n == old.length) {
      return old;
    }
    int[] newArray = new int[n];
    System.arraycopy(old, 0, newArray, 0, n);
    return newArray;
  }

  /** Trim code array to specified size. */
  void closeCode(int n) {
    code = trimInt(code, n);
  }

  /** Trim lineinfo array to specified size. */
  void closeLineinfo(int n) {
    lineinfo = trimInt(lineinfo, n);
  }

  /** Trim k (constant) array to specified size. */
  void closeK(int n) {
    if (n == k.length) {
      return;
    }
    Object[] newArray = new Object[n];
    System.arraycopy(k, 0, newArray, 0, n);
    k = newArray;
  }

  /** Trim p (proto) array to specified size. */
  void closeP(int n) {
    if (n == p.length) {
      return;
    }
    Proto[] newArray = new Proto[n];
    System.arraycopy(p, 0, newArray, 0, n);
    p = newArray;
  }

  /** Trim locvar array to specified size. */
  void closeLocvar(int n) {
    if (n == locvar.length) {
      return;
    }
    LocVar[] newArray = new LocVar[n];
    System.arraycopy(locvar, 0, newArray, 0, n);
    locvar = newArray;
  }

  /** Trim upvalue array to size <var>nups</var>. */
  void closeUpvalue() {
    if (nups == upvalue.length) {
      return;
    }
    String[] newArray = new String[nups];
    System.arraycopy(upvalue, 0, newArray, 0, nups);
    upvalue = newArray;
  }
}

