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


/**
 * Models an arbitrary Java reference as a Lua value.
 * This class provides a facility that is equivalent to the userdata
 * facility provided by the PUC-Rio implementation.  It has two primary
 * uses: the first is when you wish to store an arbitrary Java reference
 * in a Lua table; the second is when you wish to create a new Lua type
 * by defining an opaque object with metamethods.  The former is
 * possible because a <code>LuaUserdata</code> can be stored in tables,
 * and passed to functions, just like any other Lua value.  The latter
 * is possible because each <code>LuaUserdata</code> supports a
 * metatable.
 */
public final class LuaUserdata
{
  private Object userdata;
  private LuaTable metatable;
  private LuaTable env;
  /**
   * Wraps an arbitrary Java reference.  To retrieve the reference that
   * was wrapped, use @{link Lua.toUserdata}.
   * @param  o The Java reference to wrap.
   */
  public LuaUserdata(Object o)
  {
    userdata = o;
  }

  /**
   * Getter for userdata.
   * @return the userdata that was passed to the constructor of this
   * instance.
   */
  Object getUserdata()
  {
    return userdata;
  }

  /**
   * Getter for metatable.
   * @return the metatable.
   */
  LuaTable getMetatable()
  {
    return metatable;
  }
  /**
   * Setter for metatable.
   * @param metatable The metatable.
   */
  void setMetatable(LuaTable metatable)
  {
    this.metatable = metatable;
  }

  /**
   * Getter for environment.
   * @return The environment.
   */
  LuaTable getEnv()
  {
    return env;
  }
  /**
   * Setter for environment.
   * @param env  The environment.
   */
  void setEnv(LuaTable env)
  {
    this.env = env;
  }
}
