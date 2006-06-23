/*  $Header$
 *  (c) Copyright 2002-2006, Intuwave Ltd. All Rights Reserved.
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

/**
 * Equivalent to struct lua_Debug.  This implementatino is incomplete
 * because it is not intended to form part of the public API.  It has
 * only been implemented to the extent necessary for internal use.
 */
public final class Debug {
  // private, not public accessors defined
  private int i_ci;

  // public accessors may be defined for these.
  private String what;
  private String source;
  private int currentline;
  private int linedefined;
  private int lastlinedefined;
  private String short_src;

  /**
   * @param i_ci  index of CallInfo record in L.civ
   */
  Debug(int i_ci) {
    this.i_ci = i_ci;
  }

  /**
   * Get i_ci, index of the {@link CallInfo} record.
   */
  int i_ci() {
    return i_ci;
  }

  /**
   * Sets the what field.
   */
  void setWhat(String what) {
    this.what = what;
  }

  /**
   * Sets the source, and the short_src.
   */
  void setSource(String source) {
    this.source = source;
    this.short_src = Lua.oChunkid(source);
  }

  /**
   * Gets the current line.  May become public.
   */
  int currentline() {
    return currentline;
  }

  /**
   * Set currentline.
   */
  void setCurrentline(int currentline) {
    this.currentline = currentline;
  }

  /**
   * Get linedefined.
   */
  int linedefined() {
    return linedefined;
  }

  /**
   * Set linedefined.
   */
  void setLinedefined(int linedefined) {
    this.linedefined = linedefined;
  }

  /**
   * Set lastlinedefined.
   */
  void setLastlinedefined(int lastlinedefined) {
    this.lastlinedefined = lastlinedefined;
  }

  /**
   * Gets the "printable" version of source, for error messages.
   * Mat become public.
   */
  String short_src() {
    return short_src;
  }
}
