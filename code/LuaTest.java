// $Header$

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

import java.io.FileInputStream;
import java.io.InputStream;

// Uses some of the same ancillary files as LoaderTest.

/**
 * J2MEUnit tests for Jili's public API.  DO NOT SUBCLASS.  public
 * access granted only because j2meunit makes it necessary.
 * This test does not run in CLDC 1.1.
 */
public class LuaTest extends TestCase {
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner LuaTest</code>
   */
  public LuaTest() { }

  /** Clones constructor from superclass.  */
  private LuaTest(String name) {
    super(name);
  }

  /** Tests that we can create a Lua state. */
  public void testLua0() {
    Lua L = new Lua();
  }

  /** Tests loading and execution of simple file. */
  public void testLua1() {
    Lua L = new Lua();
    LuaFunction f = null;
    try {
      f = L.load(new FileInputStream("LoaderTest0.luc"), "LoaderTest0.luc");
    } catch (Exception e) {
      e.printStackTrace();
    }
    assertNotNull("Loaded script", f);
    L.push(f);
    int top = L.gettop();
    assertTrue("TOS == 1", 1 == top);
    L.call(0, 0);
    top = L.gettop();
    assertTrue("TOS == 0", 0 == top);
    L.push(f);
    L.call(0, 1);
    top = L.gettop();
    assertTrue("1 result", 1 == top);
    System.out.println(L.value(1));
  }

  public Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new LuaTest("testLua0") {
        public void runTest() { testLua0(); } });
    suite.addTest(new LuaTest("testLua1") {
        public void runTest() { testLua1(); } });
    return suite;
  }
}
