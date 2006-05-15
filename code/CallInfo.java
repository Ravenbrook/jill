// $Header$

final class CallInfo {
  private int savedpc;

  CallInfo(Object f, int base, int top, int nresults) { }

  void setSavedpc(int pc) {
    savedpc = pc;
  }
}
