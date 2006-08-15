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

package mnj.lua;

/**
 * Class that models Lua's tables.  Each Lua table is an instance of
 * this class.
 */
public final class LuaTable extends java.util.Hashtable
{
  private LuaTable metatable;

  LuaTable()
  {
    super();
  }

  /**
   * Fresh LuaTable with hints for preallocating to size.
   * @param narray  number of array slots to preallocate.
   * @param nhash   number of hash slots to preallocate.
   */
  LuaTable(int narray, int nhash)
  {
    super(narray+nhash);
  }

  /**
   * Implements discriminating equality.  <code>o1.equals(o2) == (o1 ==
   * o2) </code>.  This method is not necessary in CLDC, it's only
   * necessary in J2SE because java.util.Hashtable overrides equals.
   * @param o  the reference to compare with.
   * @return true when equal.
   */
  public boolean equals(Object o)
  {
    return this == o;
  }

  /**
   * Provided to avoid Checkstyle warning.  This method is not necessary
   * for correctness (in neither JME nor JSE), it's only provided to
   * remove a Checkstyle warning.
   * Since {@link LuaTable#equals} implements the most discriminating
   * equality possible, this method can have any implementation.
   * @return an int.
   */
  public int hashCode()
  {
    return System.identityHashCode(this);
  }

  /**
   * Getter for metatable member.
   * @return  The metatable.
   */
  LuaTable getMetatable()
  {
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
  void setMetatable(LuaTable metatable)
  {
    this.metatable = metatable;
    return;
  }

  /** Like get for numeric (integer) keys. */
  Object getnum(int k)
  {
    return getlua(new Double(k));
  }

  /**
   * Like put for numeric (integer) keys.
   */
  void putnum(int k, Object v)
  {
    // The key can never be NIL so putlua will never notice that its L
    // argument is null.
    putlua(null, new Double(k), v);
  }

  /**
   * Supports Lua's length (#) operator.  More or less equivalent to
   * "unbound_search" in ltable.c.
   */
  int getn()
  {
    int i = 0;
    int j = 1;
    // Find 'i' and 'j' such that i is present and j is not.
    // Note that this test goes to the superclass get method directly.
    // This is unusual and only done in this case for speed.
    while (super.get(new Double(j)) != null)
    {
      i = j;
      j *= 2;
      if (j < 0)        // overflow
      {
        // Pathological case.  Linear search.
        i = 1;
        while (super.get(new Double(i)) != null)
        {
          ++i;
        }
        return i-1;
      }
    }
    // binary search between i and j
    while (j - i > 1)
    {
      int m = (i+j)/2;
      if (super.get(new Double(m)) == null)
      {
        j = m;
      }
      else
      {
        i = m;
      }
    }
    return i;
  }

  /**
   * Like {@link java.util.Hashtable#get}.  Ensures that indexes
   * with no value return {@link Lua#NIL}.  In order to get the correct
   * behaviour for <code>t[nil]</code>, this code assumes that Lua.NIL
   * is non-<code>null</code>.
   */
  Object getlua(Object key)
  {
    Object r = super.get(key);
    if (r == null)
    {
      r = Lua.NIL;
    }
    return r;
  }

  /**
   * Like {@link java.util.Hashtable#put} but enables Lua's semantics
   * for <code>nil</code>;
   * In particular that <code>x = nil</nil>
   * deletes <code>x</code>.
   * And also that <code>t[nil]</code> raises an error.
   * Generally, users of Jili should be using
   * {@link Lua#setTable} instead of this.
   * In Jili it is dangerous to use the return
   * value from this method (because it may be <code>null</code> which
   * is not a Lua value).
   * @param key key.
   * @param value value.
   * @return something not well defined.
   */
  Object putlua(Lua L, Object key, Object value)
  {
    if (key == Lua.NIL)
    {
      L.gRunerror("table index is nil");
    }
    // :todo: Consider checking key for NaN (PUC-Rio does)
    if (value == Lua.NIL)
    {
      return remove(key);
    }
    return super.put(key, value);
  }

  /**
   * Do not use, implementation exists only to generate deprecated
   * warning.
   * @deprecated Use getlua instead.
   */
  public Object get(Object key)
  {
    throw new IllegalArgumentException();
  }

  /**
   * Do not use, implementation exists only to generate deprecated
   * warning.
   * @deprecated Use putlua instead.
   */
  public Object put(Object key, Object value)
  {
    throw new IllegalArgumentException();
  }
}
