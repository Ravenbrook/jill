// $Header$

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
final class UpVal {
  private Vector a;
  private int offset;
  /**
   * A fresh upvalue from a Vector and an offset.
   * @param a  Vector of Lua values (usually the VM stack).
   * @param offset index into vector, must be a valid index.
   * @throws NullPointerException if a is null.
   * @throws IllegalArgumentException if offset is negative or too big.
   */
  UpVal(Vector a, int offset) {
    if (null == a) {
      throw new NullPointerException();
    }
    if (offset < 0 || offset >= a.size()) {
      throw new IllegalArgumentException();
    }

    this.a = a;
    this.offset = offset;
  }

  /**
   * Getter for underlying value.
   */
  Object getValue() {
    return a.elementAt(offset);
  }
  /**
   * Setter for underlying value.
   */
  void setValue(Object o) {
    a.setElementAt(o, offset);
  }
  /**
   * The stack offset.
   */
  int offset() {
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
  void close() {
    Object o = getValue();
    a = new Vector(1, 0);
    a.addElement(o);
    offset = 0;
  }
}
