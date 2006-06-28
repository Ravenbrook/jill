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


import java.util.Hashtable;

/**
 * Used to model a function during compilation.  Code generation uses
 * this structure extensively.  Most of the PUC-Rio functions from
 * lcode.c have moved into this class, alongwith a few functions from
 * lparser.c
 */
final class FuncState {
  /** See NO_JUMP in lcode.h. */
  static final int NO_JUMP = -1;

  /** Proto object for this function. */
  Proto f;
  /**
   * Table to find (and reuse) elements in <var>f.k</var>.  Maps from
   * Object (a constant Lua value) to an index into <var>f.k</var>.
   */
  Hashtable h;
  /** Enclosing function. */
  FuncState prev;
  /** Lexical state. */
  Syntax ls;
  /** Lua state. */
  Lua L;
  /** chain of current blocks */
  BlockCnt bl;
  /** next position to code. */
  int pc;
  /** pc of last jump target. */
  int lasttarget;
  /** List of pending jumps to <var>pc</var>. */
  int jpc;
  /** First free register. */
  int freereg;
  /** number of elements in <var>k</var>. */
  int nk;
  /** number of elements in <var>p</var>. */
  int np;
  /** number of elements in <var>locvars</var>. */
  short nlocvars;
  /** number of active local variables. */
  short nactvar;
  /** upvalues as 8-bit k and 8-bit info */
  int [] upvalues = new int [Lua.MAXUPVALUES] ;
  /** declared-variable stack. */
  short[] actvar = new short[Lua.MAXVARS];

  /**
   * Constructor.  Much of this is taken from <code>open_func</code> in
   * <code>lparser.c</code>.
   */
  FuncState(Syntax ls) {
    f = new Proto(ls.source());
    prev = ls.linkfs(this);
    this.ls = ls;
    // pc = 0;
    lasttarget = -1;
    jpc = NO_JUMP;
    // freereg = 0;
    // nk = 0;
    // np = 0;
    // nlocvars = 0;
    // nactvar = 0;
    // bl = null;
    h = new Hashtable();
  }

  /** Equivalent to <code>close_func</code> from <code>lparser.c</code>. */
  void close() {
    f.closeCode(pc);
    f.closeLineinfo(pc);
    f.closeK(nk);
    f.closeP(np);
    f.closeLocvars(nlocvars);
    f.closeUpvalues();
    Lua.gCheckcode(f);
    // assert bl == null;
  }

  /** Equivalent to getlocvar from lparser.c.
   * Accesses <code>LocVar</code>s of the {@link Proto}.
   */
  LocVar getlocvar(int idx) {
    return f.locvars[actvar[idx]];
  }


  // Functions from lcode.c

  /** Equivalent to luaK_checkstack. */
  void kCheckstack(int n) {
    int newstack = freereg + n;
    if (newstack > f.maxstacksize()) {
      if (newstack >= Lua.MAXSTACK) {
        ls.xSyntaxerror("function or expression too complex");
      }
      f.setMaxstacksize(newstack);
    }
  }

  /** Equivalent to luaK_code. */
  int kCode(int i, int line) {
    dischargejpc();
    // Put new instruction in code array.
    f.codeAppend(pc, i, line);
    return pc++;
  }

  /** Equivalent to luaK_codeABC. */
  int kCodeABC(int o, int a, int b, int c) {
    // assert getOpMode(o) == iABC;
    // assert getBMode(o) != OpArgN || b == 0;
    // assert getCMode(o) != OpArgN || c == 0;
    return kCode(Lua.CREATE_ABC(o, a, b, c), ls.lastline());
  }

  /** Equivalent to luaK_codeABx. */
  int kCodeABx(int o, int a, int bc) {
    // assert getOpMode(o) == iABx || getOpMode(o) == iAsBx);
    // assert getCMode(o) == OpArgN);
    return kCode(Lua.CREATE_ABx(o, a, bc), ls.lastline());
  }

  /** Equivalent to luaK_codeAsBx. */
  int kCodeAsBx(int o, int a, int bc) {
    return kCodeABx(o, a, bc+Lua.MAXARG_sBx);
  }

  /** Equivalent to luaK_dischargevars. */
  void kDischargevars(Expdesc e) {
    switch (e.kind()) {
      case Expdesc.VLOCAL:
        e.setKind(Expdesc.VNONRELOC);
        break;
      case Expdesc.VUPVAL:
        e.reloc(kCodeABC(Lua.OP_GETUPVAL, 0, e.info(), 0));
        break;
      case Expdesc.VGLOBAL:
        e.reloc(kCodeABx(Lua.OP_GETGLOBAL, 0, e.info()));
        break;
      case Expdesc.VINDEXED:
        freereg(e.aux());
        freereg(e.info());
        e.reloc(kCodeABC(Lua.OP_GETTABLE, 0, e.info(), e.aux()));
        break;
      case Expdesc.VVARARG:
      case Expdesc.VCALL:
        kSetoneret(e);
        break;
      default:
        break;  // there is one value available (somewhere)
    }
  }

  /** Equivalent to luaK_exp2anyreg. */
  int kExp2anyreg(Expdesc e) {
    kDischargevars(e);
    if (e.kind() == Expdesc.VNONRELOC) {
      if (!e.hasjumps()) {
        return e.info();
      }
      if (e.info() >= nactvar) {        // reg is not a local?
        exp2reg(e, e.info());   // put value on it
        return e.info();
      }
    }
    kExp2nextreg(e);    // default
    return e.info();
  }

  /** Equivalent to luaK_exp2nextreg. */
  void kExp2nextreg(Expdesc e) {
    kDischargevars(e);
    freeexp(e);
    kReserveregs(1);
    exp2reg(e, freereg - 1);
  }

  /** Equivalent to luaK_fixline. */
  void kFixline(int line) {
    f.setLineinfo(pc-1, line);
  }

  /** Equivalent to luaK_infix. */
  void kInfix(int op, Expdesc v) {
  switch (op) {
    case Syntax.OPR_AND:
      kGoiftrue(v);
      break;
    case Syntax.OPR_OR:
      kGoiffalse(v);
      break;
    case Syntax.OPR_CONCAT:
      kExp2nextreg(v);  /* operand must be on the `stack' */
      break;
    default:
      if (!isnumeral(v)) 
        kExp2RK(v);
      break;
    }
  }


  private boolean isnumeral(Expdesc e) {
    return e.k == Expdesc.VKNUM &&
        e.t == NO_JUMP &&
        e.f == NO_JUMP ;
  }

  /** Equivalent to luaK_nil. */
  void kNil(int from, int n) {
    // :todo: optimisation case
    kCodeABC(Lua.OP_LOADNIL, from, from+n-1, 0);
  }

  /** Equivalent to luaK_numberK. */
  int kNumberK(double r) {
    return addk(L.valueOfNumber(r));
  }

  /** Equivalent to luaK_posfix. */
  void kPosfix(int op, Expdesc e1, Expdesc e2) {
    switch (op) {
      case Syntax.OPR_AND:
        // lua_assert(e1.t == NO_JUMP);  /* list must be closed */
        kDischargevars(e2);
        kConcat(e1.f, e2.f);
        e1.k = e2.k;
        e1.info = e2.info;
        e1.aux = e2.aux;
        e1.t = e2.t;
        break;

      case Syntax.OPR_OR:
        //lua_assert(e1.f == NO_JUMP);  /* list must be closed */
        kDischargevars(e2);
        kConcat(e1.t, e2.t);
        e1.k = e2.k;
        e1.info = e2.info;
        e1.aux = e2.aux;
        e1.f = e2.f;
        break;

      case Syntax.OPR_CONCAT:
        kExp2val(e2);
        if (e2.k == Expdesc.VRELOCABLE && Lua.OPCODE(getcode(e2)) == Lua.OP_CONCAT) {
          //lua_assert(e1.info == Lua.ARGB(getcode(e2))-1);
          freeexp(e1);
          setcode(e2, Lua.SETARG_B(getcode(e2), e1.info));
          e1.k = e2.k; 
          e1.info = e2.info;
        }
        else {
          kExp2nextreg(e2);  /* operand must be on the 'stack' */
          codearith(Lua.OP_CONCAT, e1, e2);
        }
        break;

      case Syntax.OPR_ADD: codearith(Lua.OP_ADD, e1, e2); break;
      case Syntax.OPR_SUB: codearith(Lua.OP_SUB, e1, e2); break;
      case Syntax.OPR_MUL: codearith(Lua.OP_MUL, e1, e2); break;
      case Syntax.OPR_DIV: codearith(Lua.OP_DIV, e1, e2); break;
      case Syntax.OPR_MOD: codearith(Lua.OP_MOD, e1, e2); break;
      case Syntax.OPR_POW: codearith(Lua.OP_POW, e1, e2); break;
      case Syntax.OPR_EQ: codecomp(Lua.OP_EQ, true,  e1, e2); break;
      case Syntax.OPR_NE: codecomp(Lua.OP_EQ, false, e1, e2); break;
      case Syntax.OPR_LT: codecomp(Lua.OP_LT, true,  e1, e2); break;
      case Syntax.OPR_LE: codecomp(Lua.OP_LE, true,  e1, e2); break;
      case Syntax.OPR_GT: codecomp(Lua.OP_LT, false, e1, e2); break;
      case Syntax.OPR_GE: codecomp(Lua.OP_LE, false, e1, e2); break;
      default: 
          //lua_assert(false);
    }
  }

  /** Equivalent to luaK_prefix. */
  void kPrefix(int op, Expdesc e) {
    Expdesc e2 = new Expdesc(Expdesc.VKNUM, 0);
    switch (op) {
      case Syntax.OPR_MINUS:
        if (e.kind() == Expdesc.VK) {
          kExp2anyreg(e);
        }
        codearith(Lua.OP_UNM, e, e2);
        break;
      case Syntax.OPR_NOT:
        codenot(e);
        break;
      case Syntax.OPR_LEN:
        kExp2anyreg(e);
        codearith(Lua.OP_LEN, e, e2);
        break;
      default:
        throw new IllegalArgumentException();
    }
  }

  /** Equivalent to luaK_reserveregs. */
  void kReserveregs(int n) {
    kCheckstack(n);
    freereg += n;
  }

  /** Equivalent to luaK_ret. */
  void kRet(int first, int nret) {
    kCodeABC(Lua.OP_RETURN, first, nret+1, 0);
  }

  /** Equivalent to luaK_setmultret (in lcode.h). */
  void kSetmultret(Expdesc e) {
    kSetreturns(e, Lua.MULTRET);
  }

  /** Equivalent to luaK_setoneret. */
  void kSetoneret(Expdesc e) {
    if (e.kind() == Expdesc.VCALL) {    // expression is an open function call?
      e.nonreloc(Lua.ARGA(getcode(e)));
    } else if (e.kind() == Expdesc.VVARARG) {
      setargb(e, 2);
      e.setKind(Expdesc.VRELOCABLE);
    }
  }

  /** Equivalent to luaK_setreturns. */
  void kSetreturns(Expdesc e, int nresults) {
    if (e.kind() == Expdesc.VCALL) {    // expression is an open function call?
      setargc(e, nresults+1);
    } else if (e.kind() == Expdesc.VVARARG) {
      setargb(e, nresults+1);
      setarga(e, freereg);
      kReserveregs(1);
    }
  }

  /** Equivalent to luaK_stringK. */
  int kStringK(String s) {
    return addk(s);
  }

  private int addk(Object o) {
    Object v;
    v = h.get(o);
    if (v != null) {
      // :todo: assert
      return ((Integer)v).intValue();
    }
    // constant not found; create a new entry
    f.constantAppend(nk, o);
    h.put(o, new Integer(nk));
    return nk++;
  }

  private void codearith(int op, Expdesc e1, Expdesc e2) {
    if (constfolding(op, e1, e2))
      return;
    else {
      int o1 = kExp2RK(e1);
      int o2 = (op != Lua.OP_UNM && op != Lua.OP_LEN) ? kExp2RK(e2) : 0;
      freeexp(e2);
      freeexp(e1);
      e1.info = kCodeABC(op, 0, o1, o2);
      e1.k = Expdesc.VRELOCABLE;
    }
  }

  private boolean constfolding (int op, Expdesc e1, Expdesc e2) {
    double v1, v2, r;
    if (!isnumeral(e1) || !isnumeral(e2))
      return false;
    v1 = e1.nval;
    v2 = e2.nval;
    switch (op) {
      case Lua.OP_ADD: r = v1 + v2; break;
      case Lua.OP_SUB: r = v1 - v2; break;
      case Lua.OP_MUL: r = v1 * v2; break;
      case Lua.OP_DIV:
          if (v2 == 0.0) 
            return false;  /* do not attempt to divide by 0 */
          r = v1 / v2;
          break;
      case Lua.OP_MOD:
          if (v2 == 0.0)
            return false;  /* do not attempt to divide by 0 */
          r = v1 % v2;
          break;
      case Lua.OP_POW: r = Math.pow(v1, v2); break;
      case Lua.OP_UNM: r = -v1; break;
      case Lua.OP_LEN: return false;  /* no constant folding for 'len' */
      default:
          // lua_assert(false);
          r = 0.0; break;
    }
    if (!(r == r)) // NaN test
      return false;  /* do not attempt to produce NaN */
    e1.nval = r;
    return true;
  }

  private void codenot(Expdesc e) {
    kDischargevars(e);
    switch (e.k) {
      case Expdesc.VNIL:
      case Expdesc.VFALSE:
        e.k = Expdesc.VTRUE;
        break;

      case Expdesc.VK:
      case Expdesc.VKNUM:
      case Expdesc.VTRUE:
        e.k = Expdesc.VFALSE;
        break;

      case Expdesc.VJMP:
        invertjump(e);
        break;

      case Expdesc.VRELOCABLE:
      case Expdesc.VNONRELOC:
        discharge2anyreg(e);
        freeexp(e);
        e.info = kCodeABC(Lua.OP_NOT, 0, e.info, 0);
        e.k = Expdesc.VRELOCABLE;
        break;

      default:
        //lua_assert(false);  /* cannot happen */
        break;
    }
    /* interchange true and false lists */
    { int temp = e.f; e.f = e.t; e.t = temp; }
    removevalues(e.f);
    removevalues(e.t);
  }

  private void removevalues (int list) {
    for (; list != NO_JUMP; list = getjump(list))
      patchtestreg(list, Lua.NO_REG);
  }


  private void dischargejpc() {
    // :todo: implement me
    jpc = NO_JUMP;
  }

  private void discharge2reg(Expdesc e, int reg) {
    kDischargevars(e);
    switch (e.kind()) {
      case Expdesc.VNIL:
        kNil(reg, 1);
        break;
      case Expdesc.VFALSE: case Expdesc.VTRUE:
        kCodeABC(Lua.OP_LOADBOOL, reg,
            e.kind() == Expdesc.VTRUE ? 1 : 0, 0);
        break;
      case Expdesc.VK:
        kCodeABx(Lua.OP_LOADK, reg, e.info());
        break;
      case Expdesc.VKNUM:
        kCodeABx(Lua.OP_LOADK, reg, kNumberK(e.nval()));
        break;
      case Expdesc.VRELOCABLE:
        setarga(e, reg);
        break;
      case Expdesc.VNONRELOC:
        if (reg != e.info()) {
          kCodeABC(Lua.OP_MOVE, reg, e.info(), 0);
        }
        break;
      default:
        throw new IllegalArgumentException();
    }
    e.nonreloc(reg);
  }

  private void exp2reg(Expdesc e, int reg) {
    discharge2reg(e, reg);
    if (e.kind() == Expdesc.VJMP) {
      // :todo: put this jump in 't' list
    }
    if (e.hasjumps()) {
      // :todo: implement me
    }
    e.init(Expdesc.VNONRELOC, reg);
  }

  private void freeexp(Expdesc e) {
    if (e.kind() == Expdesc.VNONRELOC) {
      freereg(e.info());
    }
  }

  private void freereg(int reg) {
    if (!Lua.ISK(reg) && reg >= nactvar) {
      --freereg;
      // assert reg == freereg;
    }
  }

  int getcode(Expdesc e) {
    return f.code[e.info];
  }

  void setcode (Expdesc e, int code) {
    f.code[e.info] = code ;
  }

  /** Equivalent to indexupvalue from lparser.c */
  int indexupval(String name, Expdesc v) {
    // :todo: implement me
    return 99;
  }

  /** Equivalent to markupval from lparser.c */
  void markupval(int level) {
    // :todo: implement me
  }

  /** Equivalent to searchvar from lparser.c */
  int searchvar(String n) {
    // caution: descending loop (in emulation of PUC-Rio).
    for (int i=nactvar-1; i >= 0; i--) {
      if (n == getlocvar(i).varname) {
        return i;
      }
    }
    return -1;  // not found
  }

  void setarga(Expdesc e, int a) {
   int pc = e.info();
   int[] code = f.code();
   code[pc] = Lua.SETARG_A(code[pc], a);
  }

  void setargb(Expdesc e, int b) {
    int pc = e.info();
    int[] code = f.code();
    code[pc] = Lua.SETARG_B(code[pc], b);
  }

  void setargc(Expdesc e, int c) {
    int pc = e.info();
    int[] code = f.code();
    code[pc] = Lua.SETARG_C(code[pc], c);
  }

  /** Equivalent to <code>luaK_getlabel</code>. */
  int kGetlabel () {
    lasttarget = pc ;
    return pc;
  }

  /** Equivalent to <code>luaK_concat</code>. */
  /** l1 was an int*, now passing back as result */
  int kConcat (int l1, int l2) {
    if (l2 == NO_JUMP)
      return l1;
    else if (l1 == NO_JUMP)
      return l2;
    else {
      int list = l1;
      int next;
      while ((next = getjump(list)) != NO_JUMP)  /* find last element */
        list = next;
      fixjump(list, l2);
      return l1;
    }
  }

  /** Equivalent to <code>luaK_patchlist</code>. */
  void kPatchlist (int list, int target) {
    if (target == pc)
      kPatchtohere(list);
    else {
      //lua_assert(target < fs->pc);
      patchlistaux(list, target, Lua.NO_REG, target);
    }
  }

  private void patchlistaux (int list, int vtarget, int reg,
                             int dtarget) {
    while (list != NO_JUMP) {
      int next = getjump(list);
      if (patchtestreg(list, reg))
        fixjump(list, vtarget);
      else
        fixjump(list, dtarget);  /* jump to default target */
      list = next;
    }
  }

  private boolean patchtestreg (int node, int reg) {
    int i = getjumpcontrol(node);
    int [] code = f.code ;
    int instr = code[i] ;
    if (Lua.OPCODE(instr) != Lua.OP_TESTSET)
      return false;  /* cannot patch other instructions */
    if (reg != Lua.NO_REG && reg != Lua.ARGB(instr))
      code[i] = Lua.SETARG_A(instr, reg);
    else  /* no register to put value or register already has the value */
      code[i] = Lua.CREATE_ABC(Lua.OP_TEST, Lua.ARGB(instr), 0, Lua.ARGC(instr));

    return true;
  }

  private int getjumpcontrol (int pc) {
    int [] code = f.code ;
    if (pc >= 1 && testTMode(Lua.OPCODE(code[pc-1])))
      return pc-1;
    else
      return pc;
  }

  /*
  ** masks for instruction properties. The format is:
  ** bits 0-1: op mode
  ** bits 2-3: C arg mode
  ** bits 4-5: B arg mode
  ** bit 6: instruction set register A
  ** bit 7: operator is a test
  */

  /** arg modes */
  static final int OpArgN = 0 ;
  static final int OpArgU = 1 ;
  static final int OpArgR = 2 ;
  static final int OpArgK = 3 ;

  /** op modes */
  static final int iABC = 0 ;
  static final int iABx = 1 ;
  static final int iAsBx = 2 ;

  static byte opmode(int t, int a, int b, int c, int m) {
    return (byte) ((t<<7) | (a<<6) | (b<<4) | (c<<2) | m) ;
  }

    static final byte [] pOpmodes = new byte [] {
/*       T  A    B       C     mode                opcode       */
  opmode(0, 1, OpArgR, OpArgN, iABC)            /* OP_MOVE */
 ,opmode(0, 1, OpArgK, OpArgN, iABx)            /* OP_LOADK */
 ,opmode(0, 1, OpArgU, OpArgU, iABC)            /* OP_LOADBOOL */
 ,opmode(0, 1, OpArgR, OpArgN, iABC)            /* OP_LOADNIL */
 ,opmode(0, 1, OpArgU, OpArgN, iABC)            /* OP_GETUPVAL */
 ,opmode(0, 1, OpArgK, OpArgN, iABx)            /* OP_GETGLOBAL */
 ,opmode(0, 1, OpArgR, OpArgK, iABC)            /* OP_GETTABLE */
 ,opmode(0, 0, OpArgK, OpArgN, iABx)            /* OP_SETGLOBAL */
 ,opmode(0, 0, OpArgU, OpArgN, iABC)            /* OP_SETUPVAL */
 ,opmode(0, 0, OpArgK, OpArgK, iABC)            /* OP_SETTABLE */
 ,opmode(0, 1, OpArgU, OpArgU, iABC)            /* OP_NEWTABLE */
 ,opmode(0, 1, OpArgR, OpArgK, iABC)            /* OP_SELF */
 ,opmode(0, 1, OpArgK, OpArgK, iABC)            /* OP_ADD */
 ,opmode(0, 1, OpArgK, OpArgK, iABC)            /* OP_SUB */
 ,opmode(0, 1, OpArgK, OpArgK, iABC)            /* OP_MUL */
 ,opmode(0, 1, OpArgK, OpArgK, iABC)            /* OP_DIV */
 ,opmode(0, 1, OpArgK, OpArgK, iABC)            /* OP_MOD */
 ,opmode(0, 1, OpArgK, OpArgK, iABC)            /* OP_POW */
 ,opmode(0, 1, OpArgR, OpArgN, iABC)            /* OP_UNM */
 ,opmode(0, 1, OpArgR, OpArgN, iABC)            /* OP_NOT */
 ,opmode(0, 1, OpArgR, OpArgN, iABC)            /* OP_LEN */
 ,opmode(0, 1, OpArgR, OpArgR, iABC)            /* OP_CONCAT */
 ,opmode(0, 0, OpArgR, OpArgN, iAsBx)           /* OP_JMP */
 ,opmode(1, 0, OpArgK, OpArgK, iABC)            /* OP_EQ */
 ,opmode(1, 0, OpArgK, OpArgK, iABC)            /* OP_LT */
 ,opmode(1, 0, OpArgK, OpArgK, iABC)            /* OP_LE */
 ,opmode(1, 1, OpArgR, OpArgU, iABC)            /* OP_TEST */
 ,opmode(1, 1, OpArgR, OpArgU, iABC)            /* OP_TESTSET */
 ,opmode(0, 1, OpArgU, OpArgU, iABC)            /* OP_CALL */
 ,opmode(0, 1, OpArgU, OpArgU, iABC)            /* OP_TAILCALL */
 ,opmode(0, 0, OpArgU, OpArgN, iABC)            /* OP_RETURN */
 ,opmode(0, 1, OpArgR, OpArgN, iAsBx)           /* OP_FORLOOP */
 ,opmode(0, 1, OpArgR, OpArgN, iAsBx)           /* OP_FORPREP */
 ,opmode(1, 0, OpArgN, OpArgU, iABC)            /* OP_TFORLOOP */
 ,opmode(0, 0, OpArgU, OpArgU, iABC)            /* OP_SETLIST */
 ,opmode(0, 0, OpArgN, OpArgN, iABC)            /* OP_CLOSE */
 ,opmode(0, 1, OpArgU, OpArgN, iABx)            /* OP_CLOSURE */
 ,opmode(0, 1, OpArgU, OpArgN, iABC)            /* OP_VARARG */
      };

  private int getOpMode(int m) { return pOpmodes[m] & 3 ; }
  private int getBMode (int m) { return (pOpmodes[m] >> 4) & 3 ; }
  private int getCMode (int m) { return (pOpmodes[m] >> 2) & 3 ; }
  private boolean testAMode (int m) { return (pOpmodes[m] & (1<<6)) != 0 ; }
  private boolean testTMode (int m) { return (pOpmodes[m] & (1<<7)) != 0 ; }

  /** Equivalent to <code>luaK_patchtohere</code>. */
  void kPatchtohere (int list) {
    kGetlabel();
    jpc = kConcat(jpc, list);
  }

  private void fixjump (int pc, int dest) {
    int jmp = f.code[pc];
    int offset = dest-(pc+1);
    // lua_assert(dest != NO_JUMP);
    if (Math.abs(offset) > Lua.MAXARG_sBx)
      ls.xSyntaxerror("control structure too long");
    Lua.SETARG_sBx(jmp, offset);
  }

  private int getjump (int pc) {
    int offset = Lua.ARGsBx(f.code[pc]);
    if (offset == NO_JUMP)  /* point to itself represents end of list */
     return NO_JUMP;  /* end of list */
    else
      return (pc+1)+offset;  /* turn offset into absolute position */
  }

  /** Equivalent to <code>luaK_jump</code>. */
  int kJump () {
    int old_jpc = jpc;  /* save list of jumps to here */
    jpc = NO_JUMP;
    int j = kCodeAsBx(Lua.OP_JMP, 0, NO_JUMP);
    j = kConcat(j, old_jpc);  /* keep them on hold */
    return j;
  }

  /** Equivalent to <code>luaK_storevar</code>. */
  void kStorevar (Expdesc var, Expdesc ex) {
    switch (var.k) {
      case Expdesc.VLOCAL: {
        freeexp(ex);
        exp2reg(ex, var.info);
        return;
      }
      case Expdesc.VUPVAL: {
        int e = kExp2anyreg(ex);
        kCodeABC(Lua.OP_SETUPVAL, e, var.info, 0);
        break;
      }
      case Expdesc.VGLOBAL: {
        int e = kExp2anyreg(ex);
        kCodeABx(Lua.OP_SETGLOBAL, e, var.info);
        break;
      }
      case Expdesc.VINDEXED: {
        int e = kExp2RK(ex);
        kCodeABC(Lua.OP_SETTABLE, var.info, var.aux, e);
        break;
      }
      default: {
        //lua_assert(0);  /* invalid var kind to store */
        break;
      }
    }
    freeexp(ex);
  }

  /** Equivalent to <code>luaK_indexed</code>. */
  void kIndexed (Expdesc t, Expdesc k) {
    t.aux = kExp2RK(k);
    t.k = Expdesc.VINDEXED;
  }

  /** Equivalent to <code>luaK_exp2RK</code>. */
  int kExp2RK (Expdesc e) {
    kExp2val(e);
    switch (e.k) {
      case Expdesc.VKNUM:
      case Expdesc.VTRUE:
      case Expdesc.VFALSE:
      case Expdesc.VNIL:
        if (nk <= Lua.MAXINDEXRK) {  /* constant fit in RK operand? */
          e.info = (e.k == Expdesc.VNIL)  ? nilK() :
              (e.k == Expdesc.VKNUM) ? kNumberK(e.nval) :
              boolK(e.k == Expdesc.VTRUE);
          e.k = Expdesc.VK;
          return e.info | Lua.BITRK;
        }
        else break;

      case Expdesc.VK:
        if (e.info <= Lua.MAXINDEXRK)  /* constant fit in argC? */
          return e.info | Lua.BITRK;
        else break;

      default: break;
    }
    /* not a constant in the right range: put it in a register */
    return kExp2anyreg(e);
  }

  /** Equivalent to <code>luaK_exp2val</code>. */
  void kExp2val (Expdesc e) {
    if (e.hasjumps())
        kExp2anyreg(e);
    else
        kDischargevars(e);
  }

  /** TODO: int may become boolean or Boolean */
  private int boolK (boolean b) {
    return addk(Lua.valueOfBoolean(b));
  }

  private int nilK () {
    return addk(Lua.NIL);
  }

  /** Equivalent to <code>luaK_goiffalse</code>. */
  void kGoiffalse (Expdesc e) {
    int pc;  /* pc of last jump */
    kDischargevars(e);
    switch (e.k) {
      case Expdesc.VNIL:
      case Expdesc.VFALSE:
        pc = NO_JUMP;  /* always false; do nothing */
        break;

      case Expdesc.VTRUE:
        pc = kJump();  /* always jump */
        break;

      case Expdesc.VJMP:
        pc = e.info;
        break;

      default:
        pc = jumponcond(e, true);
        break;
    }
    kConcat(e.t, pc);  /* insert last jump in `t' list */
    kPatchtohere(e.f);
    e.f = NO_JUMP;
  }

  /** Equivalent to <code>luaK_goiftrue</code>. */
  void kGoiftrue (Expdesc e) {
    int pc;  /* pc of last jump */
    kDischargevars(e);
    switch (e.k) {
      case Expdesc.VK:
      case Expdesc.VKNUM:
      case Expdesc.VTRUE:
        pc = NO_JUMP;  /* always true; do nothing */
        break;

      case Expdesc.VFALSE:
        pc = kJump();  /* always jump */
        break;

      case Expdesc.VJMP:
        invertjump(e);
        pc = e.info;
        break;

      default:
        pc = jumponcond(e, false);
        break;
    }
    e.f = kConcat(e.f, pc);  /* insert last jump in `f' list */
    kPatchtohere(e.t);
    e.t = NO_JUMP;
  }

  private void invertjump (Expdesc e) {
    int pc = getjumpcontrol(e.info);
    // lua_assert(testTMode(GET_OPCODE(*pc)) && GET_OPCODE(*pc) != Lua.OP_TESTSET && GET_OPCODE(*pc) != Lua.OP_TEST);
    int [] code = f.code ;
    int instr = code[pc] ;
    code[pc] = Lua.SETARG_A(instr, (Lua.ARGA(instr) == 0 ? 1 : 0));
  }


  private int jumponcond (Expdesc e, boolean cond) {
    if (e.k == Expdesc.VRELOCABLE) {
      int ie = getcode(e);
      if (Lua.OPCODE(ie) == Lua.OP_NOT) {
        pc--;  /* remove previous OP_NOT */
        return condjump(Lua.OP_TEST, Lua.ARGB(ie), 0, cond ? 0 : 1);
      }
      /* else go through */
    }
    discharge2anyreg(e);
    freeexp(e);
    return condjump(Lua.OP_TESTSET, Lua.NO_REG, e.info, cond ? 1 : 0);
  }

  private int condjump (int op, int A, int B, int C) {
    kCodeABC(op, A, B, C);
    return kJump();
  }

  private void discharge2anyreg (Expdesc e) {
    if (e.k != Expdesc.VNONRELOC) {
      kReserveregs(1);
      discharge2reg(e, freereg-1);
    }
  }


  void kSelf (Expdesc e, Expdesc key) {
    kExp2anyreg(e);
    freeexp(e);
    int func = freereg;
    kReserveregs(2);
    kCodeABC(Lua.OP_SELF, func, e.info, kExp2RK(key));
    freeexp(key);
    e.info = func;
    e.k = Expdesc.VNONRELOC;
  }

  void kSetlist (int base, int nelems, int tostore) {
    int c =  (nelems - 1) / Syntax.LFIELDS_PER_FLUSH + 1;
    int b = (tostore == Lua.MULTRET) ? 0 : tostore;
    //lua_assert(tostore != 0);
    if (c <= Lua.MAXARG_C)
      kCodeABC(Lua.OP_SETLIST, base, b, c);
    else {
      kCodeABC(Lua.OP_SETLIST, base, b, 0);
      kCode(c, ls.lastline);
    }
    freereg = base + 1;  /* free registers with list values */
  }


  void codecomp (int op, boolean cond, Expdesc e1, Expdesc e2) {
    int o1 = kExp2RK(e1);
    int o2 = kExp2RK(e2);
    freeexp(e2);
    freeexp(e1);
    if ((!cond) && op != Lua.OP_EQ) {
      /* exchange args to replace by `<' or `<=' */
      int temp = o1; o1 = o2; o2 = temp;  /* o1 <==> o2 */
      cond = true;
    }
    e1.info = condjump(op, (cond ? 1 : 0), o1, o2);
    e1.k = Expdesc.VJMP;
  }
}
