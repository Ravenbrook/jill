// $Header$

import java.util.Hashtable;

/** Used to model a function during compilation. */
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
  // :todo: chain of current blocks
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

  /** Equivalent to luaK_dischargevars. */
  void kDischargevars(Expdesc e) {
    switch (e.kind()) {
      // :todo: more cases
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

  /** Equivalent to luaK_infix. */
  void kInfix (int op, Expdesc e) {
    // :todo: implement me
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

  void codearith (int op, Expdesc e1, Expdesc e2) {
    // :todo: implement me
    return;
  }

  void codenot (Expdesc e) {
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
      case Expdesc.VKNUM:
        kCodeABx(Lua.OP_LOADK, reg, kNumberK(e.nval()));
        break;
      // :todo: more cases
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

  private void freeexp (Expdesc e) {
    if (e.kind() == Expdesc.VNONRELOC) {
      freereg(e.info());
    }
  }

  private void freereg (int reg) {
    if (!Lua.ISK(reg) && reg >= nactvar) {
      --freereg;
      // assert reg == freereg;
    }
  }
}

