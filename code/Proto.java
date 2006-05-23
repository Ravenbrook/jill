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
  // Generally the fields are named following the PUC-Rio implementation
  // and so are unusually terse.
  /** Array of constants.  Do not modify the array. */
  private Object[] k;
  /** Array of VM instructions.  Do not modify the array. */
  private int[] code;
  /** Array of Proto objects.  Do not modify the array. */
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

  /**
   * Fresh Proto.  All the arrays that are passed to the constructor are
   * referenced by the instance.  Avoid unintentional sharing.  All
   * arrays must be non-null and all int parameters must not be
   * negative.
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

  /** Instruction block. */
  int[] code() {
    return code;
  }

  /** Array of inner protos. */
  Proto[] proto() {
    return p;
  }

  /** Constant array. */
  Object[] constant() {
    return k;
  }
}

