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
 * member); hence they share updates to the variable.
 */
final class UpVal
{
  /**
   * The offset field.  Stored here, but not actually used directly by
   * this class.
   * Used (by {@Lua}) when searching for {@link UpVal} instances.
   * An open UpVal has a valid offset field.  Its slot is shared
   * with a slot of the VM stack.
   * A closed UpVal has offset == -1.  It's slot will be a fresh copy
   * and not shared with any other.
   */
  private int offset;
  /**
   * The slot object used to store the Lua value.
   */
  private Slot s;

  /**
   * A fresh upvalue from an offset, and a slot.
   * Conceptually <var>offset</var> and <var>slot</var> convey the same
   * information, only one is necessary since the offset implies the
   * slot and vice-versa.  <var>slot</var> is used to directly reference
   * the value (this avoids an indirection to the VM stack). <var>offset</var>
   * is used when searching for UpVals in the openupval list; this
   * happens when closing UpVals (function return) or creating them
   * (execution of functon declaration).
   * @param offset  index into Lua thread's VM stack, must be a valid index.
   * @param s  Slot corresponding to offset.
   * @throws NullPointerException if L is null.
   */
  UpVal(int offset, Slot s)
  {
    this.offset = offset;
    this.s = s;
  }

  /**
   * Getter for underlying value.
   */
  Object getValue()
  {
    return s.asObject();
  }

  /**
   * Setter for underlying value.
   */
  void setValue(Object o)
  {
    s.setObject(o);
  }

  /**
   * The offset.
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
    s = new Slot(s);
    offset = -1;
  }
}
