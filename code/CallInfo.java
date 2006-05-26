// $Header$

final class CallInfo {
  private int savedpc;
  private int func;
  private int base;
  private int top;
  private int nresults;
  private int tailcalls;

  /** Only used to create the first instance. */
  CallInfo() { }

  /**
   * @param func  stack index of function
   * @param base  stack base for this frame
   * @param top   top-of-stack for this frame
   * @param nresults  number of results expected by caller
   */
  CallInfo(int func, int base, int top, int nresults) {
    this.func = func;
    this.base = base;
    this.top = top;
    this.nresults = nresults;
  }

  /** Setter for savedpc. */
  void setSavedpc(int pc) {
    savedpc = pc;
  }
  /** Getter for savedpc. */
  int savedpc() {
    return savedpc;
  }

  /**
   * Get the stack index for the function object for this record.
   */
  int function() {
    return func;
  }

  /**
   * Get stack index where results should end up.  This is an absolute
   * stack index, not relative to L.base.
   */
  int res() {
    // Same location as function.
    return func;
  }

  /**
   * Get stack base for this record.
   */
  int base() {
    return base;
  }

  /**
   * Get top-of-stack for this record.  This is the number of elements
   * in the stack (or will be when the function is resumed).
   */
  int top() {
    return top;
  }
  /**
   * Get number of results expected by the caller of this function.
   * Used to adjust the returned results to the correct number.
   */
  int nresults() {
    return nresults;
  }
  /**
   * Used during tailcall to set the base and top members.
   */
  void tailcall(int base, int top) {
    this.base = base;
    this.top = top;
    ++tailcalls;
  }
}
