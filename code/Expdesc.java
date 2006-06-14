// $Header$

/** Equivalent to struct expdesc. */
final class Expdesc {

  static final int VVOID = 0;           // no value
  static final int VNIL = 1;
  static final int VTRUE = 2;
  static final int VFALSE = 3;
  static final int VK = 4;              // info = index into 'k'
  static final int VKNUM = 5;           // nval = numerical value
  static final int VLOCAL = 6;          // info = local register
  static final int VUPVAL = 7;          // info = index into 'upvalues'
  static final int VGLOBAL = 8;         // info = index of table; 
                                        // aux = index of global name in 'k'
  static final int VINDEXED = 9;        // info = table register
                                        // aux = index register (or 'k')
  static final int VJMP = 10;           // info = instruction pc
  static final int VRELOCABLE = 11;     // info = instruction pc
  static final int VNONRELOC = 12;      // info = result register
  static final int VCALL = 13;          // info = instruction pc
  static final int VVARARG = 14;        // info = instruction pc

  private int k;        // one of V* enums above
  private int info;
  private int aux;
  private double nval;
  private int t;
  private int f;
  
  Expdesc() { }

  Expdesc(int k, int i) {
    init(k, i);
  }

  /** Equivalent to init_exp from lparser.c */
  void init(int k, int i) {
    this.f = this.t = FuncState.NO_JUMP;
    this.k = k;
    this.info = i;
  }

  int kind() {
    return k;
  }

  int info() {
    return info;
  }

  double nval() {
    return nval;
  }

  void setNval(double d) {
    this.nval = d;
  }

  /** Equivalent to hasjumps from lcode.c. */
  boolean hasjumps() {
    return t != f;
  }

  void nonreloc(int i) {
    k = VNONRELOC;
    info = i;
  }
}