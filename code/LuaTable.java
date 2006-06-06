// $Header$

/**
 * Class that models Lua's tables.  Each Lua table is an instance of
 * this class.
 */
public final class LuaTable extends java.util.Hashtable {
  private LuaTable metatable;

  LuaTable() {
    super();
  }

  /**
   * Fresh LuaTable with hints for preallocating to size.
   * @param narray  number of array slots to preallocate.
   * @param nhash   number of hash slots to preallocate.
   */
  LuaTable(int narray, int nhash) {
    super(narray+nhash);
  }

  /** Implements discriminating equality.  <code>o1.equals(o2) == (o1 ==
   * o2) </code>.  This method is not necessary in CLDC, it's only
   * necessary in J2SE because java.util.Hashtable overrides equals.
   */
  public boolean equals(Object o) {
    return this == o;
  }

  /**
   * Getter for metatable member.
   * @return  The metatable.
   */
  LuaTable getMetatable() {
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
  void setMetatable(LuaTable metatable) {
    this.metatable = metatable;
    return;
  }

  /** Like get for numeric (integer) keys. */
  Object getnum(int k) {
    return get(new Double(k));
  }

  /**
   * Like put for numeric (integer) keys.
   */
  void putnum(int k, Object v) {
    put(new Double(k), v);
  }

  /**
   * Supports Lua's length (#) operator.  More or less equivalent to
   * "unbound_search" in ltable.c.
   */
  int getn() {
    int i = 0;
    int j = 1;
    // Find 'i' and 'j' such that i is present and j is not.
    while (get(new Double(j)) != null) {
      i = j;
      j *= 2;
      if (j < 0) {      // overflow
        // Pathological case.  Linear search.
        i = 1;
        while (get(new Double(i)) != null) {
          ++i;
        }
        return i-1;
      }
    }
    // binary search between i and j
    while (j - i > 1) {
      int m = (i+j)/2;
      if (get(new Double(m)) == null) {
        j = m;
      } else {
        i = m;
      }
    }
    return i;
  }
}
