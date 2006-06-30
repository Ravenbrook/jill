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
final class Proto
{
  /** Interned 0-element array. */
  private static final int[] ZERO_INT_ARRAY = new int[0];
  private static final LocVar[] ZERO_LOCVAR_ARRAY = new LocVar[0];
  private static final Object[] ZERO_OBJECT_ARRAY = new Object[0];
  private static final Proto[] ZERO_PROTO_ARRAY = new Proto[0];
  private static final String[] ZERO_STRING_ARRAY = new String[0];

  // Generally the fields are named following the PUC-Rio implementation
  // and so are unusually terse.
  /** Array of constants. */
  Object[] k;
  int sizek;
  /** Array of VM instructions. */
  int[] code;
  int sizecode;
  /** Array of Proto objects. */
  Proto[] p;
  int sizep;
  /**
   * Number of upvalues used by this prototype (and so by all the
   * functions created from this Proto).
   */
  int nups;
  /**
   * Number of formal parameters used by this prototype, and so the
   * number of argument received by a function created from this Proto.
   * In a function defined to be variadic then this is the number of
   * fixed parameters, the number appearing before '...' in the parameter
   * list.
   */
  int numparams;
  /**
   * <code>true</code> if and only if the function is variadic, that is,
   * defined with '...' in its parameter list.
   */
  boolean is_vararg;
  int maxstacksize;
  // Debug info
  /** Map from PC to line number. */
  int[] lineinfo;
  int sizelineinfo;
  LocVar[] locvars;
  int sizelocvars ;
  String[] upvalues;
  int sizeupvalues;
  String source;
  int linedefined;
  int lastlinedefined;

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
   * @param is_vararg     whether '...' is used.
   * @param maxstacksize  number of stack slots required when invoking.
   * @throws NullPointerException if any array arguments are null.
   * @throws IllegalArgumentException if nups or numparams is negative.
   */
  Proto(Object[] constant,
        int[] code,
        Proto[] proto,
        int nups,
        int numparams,
        boolean is_vararg,
        int maxstacksize)
  {
    if (null == constant || null == code || null == proto)
    {
      throw new NullPointerException();
    }
    if (nups < 0 || numparams < 0 || maxstacksize < 0)
    {
      throw new IllegalArgumentException();
    }
    this.k = constant; sizek = k.length ;
    this.code = code;  sizecode = code.length ;
    this.p = proto;    this.sizep = proto.length ;
    this.nups = nups;
    this.numparams = numparams;
    this.is_vararg = is_vararg;
    this.maxstacksize = maxstacksize;
  }

  /**
   * Blank Proto in preparation for compilation.
   */
  Proto(String source, int maxstacksize)
  {
      this.maxstacksize = maxstacksize;
      //    maxstacksize = 2;   // register 0/1 are always valid.
    this.source = source;
    this.k = ZERO_OBJECT_ARRAY;        this.sizek = 0 ;
    this.code = ZERO_INT_ARRAY;        this.sizecode = 0 ;
    this.p = ZERO_PROTO_ARRAY;         this.sizep = 0;
    this.lineinfo = ZERO_INT_ARRAY;    this.sizelineinfo = 0;
    this.locvars = ZERO_LOCVAR_ARRAY;  this.sizelocvars = 0 ;
    this.upvalues = ZERO_STRING_ARRAY; this.sizeupvalues = 0;
  }

  /**
   * Augment with debug info.  All the arguments are referenced by the
   * instance after the method has returned, so try not to share them.
   */
  void debug(int[] lineinfo, LocVar[] locvars, String[] upvalues)
  {
    this.lineinfo = lineinfo;  sizelineinfo = lineinfo.length;
    this.locvars = locvars;    sizelocvars = locvars.length;
    this.upvalues = upvalues;  sizeupvalues = upvalues.length;
  }

  /** Gets source. */
  String source()
  {
    return source;
  }

  /** Setter for source. */
  void setSource(String source)
  {
    this.source = source;
  }

  int linedefined()
  {
    return linedefined;
  }
  void setLinedefined(int linedefined)
  {
    this.linedefined = linedefined;
  }

  int lastlinedefined()
  {
    return lastlinedefined;
  }
  void setLastlinedefined(int lastlinedefined)
  {
    this.lastlinedefined = lastlinedefined;
  }

  /** Gets Number of Upvalues */
  int nups()
  {
    return nups;
  }

  /** Number of Parameters. */
  int numparams()
  {
    return numparams;
  }

  /** Maximum Stack Size. */
  int maxstacksize()
  {
    return maxstacksize;
  }

  /** Setter for maximum stack size. */
  void setMaxstacksize(int m)
  {
    maxstacksize = m;
  }

  /** Instruction block (do not modify). */
  int[] code()
  {
    return code;
  }

  /** Append instruction. */
  void codeAppend(Lua L, int pc, int instruction, int line)
  {
    ensureCode (L, pc);
    code[pc] = instruction;

    /** TODO: errorcase */
    if (pc >= lineinfo.length)
    {
      int[] newLineinfo = new int[lineinfo.length*2+1];
      System.arraycopy(lineinfo, 0, newLineinfo, 0, lineinfo.length);
      lineinfo = newLineinfo;
    }
    lineinfo[pc] = line;
  }

  void ensureLocvars (Lua L, int atleast, int limit)
  {
    if (atleast + 1 > sizelocvars)
    {
      int newsize = atleast*2+1 ;
      if (newsize > limit)
        newsize = limit ;
      if (atleast + 1 > newsize)
        L.gRunerror("too many local variables") ;
      LocVar [] newlocvars = new LocVar [newsize] ;
      System.arraycopy(locvars, 0, newlocvars, 0, sizelocvars) ;
      for (int i = sizelocvars ; i < newsize ; i++)
        newlocvars[i] = new LocVar () ;
      locvars = newlocvars ;
      sizelocvars = newsize ;
    }
  }

  void ensureProtos (Lua L, int atleast)
  {
    if (atleast + 1 > sizep)
    {
      int newsize = atleast*2+1 ;
      if (newsize > Lua.MAXARG_Bx)
        newsize = Lua.MAXARG_Bx ;
      if (atleast + 1 > newsize)
        L.gRunerror("constant table overflow") ;
      Proto [] newprotos = new Proto [newsize] ;
      System.arraycopy(p, 0, newprotos, 0, sizep) ;
      p = newprotos ;
      sizep = newsize ;
    }
  }

  void ensureUpvals (Lua L, int atleast)
  {
    if (atleast + 1 > sizeupvalues)
    {
      int newsize = atleast*2+1 ;
      if (atleast + 1 > newsize)
        L.gRunerror("upvalues overflow") ;
      String [] newupvalues = new String [newsize] ;
      System.arraycopy(upvalues, 0, newupvalues, 0, sizeupvalues) ;
      upvalues = newupvalues ;
      sizeupvalues = newsize ;
    }
  }

  void ensureCode (Lua L, int atleast)
  {
    if (atleast + 1 > sizecode)
    {
      int newsize = atleast*2+1 ;
      if (atleast + 1 > newsize)
        L.gRunerror("code overflow") ;
      int [] newcode = new int [newsize] ;
      System.arraycopy(code, 0, newcode, 0, sizecode) ;
      code = newcode ;
      sizecode = newsize ;
    }
  }

  /** Set lineinfo record. */
  void setLineinfo(int pc, int line)
  {
    lineinfo[pc] = line;
  }

  /** Get linenumber corresponding to pc, or 0 if no info. */
  int getline(int pc)
  {
    if (lineinfo.length == 0)
    {
      return 0;
    }
    return lineinfo[pc];
  }

  /** Array of inner protos (do not modify). */
  Proto[] proto()
  {
    return p;
  }

  /** Constant array (do not modify). */
  Object[] constant()
  {
    return k;
  }

  /** Append constant. */
  void constantAppend(int idx, Object o)
  {
    if (idx >= k.length)
    {
      Object[] newK = new Object[k.length*2+1];
      System.arraycopy(k, 0, newK, 0, k.length);
      k = newK;
    }
    k[idx] = o;
  }

  /** Predicate for whether function uses ... in its parameter list. */
  boolean is_vararg()
  {
    return is_vararg;
  }

  /** "Setter" for is_vararg.  Sets it to true. */
  void setIs_vararg()
  {
    is_vararg = true;
  }

  /** LocVar array (do not modify). */
  LocVar[] locvars()
  {
    return locvars;
  }

  // All the trim functions, below, check for the redundant case of
  // trimming to the length that they already are.  Because they are
  // initially allocated as interned zero-length arrays this also means
  // that no unnecesary zero-length array objects are allocated.

  /**
   * Trim an int array to specified size.
   * @return the trimmed array.
   */
  private int[] trimInt(int[] old, int n)
  {
    if (n == old.length)
    {
      return old;
    }
    int[] newArray = new int[n];
    System.arraycopy(old, 0, newArray, 0, n);
    return newArray;
  }

  /** Trim code array to specified size. */
  void closeCode(int n)
  {
    code = trimInt(code, n);
    sizecode = code.length ;
  }

  /** Trim lineinfo array to specified size. */
  void closeLineinfo(int n)
  {
    lineinfo = trimInt(lineinfo, n);
    sizelineinfo = n;
  }

  /** Trim k (constant) array to specified size. */
  void closeK(int n)
  {
    if (k.length > n)
    {
      Object [] newArray = new Object[n];
      System.arraycopy(k, 0, newArray, 0, n);
      k = newArray;
    }
    sizek = n ;
    return;
  }

  /** Trim p (proto) array to specified size. */
  void closeP(int n)
  {
    if (n == p.length)
    {
      return;
    }
    Proto[] newArray = new Proto[n];
    System.arraycopy(p, 0, newArray, 0, n);
    p = newArray;
    sizep = n ;
  }

  /** Trim locvar array to specified size. */
  void closeLocvars(int n)
  {
    if (n == locvars.length)
    {
      return;
    }
    LocVar[] newArray = new LocVar[n];
    System.arraycopy(locvars, 0, newArray, 0, n);
    locvars = newArray;
    sizelocvars = n;
  }

  /** Trim upvalues array to size <var>nups</var>. */
  void closeUpvalues()
  {
    if (nups == upvalues.length)
    {
      return;
    }
    String[] newArray = new String[nups];
    System.arraycopy(upvalues, 0, newArray, 0, nups);
    upvalues = newArray;
    sizeupvalues = nups;
  }

}
