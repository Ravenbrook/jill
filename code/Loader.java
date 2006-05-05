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

  /**
   * Primitive reader for undumping.
   * Reads exactly enough bytes from <code>this.in</code> to fill the
   * array <code>b</code>.  If there aren't enough to fill
   * <code>b</code> then an exception is thrown.  Similar to
   * <code>LoadBlock</code> from PUC-Rio's <code>lundump.c</code>.
   * @param b  byte array to fill.
   * @throws EOFException when the stream is exhausted too early.
   * @throws IOException when the underlying stream does.
   */
  private void block(byte []b) throws IOException {
    int n;

    n = in.read(b);
    if (n != b.length) {
      throw new EOFException();
    }
  }

  private byte byteLoad() throws IOException {
    byte[] buf = new byte[1];
    block(buf);
    return buf[0];
  }

  /**
   * Undumps the code for a <code>Proto</code>.  The code is an array of
   * VM instructions.
   */
  private int[] code() throws IOException {
    int n = intLoad();
    int[] code = new int[n];

    for (int i=0; i<n; ++i) {
      // :Instruction:size  Here we assume that a dumped Instruction is
      // the same size as a dumped int.
      code[i] = intLoad();
    }

    return code;
  }

  /**
   * Undumps the constant array contained inside a <code>Proto</code>
   * object.  First half of <code>LoadConstants</code>, see
   * <code>proto</code> for the second half of
   * <code>LoadConstants</code>.
   */
  private Object[] constant() throws IOException {
    int n = intLoad();
    Object[] k = new Object[n];

    // Load each constant one by one.  We use the following values for
    // the Lua tagtypes (taken from <code>lua.h</code> from the PUC-Rio
    // Lua 5.1 distribution):
    // LUA_TNIL         0
    // LUA_TBOOLEAN     1
    // LUA_TNUMBER      3
    // LUA_TSTRING      4
    // All other tagtypes are invalid
    for (int i=0; i<n; ++i) {
      int t = byteLoad();
      switch (t) {
        case 0: // LUA_TNIL
          k[i] = null;
          break;

        case 1: // LUA_TBOOLEAN
          byte b = byteLoad();
          if (b > 1) {
            throw new IOException();
          }
          k[i] = Lua.valueOfBoolean(b != 0);
          break;

        case 3: // LUA_TNUMBER
          k[i] = number();
          break;

        case 4: // LUA_TSTRING
          k[i] = string();
          break;

        default:
          throw new IOException();
      }
    }

    return k;
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
    // Curiously "is_vararg" byte values of 0 or 2 are the only ones we
    // accept.  It's a 3-bit field, the remaining bits (bit 0 and bit 2)
    // are used for the 5.0 compatibility 'arg' style varargs, which are
    // deprecated and not supported by Jili.
    if (!(varargByte == 0 || varargByte == 2)) {
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

    block(buf);

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

  /**
   * Undumps an int.  This is the only method that needs to swab.
   * size_t and Instruction need swabbing too, but the code 
   * simply uses this method to load size_t and Instruction.
   */
  private int intLoad() throws IOException {
    // :size:int  Here we assume an int is 4 bytes.
    byte buf[] = new byte[4];
    block(buf);

    int i;
    if (bigendian) {
      i = (buf[0] << 24) | (buf[1] << 16) | (buf[2] << 8) | buf[3];
    } else {
      i = (buf[3] << 24) | (buf[2] << 16) | (buf[1] << 8) | buf[0];
    }
    return i;
  }

  /**
   * Undumps a Lua number.  Which is assumed to be a 64-bit IEEE double.
   */
  private Object number() throws IOException {
    // :lua_Number:size  Here we assume that the size is 8.
    byte[] buf = new byte[8];
    block(buf);
    // We assume that doubles are always stored with the sign bit first.
    long l = 0;
    for (int i=0; i<buf.length; ++i) {
      l = (l << 8) | buf[i];
    }
    double d = Double.longBitsToDouble(l);
    return Lua.valueOfNumber(d);
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

  /**
   * Undumps a String or null.  As per <code>LoadString</code> in
   * PUC-Rio's lundump.c.  Strings are converted from the binary
   * according to the default character encoding, using the {@link
   * java.lang.String#String(byte[]) String(byte[])} constructor.
   */
  private String string() throws IOException {
    // :size_t:size we assume that size_t is same size as int.
    int size = intLoad();
    if (0 == size) {
      return null;
    }

    byte buf[] = new byte[size-1];
    block(buf);
    // Discard trailing NUL byte
    block(new byte[1]);

    return new String(buf);
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


