// $Header$

final class LocVar {
  private final String name;
  private final int startpc;
  private int endpc;

  LocVar(String name, int startpc, int endpc) {
    this.name = name;
    this.startpc = startpc;
    this.endpc = endpc;
  }

  void setEndpc(int endpc) {
    this.endpc = endpc;
  }
}
