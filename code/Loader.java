// $Header$

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;

/**
 * Loads Lua 5.1 binary chunks.
 * This loader is restricted to loading Lua 5.1 binary chunks where:
 * <ul>
 * <li><code>LUAC_VERSION</code> is <code>0x51</code>.</li>
 * <li><code>int</code> is 32 bits.</li>
 * <li><code>size_t</code> is 32 bits.</li>
 * <li><code>Instruction</code> is 32 bits (this is a type defined in
 * the PUC-Rio Lua).</li>
 * <li><code>lua_Number</code> is an IEEE 754 64-bit double.  Suitable
 * for passing to {@link java.lang.Double#longBitsToDouble}.</li>
 * <li>endianness does not matter (the loader swabs as appropriate).</li>
 * </ul>
 * Any Lua chunk compiled by a stock Lua 5.1 running on a 32-bit Windows
 * PC or at 32-bit OS X machine should be fine.
 */
final class Loader {
  /**
   * Whether integers in the binary chunk are stored big-endian or
   * little-endian.  Recall that the number 0x12345678 is stored: 0x12
   * 0x34 0x56 0x78 in big-endian format; and, 0x78 0x56 0x34 0x12 in
   * little-endian format.
   */
  private boolean bigendian;
  private InputStream in;
  private String name;
  /**
   * A new chunk loader.  The <code>InputStream</code> must be
   * positioned at the beginning of the <code>LUA_SIGNATURE</code> that
   * marks the beginning of a Lua binary chunk.
   * @param in    The binary stream from which the chunk is read.
   * @param name  The name of the chunk.
   */
  Loader(InputStream in, String name) {
    if (null == in) {
      throw new NullPointerException();
    }
    this.in = in;
    // The name is treated slightly.  See lundump.c in the PUC-Rio
    // source for details.
    if (name.startsWith("@") || name.startsWith("=")) {
      this.name = name.substring(1);
    } else if (false) {
      // :todo: Select some equivalent for the binary string case.
      this.name = "binary string";
    } else {
      this.name = name;
    }
  }

  /**
   * Loads (undumps) a dumped binary chunk.
   * @throws IOException  if chunk is malformed or unacceptable.
   */
  Proto undump() throws IOException {
    this.header();
    return this.function(null);
  }

  private byte byteLoad() {
    // :todo: implement me
    return 0;
  }

  /**
   * Undumps the code for a <code>Proto</code>.  The code is an array of
   * VM instructions.
   */
  private int[] code() {
    // :todo: implement me
    return null;
  }

  /**
   * Undumps the constant array contained inside a <code>Proto</code>
   * object.
   */
  private Object[] constant() {
    // :todo: implement me
    return null;
  }

  /**
   * Undumps the debug info for a <code>Proto</code>.
   * :todo: receive a Proto and decorate it with debug info.
   */
  private void debug() {
    // :todo: implement me
    return;
  }

  /**
   * Undumps a Proto object.  This is named 'function' after
   * <code>LoadFunction</code> in PUC-Rio's <code>lundump.c</code>.
   * @param parentSource_  Reserved for future expansion.
   * @throws IOException  when binary is malformed.
   */
  private Proto function(String parentSource_) throws IOException {
    String source;
    int linedefined, lastlinedefined;
    int nups, numparams;
    byte varargByte;
    boolean vararg;
    int maxstacksize;
    int[] code;
    Object[] constant;
    Proto[] proto;

    source = this.string();
    linedefined = this.intLoad();
    lastlinedefined = this.intLoad();
    nups = this.byteLoad();
    numparams = this.byteLoad();
    varargByte = this.byteLoad();
    if (varargByte > 1) {
      throw new IOException();
    }
    vararg = (0 != varargByte);
    maxstacksize = this.byteLoad();
    code = this.code();
    constant = this.constant();
    proto = this.proto();
    this.debug();
    // :todo: call code verifier
    return null;
  }

  private static final int HEADERSIZE = 12;

  /**
   * Loads and checks the binary chunk header.  Sets
   * <code>this.bigendian</code> accordingly.
   *
   * A Lua 5.1 header looks like this:
   * <pre>
   * b[0]    0x33
   * b[1..3] "Lua";
   * b[4]    0x51 (LUAC_VERSION)
   * b[5]    0 (LUAC_FORMAT)
   * b[6]    0 big-endian, 1 little-endian
   * b[7]    4 (sizeof(int))
   * b[8]    4 (sizeof(size_t))
   * b[9]    4 (sizeof(Instruction))
   * b[10]   8 (sizeof(lua_Number))
   * b[11]   0 (floating point)
   * </pre>
   *
   * To conserve JVM bytecodes the sizes of the types <code>int</code>,
   * <code>size_t</code>, <code>Instruction</code>,
   * <code>lua_Number</code> are assumed by the code to be 4, 4, 4, and
   * 8, respectively.  Where this assumption is made the tags :int:size,
   * :size_t:size :Instruction:size :lua_Number:size will appear so that
   * you can grep for them, should you wish to modify this loader to
   * load binary chunks from different architectures.
   *
   * @throws IOException  when header is malformed or not suitable.
   */

  private void header() throws IOException {
    byte[] buf = new byte[HEADERSIZE];
    int n;

    n = in.read(buf);
    if (n != buf.length) {
      throw new EOFException();
    }

    // A chunk header that is correct.  For comparison with the header
    // that we read.  The endian byte, at index 6, is copied so that it
    // always compares correctly; we cope with either endianness.
    byte[] golden = new byte[] {
        033, (byte)'L', (byte)'u', (byte)'a',
        0x51, 0, buf[6], 4,
        4, 4, 8, 0};

    if (buf[6] > 1 || !arrayEquals(golden, buf)) {
      throw new IOException();
    }
    bigendian = (buf[6] == 0);
  }

  private int intLoad() {
    // :todo: implement me
    return 0;
  }

  /**
   * Undumps the <code>Proto</code> array contained inside a
   * <code>Proto</code> object.  These are the <code>Proto</code>
   * objects for all inner functions defined inside an existing
   * function.
   */
  private Proto[] proto() {
    // :todo: implement me
    return null;
  }

  private String string() {
    // :todo: implement me
    return null;
  }
    

  /**
   * CLDC 1.1 does not provide <code>java.util.Arrays</code> so we make
   * do with this.
   */
  private static boolean arrayEquals(byte[] x, byte[] y) {
    if (x.length != y.length) {
      return false;
    }
    for (int i=0; i < x.length; ++i) {
      if (x[i] != y[i]) {
        return false;
      }
    }
    return true;
  }
}


