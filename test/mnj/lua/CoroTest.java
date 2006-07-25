// $Header$

package mnj.lua;

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestSuite;

// Auxiliary files
// none so far

/**
 * J2MEUnit tests for Jili's coroutine functionality.  DO NOT SUBCLASS.
 * public access granted only because j2meunit makes it necessary.
 */
public class CoroTest extends JiliTestCase
  {
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner CoroTest</code>
   */
  public CoroTest() { }

  /** Clones constructor from superclass.  */
  private CoroTest(String name)
  {
    super(name);
  }

  /** Ordinary completion of a thread. */
  public void test1()
  {
    Lua L = new Lua();
    BaseLib.open(L);
    L.push(L.getGlobal("tostring"));
    L.push("hello");
    int status = L.resume(1);
    assertTrue(status == 0);
    assertTrue(L.getTop() == 1);
  }

  /** Thread that yields. */
  public void test2()
  {
    final boolean[] yielded = { false };
    Lua L = new Lua();
    BaseLib.open(L);
    L.push(new LuaJavaCallback()
      {
        public int luaFunction(Lua L)
        {
          System.out.println("Yielding");
          yielded[0] = true;
          return L.yield(0);
        }
      });
    int status = L.resume(0);
    assertTrue("Status is YIELD", status == Lua.YIELD);
    assertTrue("Thread yielded", yielded[0]);
    assertTrue("Stack top", L.getTop() == 0);
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new CoroTest("test1")
      {
        public void runTest() { test1(); }
      });
    suite.addTest(new CoroTest("test2")
      {
        public void runTest() { test2(); }
      });

    return suite;
  }
}
