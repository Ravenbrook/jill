// $Header$

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
public final class LuaUserdata {
  private Object userdata;
  private Object metatable;
  /**
   * Wraps an arbitrary Java reference.  To retrieve the reference that
   * was wrapped, use @{link Lua.toUserdata}.
   * @param  o The Java reference to wrap.
   */
  public LuaUserdata(Object o) {
    userdata = o;
  }

  /**
   * Getter for userdata.
   * @return the userdata that was passed to the constructor of this
   * instance.
   */
  Object getUserdata() {
    return userdata;
  }

  /**
   * Getter for metatable.
   * @return the metatable.
   */
  Object getMetatable() {
    return metatable;
  }
  /**
   * Setter for metatable.
   * @param metatable The metatable.
   */
  void setMetatable(Object metatable) {
    this.metatable = metatable;
  }
}
