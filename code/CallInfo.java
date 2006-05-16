// $Header$

final class CallInfo {
  private int savedpc;
  private Object function;

  /** Only used to create the first instance. */
  CallInfo() { }

  CallInfo(Object f, int base, int top, int nresults) {
    this.function = f;
  }

  void setSavedpc(int pc) {
    savedpc = pc;
  }

  Object function() {
    return function;
  }
}
