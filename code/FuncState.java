// $Header$

import java.util.Hashtable;

/** Used to model a function during compilation. */
final class FuncState {
  /** See NO_JUMP in lcode.h. */
  private static final int NO_JUMP = -1;

  /** Proto object for this function. */
  Proto f;
  /** Table to find (and reuse) elements in <var>f.k</var>. */
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

  /** Equivalent to luaK_ret. */
  void kRet(int first, int nret) {
    kCodeABC(Lua.OP_RETURN, first, nret+1, 0);
  }

  void dischargejpc() {
    // :todo: implement me
    jpc = NO_JUMP;
  }
}

