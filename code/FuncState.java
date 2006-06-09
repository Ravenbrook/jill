// $Header$

import java.util.Hashtable;

/** Used to model a function during compilation. */
final class FuncState {
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
}

