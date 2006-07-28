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
    String suffix[] = { "", ".luc", ".lua" };
    InputStream is = null;
    String s = "";
    for (int i=0; i<suffix.length; ++i)
    {
      s = filename + suffix[i];
      is = getClass().getResourceAsStream(s);
      if (is != null)
      {
        break;
      }
    }
    System.out.println(filename);
    int status = L.load(is, s);
    assertTrue("Loaded " + filename + " ok", status == 0);
  }

  protected void loadFileAndRun(Lua L, String file, String name, int n)
  {
    loadFile(L, file);
    L.call(0, 0);
    System.out.println(name);
    L.push(L.getGlobal(name));
    int status = L.pcall(0, n, new AddWhere());
    if (status != 0)
    {
      System.out.println(L.toString(L.value(-1)));
    }
    assertTrue(name, status == 0);
  }

  /**
   * Compiles/loads file and leaves LuaFunction on the stack.  Fails the test
   * if there was a problem loading the file.
   * @param L         Lua state in which to load file.
   * @param filename  filename without '.luc' extension.
   */
  protected void compileLoadFile(Lua L, String filename)
  {
    filename += ".lua";
    System.out.println(filename);
    InputStream is = getClass().getResourceAsStream(filename) ;
    assertTrue ("Found "+filename+" ok", is != null) ;
    int status =  L.load(is, filename);
    assertTrue("Compiled/loaded " + filename + " ok", status == 0);
  }
}

final class AddWhere extends LuaJavaCallback
{
  int luaFunction(Lua L)
  {
    boolean any = false;
    for (int i=1; i<=3; ++i)
    {
      String s = L.where(i);
      if (!s.equals(""))
      {
        if (any)
          s = s + " > ";
        any = true;
        L.insert(s, -1);
        L.concat(2);
      }
    }
    return 1;
  }
}
