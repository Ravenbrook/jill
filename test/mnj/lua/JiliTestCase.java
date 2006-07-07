// $Header$

package mnj.lua;
import java.io.InputStream ;
import j2meunit.framework.TestCase;

/** Common superclass for all Jili's (j2meunit) tests. */

class JiliTestCase extends TestCase
{
  JiliTestCase() { }

  JiliTestCase(String s)
  {
    super(s);
  }

  /**
   * Loads file and leaves LuaFunction on the stack.  Fails the test if
   * there was a problem loading the file.
   * @param L         Lua state in which to load file.
   * @param filename  filename without '.luc' extension.
   */
  protected void loadFile(Lua L, String filename)
  {
    filename += ".luc";
    System.out.println(filename);
    int status = L.load(this.getClass().getResourceAsStream(filename), filename);
    assertTrue("Loaded " + filename + " ok", status == 0);
  }

  /**
   * Compiles/loads file and leaves LuaFunction on the stack.  Fails the test if
   * there was a problem loading the file.
   * @param L         Lua state in which to load file.
   * @param filename  filename without '.luc' extension.
   */
  protected void compileLoadFile(Lua L, String filename)
  {
    filename += ".lua";
    System.out.println(filename);
    InputStream is = this.getClass().getResourceAsStream(filename) ;
    assertTrue ("Found "+filename+" ok", is != null) ;
    int status =  L.load(is, filename);
    assertTrue("Compiled/loaded " + filename + " ok", status == 0);
  }
}
