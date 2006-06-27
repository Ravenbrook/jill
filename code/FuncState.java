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
  // :todo: upvalues. */
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
    f.closeLocvar(nlocvars);
    f.closeUpvalue();
    Lua.gCheckcode(f);
    // assert bl == null;
  }

  /** Equivalent to getlocvar from lparser.c.
   * Accesses <code>LocVar</code>s of the {@link Proto}.
   */
  LocVar getlocvar(int idx) {
    return f.locvar()[actvar[idx]];
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
  void kInfix(int op, Expdesc e) {
    // :todo: implement me
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
    // :todo: implement me.
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

  void codearith(int op, Expdesc e1, Expdesc e2) {
    // :todo: implement me
    return;
  }

  void codenot(Expdesc e) {
    kDischargevars(e);
    // :todo: implement me
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

  private int getcode(Expdesc e) {
    return f.code()[e.info()];
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
      if (n == getlocvar(i).name()) {
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
  private void kExp2val (Expdesc e) {
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

}
