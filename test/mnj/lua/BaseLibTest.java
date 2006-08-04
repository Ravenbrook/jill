// $Header$

package mnj.lua;
// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestSuite;

// Auxiliary files
// BaseLibTestLoadfile.luc
//   return 99
// BaseLibTest.lua - contains functions that test each of base library
// functions.  It is important (for testing "error") that this file is
// not stripped if it is loaded in binary form.

// :todo: test radix conversion for tonumber.
// :todo: test unpack with non-default arguments.
// :todo: test rawequal for things with metamethods.
// :todo: test rawget for tables with metamethods.
// :todo: test rawset for tables with metamethods.
// :todo: (when string library is available) test the strings returned
//     by assert.


/**
 * J2MEUnit tests for Jili's BaseLib (base library).  DO NOT SUBCLASS.
 * public access granted only because j2meunit makes it necessary.
 */
public class BaseLibTest extends JiliTestCase
{
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner BaseLibTest</code>
   */
  public BaseLibTest() { }

  /** Clones constructor from superclass.  */
  private BaseLibTest(String name)
  {
    super(name);
  }

  /**
   * Tests BaseLib.
   */
  public void testBaseLib()
  {
    System.out.println("BaseLibTest.testBaseLib()");
    Lua L = new Lua();

    BaseLib.open(L);

    // Test that each global name is defined as expected.
    String[] name =
    {
      "_VERSION",
      "_G", "ipairs", "pairs", "print", "rawequal", "rawget", "rawset",
      "select", "tonumber", "tostring", "type", "unpack"
    };
    for (int i=0; i<name.length; ++i)
    {
      Object o = L.getGlobal(name[i]);
      assertTrue(name[i] + " exists", !L.isNil(o));
    }
  }

  /**
   * Opens the base library into a fresh Lua state, calls a global
   * function, and returns the Lua state.
   * @param name  name of function to call.
   * @param n     number of results expected from function.
   */
  private Lua luaGlobal(String name, int n)
  {
    Lua L = new Lua();
    BaseLib.open(L);
    loadFile(L, "BaseLibTest");
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

  /**
   * Tests print.  Not much we can reasonably do here apart from call
   * it.  We can't automatically check that the output appears anywhere
   * or is correct.  This also tests tostring to some extent; print
   * calls tostring internally, so this tests that it can be called
   * without error, for example.
   */
  public void testPrint()
  {
    luaGlobal("testprint", 0);
  }

  public void testTostring()
  {
    nTrue("testtostring", 5);
  }

  public void testTonumber()
  {
    nTrue("testtonumber", 5);
  }

  public void testType()
  {
    nTrue("testtype", 6);
  }

  public void testSelect()
  {
    nTrue("testselect", 2);
  }

  public void testUnpack()
  {
    nTrue("testunpack", 1);
  }

  public void testPairs()
  {
    nTrue("testpairs", 4);
  }

  public void testNext()
  {
    nTrue("testnext", 4);
  }

  public void testIpairs()
  {
    nTrue("testipairs", 4);
  }

  public void testRawequal()
  {
    nTrue("testrawequal", 7);
  }

  public void testRawget()
  {
    nTrue("testrawget", 2);
  }

  public void testRawset()
  {
    nTrue("testrawset", 2);
  }

  public void testGetfenv()
  {
    nTrue("testgetfenv", 1);
  }

  public void testSetfenv()
  {
    nTrue("testsetfenv", 1);
  }

  public void testPcall()
  {
    nTrue("testpcall", 2);
  }

  public void testError()
  {
    nTrue("testerror", 2);
  }

  public void testMetatable()
  {
    nTrue("testmetatable", 2);
  }

  public void test__metatable()
  {
    nTrue("test__metatable", 2);
  }

  public void test__tostring()
  {
    nTrue("test__tostring", 1);
  }

  public void testCollectgarbage()
  {
    nTrue("testcollectgarbage", 1);
  }

  public void testAssert()
  {
    nTrue("testassert", 1);
  }

  public void testLoadstring()
  {
    nTrue("testloadstring", 1);
  }

  public void testLoadfile()
  {
    nTrue("testloadfile", 1);
  }

  public void testLoad()
  {
    nTrue("testload", 1);
  }

  public void testDofile()
  {
    nTrue("testdofile", 1);
  }

  /** Tests _VERSION */
  public void testVersion()
  {
    Lua L = new Lua();
    BaseLib.open(L);

    Object o = L.getGlobal("_VERSION");
    assertTrue("_VERSION exists", o != null);
    assertTrue("_VERSION is a string", L.isString(o));
  }

  public void testXpcall()
  {
    nTrue("testxpcall", 1);
  }

  public void testErrormore()
  {
    nTrue("testerrormore", 2);
  }

  public void testpcall2()
  {
    nTrue("testpcall2", 4);
  }

  public void testpcall3()
  {
    nTrue("testpcall3", 2);
  }

  public void testunpackbig()
  {
    nTrue("testunpackbig", 1);
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new BaseLibTest("testBaseLib")
    {
        public void runTest() { testBaseLib(); } });
    suite.addTest(new BaseLibTest("testPrint")
        {
        public void runTest() { testPrint(); } });
    suite.addTest(new BaseLibTest("testTostring")
        {
        public void runTest() { testTostring(); } });
    suite.addTest(new BaseLibTest("testTonumber")
        {
        public void runTest() { testTonumber(); } });
    suite.addTest(new BaseLibTest("testType")
        {
        public void runTest() { testType(); } });
    suite.addTest(new BaseLibTest("testSelect")
        {
        public void runTest() { testSelect(); } });
    suite.addTest(new BaseLibTest("testUnpack")
        {
        public void runTest() { testUnpack(); } });
    suite.addTest(new BaseLibTest("testPairs")
        {
        public void runTest() { testPairs(); } });
    suite.addTest(new BaseLibTest("testIpairs")
        {
        public void runTest() { testIpairs(); } });
    suite.addTest(new BaseLibTest("testRawequal")
        {
        public void runTest() { testRawequal(); } });
    suite.addTest(new BaseLibTest("testRawget")
        {
        public void runTest() { testRawget(); } });
    suite.addTest(new BaseLibTest("testRawset")
        {
        public void runTest() { testRawset(); } });
    suite.addTest(new BaseLibTest("testGetfenv")
        {
        public void runTest() { testGetfenv(); } });
    suite.addTest(new BaseLibTest("testSetfenv")
        {
        public void runTest() { testSetfenv(); } });
    suite.addTest(new BaseLibTest("testNext")
        {
        public void runTest() { testNext(); } });
    suite.addTest(new BaseLibTest("testPcall")
        {
        public void runTest() { testPcall(); } });
    suite.addTest(new BaseLibTest("testError")
        {
        public void runTest() { testError(); } });
    suite.addTest(new BaseLibTest("testMetatable")
        {
        public void runTest() { testMetatable(); } });
    suite.addTest(new BaseLibTest("test__metatable")
        {
        public void runTest() { test__metatable(); } });
    suite.addTest(new BaseLibTest("test__tostring")
        {
        public void runTest() { test__tostring(); } });
    suite.addTest(new BaseLibTest("testCollectgarbage")
        {
        public void runTest() { testCollectgarbage(); } });
    suite.addTest(new BaseLibTest("testAssert")
        {
        public void runTest() { testAssert(); } });
    suite.addTest(new BaseLibTest("testLoadstring")
        {
        public void runTest() { testLoadstring(); } });
    suite.addTest(new BaseLibTest("testLoadfile")
        {
        public void runTest() { testLoadfile(); } });
    suite.addTest(new BaseLibTest("testLoad")
        {
        public void runTest() { testLoad(); } });
    suite.addTest(new BaseLibTest("testDofile")
        {
        public void runTest() { testDofile(); } });
    suite.addTest(new BaseLibTest("testVersion")
        {
        public void runTest() { testVersion(); } });
    suite.addTest(new BaseLibTest("testXpcall")
        {
        public void runTest() { testXpcall(); } });
    suite.addTest(new BaseLibTest("testErrormore")
        {
        public void runTest() { testErrormore(); } });
    suite.addTest(new BaseLibTest("testpcall2")
      {
        public void runTest() { testpcall2(); }
      });
    suite.addTest(new BaseLibTest("testpcall3")
      {
        public void runTest() { testpcall3(); }
      });
    suite.addTest(new BaseLibTest("testunpackbig")
      {
        public void runTest() { testunpackbig(); }
      });
    return suite;
  }
}
