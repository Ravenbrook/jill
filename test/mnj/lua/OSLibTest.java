// $Header$

package mnj.lua;

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestSuite;

// Auxiliary files
// OSLibTest.lua - Lua source for test.
// OSLibTest.luc - compiled version of OSLibTest.lua

/**
 * J2MEUnit tests for Jili's OSLib (os library).  DO NOT SUBCLASS.
 * public access granted only because j2meunit makes it necessary.
 */
public class OSLibTest extends JiliTestCase
  {
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner OSLibTest</code>
   */
  public OSLibTest() { }

  /** Clones constructor from superclass.  */
  private OSLibTest(String name)
  {
    super(name);
  }

  /**
   * Tests OSLib.
   */
  public void testOSLib()
  {
    System.out.println("OSLibTest.testOSLib()");
    Lua L = new Lua();

    OSLib.open(L);

    Object lib = L.getGlobal("os");
    assertTrue("os table defined", L.isTable(lib));
  }

  /**
   * Opens the base and os libraries into a fresh Lua state,
   * calls a global function, and returns the Lua state.
   * @param name  name of function to call.
   * @param n     number of results expected from function.
   */
  private Lua luaGlobal(String name, int n)
  {
    Lua L = new Lua();
    BaseLib.open(L);
    OSLib.open(L);
    loadFile(L, "OSLibTest");
    L.call(0, 0);
    System.out.println(name);
    L.push(L.getGlobal(name));
    L.call(0, n);
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

  public void testclock()
  {
    nTrue("testclock", 2);
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new OSLibTest("testOSLib")
      {
        public void runTest() { testOSLib(); }
      });
    suite.addTest(new OSLibTest("testclock")
      {
        public void runTest() { testclock(); }
      });

    return suite;
  }
}
