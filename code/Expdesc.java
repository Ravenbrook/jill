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

  void setKind(int kind) {
    this.k = kind;
  }

  int info() {
    return info;
  }

  void setInfo(int i) {
    this.info = i;
  }

  int aux() {
    return aux;
  }

  double nval() {
    return nval;
  }

  void setNval(double d) {
    this.nval = d;
  }

  /** Equivalent to hasmultret from lparser.c */
  boolean hasmultret() {
    return k == VCALL || k == VVARARG;
  }

  /** Equivalent to hasjumps from lcode.c. */
  boolean hasjumps() {
    return t != f;
  }

  void nonreloc(int i) {
    k = VNONRELOC;
    info = i;
  }

  void reloc(int i) {
    k = VRELOCABLE;
    info = i;
  }

  void upval(int i) {
    k = VUPVAL;
    info = i;
  }
}
