// $Header$

import java.io.InputStream;
import java.io.Reader;

/**
 * Encapsulates a Lua execution environment.
 */
public final class Lua {
  /**
   * Gets the global environment.  The global environment, where global
   * variables live, is returned as a <code>LuaTable</code>.  Note that
   * modifying this table has exactly the same effect as creating or
   * changing global variables from within Lua.
   * @return  The global environment as a table.
   */
  public LuaTable getGlobals() { return null; }

  /**
   * Loads a Lua chunk in binary or source form.
   * Comparable to C's lua_load.  If the chunk is determined to be
   * binary then it is loaded directly.  Otherwise the chunk is assumed
   * to be a Lua source chunk and compilation is required first.  The
   * <code>InputStream</code> is used to create a <code>Reader</code>
   * (using the {@link InputStreamReader#InputStreamReader(InputStream)}
   * constructor) and the Lua source is compiled.
   * @param in         The binary chunk as an InputStream, for example from
   *                   {@link Class#getResourceAsStream}.
   * @param chunkname  The name of the chunk.
   * @return           The chunk as a function.
   */
  public LuaFunction load(InputStream in, String chunkname) { return null; }

  /**
   * Loads a Lua chunk in source form.
   * Comparable to C's lua_load.  Since this takes a Reader parameter,
   * this method is restricted to loading Lua chunks in source form.
   * @param in         The source chunk as a Reader, for example from
   *                   <code>java.io.InputStreamReader(Class.getResourceAsStream)</code>.
   * @param chunkname  The name of the chunk.
   * @return           The chunk as a function (after having been compiled).
   * @see java.io.InputStreamReader
   */
  public LuaFunction load(Reader in, String chunkname) { return null; }
  /**
   * Provide <code>Reader</code> interface over a <code>String</code>.
   * Equivalent of {@link java.io.StringReader#StringReader} from J2SE.
   * The ability to convert a <code>String</code> to a
   * <code>Reader</code> is required internally,
   * to provide the Lua function <code>loadstring</code>; exposed
   * externally as a convenience.
   */
  public Reader StringReader(String s) { return null; }
}

