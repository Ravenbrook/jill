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

import java.util.Vector;

/**
 * Models an upvalue.  This class is internal to Jili and should not be
 * used by clients.
 * This is the analogue of the UpVal type in PUC-Rio's Lua
 * implementation, hence the name.
 * An UpVal instance is a reference to a variable.
 * When initially created generally the variable is kept on the VM
 * stack.  When the function that defines that variable returns, the
 * corresponding stack slots are destroyed.  In order that the UpVal
 * continues to reference the variable, it is closed (using the
 * <code>close</code> method).  Lua functions that reference, via an
 * upvalue, the same instance of the same variable, will share an
 * <code>UpVal</code> (somewhere in their <code>upval</code> array
 * member).
 */
final class UpVal
{
  // An open UpVal (referencing the VM stack) has valid L and offset
  // fields, v is null.
  // A closed UpVal has a valid v field, L is null.
  // (L == null) is used to characterise a closed UpVal.
  private Lua L;
  private int offset;
  private Object v;

  /**
   * A fresh upvalue from a Lua state and an offset.
   * @param L The Lua thread.
   * @param offset index into Lua thread's VM stack, must be a valid index.
   * @throws NullPointerException if L is null.
   * @throws IllegalArgumentException if offset is negative or too big.
   */
  UpVal(Lua L, int offset)
  {
    if (null == L)
    {
      throw new NullPointerException();
    }
    if (offset < 0 || offset >= L.stack().length)
    {
      throw new IllegalArgumentException();
    }

    this.L = L;
    this.offset = offset;
  }

  /**
   * Getter for underlying value.
   */
  Object getValue()
  {
    if (L == null)
    {
      return v;
    }
    return L.stack()[offset];
  }

  /**
   * Setter for underlying value.
   */
  void setValue(Object o)
  {
    if (L == null)
    {
      v = o;
      return;
    }
    L.stack()[offset] = o;
  }

  /**
   * The stack offset.
   */
  int offset()
  {
    return offset;
  }

  /**
   * Closes an UpVal.  This ensures that the storage operated on by
   * {@link #getValue() getValue} and {@link #setValue(Object) setValue}
   * is not shared by any other object.
   * This is typically used when a function returns (executes
   * the <code>OP_RET</code> VM instruction).  Effectively this
   * transfers a variable binding from the stack to the heap.
   */
  void close()
  {
    v = getValue();
    L = null;
    offset = -1;
  }
}
