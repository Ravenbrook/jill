// $Header$

final class CallInfo {
  private int savedpc;

  /** Only used to create the first instance. */
  CallInfo() { }

  CallInfo(Object f, int base, int top, int nresults) { }

  void setSavedpc(int pc) {
    savedpc = pc;
  }
}
