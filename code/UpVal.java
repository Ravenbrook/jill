// $Header$

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
  private Object[] array;
  private int offset;
  /**
   * A fresh upvalue from an array and an offset.
   * @param array  Array of Lua values (usually the VM stack).
   * @param offset index into array, must be a valid index.
   * @throws NullPointerException if array is null.
   * @throws IllegalArgumentException if offset is negative or >= array.length.
   */
  UpVal(Object[] array, int offset) {
    if (null == array) {
      throw new NullPointerException();
    }
    if (offset < 0 || offset >= array.length) {
      throw new IllegalArgumentException();
    }

    this.array = array;
    this.offset = offset;
  }

  /**
   * Getter for underlying value.
   */
  Object getValue() {
    return array[offset];
  }
  /**
   * Setter for underlying value.
   */
  void setValue(Object o) {
    array[offset] = o;
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
    array = new Object[] { array[offset] };
    offset = 0;
  }
}
