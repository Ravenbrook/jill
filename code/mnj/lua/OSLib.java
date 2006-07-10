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

package mnj.lua;

import java.util.Date;

/**
 * The OS Library.  Can be opened into a {@link Lua} state by invoking
 * the {@link OSLib#open} method.
 */
public final class OSLib extends LuaJavaCallback {
  // Each function in the library corresponds to an instance of
  // this class which is associated (the 'which' member) with an integer
  // which is unique within this class.  They are taken from the following
  // set.
  private static final int CLOCK = 1;
  private static final int DATE = 2;
  private static final int DIFFTIME = 3;
  // EXECUTE = 4;
  // EXIT = 5;
  // GETENV = 6;
  // REMOVE = 7;
  // RENAME = 8;
  // SETLOCALE = 9;
  private static final int TIME = 10;

  /**
   * Which library function this object represents.  This value should
   * be one of the "enums" defined in the class.
   */
  private int which;

  /** Constructs instance, filling in the 'which' member. */
  private OSLib(int which)
  {
    this.which = which;
  }

  /**
   * Implements all of the functions in the Lua os library (that are
   * provided).  Do not call directly.
   * @param L  the Lua state in which to execute.
   * @return number of returned parameters, as per convention.
   */
  public int luaFunction(Lua L)
  {
    switch (which)
    {
      case CLOCK:
        return clock(L);
      case DATE:
        return date(L);
    }
    return 0;
  }

  /**
   * Opens the library into the given Lua state.  This registers
   * the symbols of the library in the table "os".
   * @param L  The Lua state into which to open.
   */
  public static void open(Lua L)
  {
    Object lib = new LuaTable();
    L.setGlobal("os", lib);

    r(L, "clock", CLOCK);
    r(L, "date", DATE);
    r(L, "difftime", DIFFTIME);
    r(L, "time", TIME);
  }

  /** Register a function. */
  private static void r(Lua L, String name, int which)
  {
    OSLib f = new OSLib(which);
    Object lib = L.getGlobal("os");
    L.setField(lib, name, f);
  }

  private static final long T0 = System.currentTimeMillis();

  /** Implements clock.  Java provides no way to get CPU time, so we
   * return the amount of wall clock time since this class was loaded.
   */
  private static int clock(Lua L)
  {
    double d = (double)System.currentTimeMillis();
    d = d - T0;
    d /= 1000;

    L.pushNumber(d);
    return 1;
  }

  /** Implements date. */
  private static final int date(Lua L)
  {
    long t;
    if (L.isnoneornil(2))
    {
      t = System.currentTimeMillis();
    }
    else
    {
      t = (long)L.checkNumber(2);
    }
    Date d = new Date(t);
    // :todo: implement all the formats of the first argument.
    L.pushString(d.toString());
    return 1;
  }
}
