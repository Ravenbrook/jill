// $Header$

package mnj.lua;

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestSuite;

// Auxiliary files
// TableLibTest.lua - test functions.
// TableLibTest.luc - compiled .lua file.

/**
 * J2MEUnit tests for Jili's TableLib (table library).  DO NOT SUBCLASS.
 * public access granted only because j2meunit makes it necessary.
 */
public class TableLibTest extends JiliTestCase
  {
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner TableLibTest</code>
   */
  public TableLibTest() { }

  /** Clones constructor from superclass.  */
  private TableLibTest(String name)
  {
    super(name);
  }

  /**
   * Tests TableLib.
   */
  public void testTableLib()
  {
    System.out.println("TableLibTest.testTableLib()");
    Lua L = new Lua();

    TableLib.open(L);

    Object lib = L.getGlobal("table");
    assertTrue("table table defined", L.isTable(lib));

    // Test that each table library name is defined as expected.
    String[] name =
    {
      "concat",
      "insert",
      "maxn",
      "remove",
      "sort"
    };
    for (int i=0; i<name.length; ++i)
    {
      Object o = L.getField(lib, name[i]);
      assertTrue(name[i] + " exists", !L.isNil(o));
    }
  }

  /**
   * Opens the base and table libraries into a fresh Lua state,
   * calls a global function, and returns the Lua state.
   * @param name  name of function to call.
   * @param n     number of results expected from function.
   */
  private Lua luaGlobal(String name, int n)
  {
    Lua L = new Lua();
    BaseLib.open(L);
    TableLib.open(L);
    loadFileAndRun(L, "TableLibTest", name, n);
    return L;
  }

  /**
   * Calls a global lua function and checks that <var>n</var> results
   * are all true.
   */
  private void nTrue(String name, int n)
  {
    Lua L = luaGlobal(name, n);
    for (int i=1; i<=n; ++i)
    {
      assertTrue("Result " + i + " is true",
          L.valueOfBoolean(true).equals(L.value(i)));
    }
  }

  public void testsort()
  {
    nTrue("testsort", 1);
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new TableLibTest("testTableLib")
      {
        public void runTest() { testTableLib(); }
      });
    suite.addTest(new TableLibTest("testsort")
      {
        public void runTest() { testsort(); }
      });

    return suite;
  }
}
