// $Header$

/**
 * Class that models Lua's tables.  Each Lua table is an instance of
 * this class.
 */
public final class LuaTable extends java.util.Hashtable {
  Object metatable;
  /**
   * Getter for metatable member.
   * @return  The metatable.
   */
  Object getMetatable() {
    return metatable;
  }
  /**
   * Setter for metatable member.
   * @param metatable  The metatable.
   */
  // :todo: Support metatable's __gc and __mode keys appropriately.
  //        This involves detecting when those keys are present in the
  //        metatable, and changing all the entries in the Hashtable
  //        to be instance of java.lang.Ref as appropriate.
  void setMetatable(Object metatable) {
    this.metatable = metatable;
    return;
  }
}
