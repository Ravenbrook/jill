// $Header$

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

// Uses some of the same ancillary files as LoaderTest.

/**
 * J2MEUnit tests for Jili's public API.  DO NOT SUBCLASS.  public
 * access granted only because j2meunit makes it necessary.
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

  /** Helper used by testLua1. */
  public void simpleScript(String filename) {
    Lua L = new Lua();
    LuaFunction f = null;
    System.out.println(filename);
    try {
      f = L.load(this.getClass().getResourceAsStream(filename),
          filename);
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
    Object r = L.value(1);
    assertTrue("result is 99.0", ((Double)r).doubleValue() == 99.0);
  }

  /** Test loading and executition of simple file.  LoaderTest0.luc and
   * LoaderTest3.luc are compiled from the same Lua source, but '0' is
   * compiled on a big-endian architecture, and '3' is compiled on a
   * little-endian architecture.
   */
  public void testLua1() {
    simpleScript("LoaderTest0.luc");
    simpleScript("LoaderTest3.luc");
  }

  /** Tests that a Lua Java function can be called. */
  public void testLua2() {
    Lua L = new Lua();
    final Object[] v = new Object[1];
    final Object MAGIC = new Object();
    class Mine extends LuaJavaCallback {
      int luaFunction(Lua L) {
        v[0] = MAGIC;
        return 0;
      }
    }
    L.push(new Mine());
    L.call(0, 0);
    assertTrue("Callback got called", v[0] == MAGIC);
  }

  public Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new LuaTest("testLua0") {
        public void runTest() { testLua0(); } });
    suite.addTest(new LuaTest("testLua1") {
        public void runTest() { testLua1(); } });
    suite.addTest(new LuaTest("testLua2") {
        public void runTest() { testLua2(); } });
    return suite;
  }
}
