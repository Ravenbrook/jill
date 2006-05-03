// $Header$

/**
 * Models a Lua function.
 * Functions are created by loading Lua chunks (in source or binary
 * form) or executing Lua code which defines functions (and, for
 * example, places them in the global table).  @{link Lua.load} is used
 * to load a Lua chunk (it returns a <code>LuaFunction</code>,
 * and @{link Lua.call} is used to call a function.  A generic Lua
 * value, one retrieved from a table for example, can be converted to a
 * <code>LuaFunction</code> using @{link Lua.toFunction} or by a
 * narrowing reference conversion, which may of course throw an
 * exception.
 */
public final class LuaFunction {
  private UpVal[] upval;
  private LuaTable env;
  private Proto p;
}
