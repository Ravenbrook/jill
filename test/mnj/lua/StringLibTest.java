// $Header$

package mnj.lua;
// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestSuite;

// Auxiliary files
// StringLibTest.lua - test functions.
// StringLibTest.luc - compiled .lua file.

/**
 * J2MEUnit tests for Jili's StringLib (string library).  DO NOT SUBCLASS.
 * public access granted only because j2meunit makes it necessary.
 */
public class StringLibTest extends JiliTestCase
  {
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner StringLibTest</code>
   */
  public StringLibTest() { }

  /** Clones constructor from superclass.  */
  private StringLibTest(String name)
  {
    super(name);
  }

  /**
   * Tests StringLib.
   */
  public void testStringLib()
  {
    System.out.println("StringLibTest.testStringLib()");
    Lua L = new Lua();

    StringLib.open(L);

    Object lib = L.getGlobal("string");
    assertTrue("string table defined", L.isTable(lib));

    // Test that each string library name is defined as expected.
    String[] name =
    {
    };
    for (int i=0; i<name.length; ++i)
    {
      Object o = L.getField(lib, name[i]);
      assertTrue(name[i] + " exists", !L.isNil(o));
    }
  }

  /**
   * Opens the base and string libraries into a fresh Lua state,
   * calls a global function, and returns the Lua state.
   * @param name  name of function to call.
   * @param n     number of results expected from function.
   */
  private Lua luaGlobal(String name, int n)
  {
    Lua L = new Lua();
    BaseLib.open(L);
    StringLib.open(L);
    loadFile(L, "StringLibTest");
    L.call(0, 0);
    System.out.println(name);
    L.push(L.getGlobal(name));
    int status = L.pcall(0, n, new AddWhere());
    if (status != 0)
    {
      System.out.println(L.toString(L.value(-1)));
    }
    assertTrue(name, status == 0);
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

  public void testlen()
  {
    nTrue("testlen", 2);
  }

  public void testlower()
  {
    nTrue("testlower", 3);
  }

  public void testrep()
  {
    nTrue("testrep", 3);
  }

  public void testupper()
  {
    nTrue("testupper", 3);
  }

  public void testsub()
  {
    nTrue("testsub", 3);
  }

  public void testmeta()
  {
    nTrue("testmeta", 2);
  }

  public void testreverse()
  {
    nTrue("testreverse", 2);
  }

  public void testbyte()
  {
    nTrue("testbyte", 5);
  }

  public void testchar()
  {
    nTrue("testchar", 2);
  }

  public void testfind()
  {
    nTrue("testfind", 6);
  }

  public void testmatch()
  {
    nTrue("testmatch", 2);
  }

  public void testformat()
  {
    nTrue("testformat", 1);
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new StringLibTest("testStringLib")
    {
        public void runTest() { testStringLib(); } });
    suite.addTest(new StringLibTest("testlen")
        {
        public void runTest() { testlen(); } });
    suite.addTest(new StringLibTest("testlower")
        {
        public void runTest() { testlower(); } });
    suite.addTest(new StringLibTest("testrep")
        {
        public void runTest() { testrep(); } });
    suite.addTest(new StringLibTest("testupper")
        {
        public void runTest() { testupper(); } });
    suite.addTest(new StringLibTest("testsub")
        {
        public void runTest() { testsub(); } });
    suite.addTest(new StringLibTest("testmeta")
        {
        public void runTest() { testmeta(); } });
    suite.addTest(new StringLibTest("testreverse")
        {
        public void runTest() { testreverse(); } });
    suite.addTest(new StringLibTest("testbyte")
        {
        public void runTest() { testbyte(); } });
    suite.addTest(new StringLibTest("testchar")
      {
        public void runTest() { testchar(); }
      });
    suite.addTest(new StringLibTest("testfind")
      {
        public void runTest() { testfind(); }
      });
    suite.addTest(new StringLibTest("testmatch")
      {
        public void runTest() { testmatch(); }
      });
    suite.addTest(new StringLibTest("testformat")
      {
        public void runTest() { testformat(); }
      });
    return suite;
  }
}

final class AddWhere extends LuaJavaCallback
{
  int luaFunction(Lua L)
  {
    L.insert(L.where(2), -1);
    L.concat(2);
    return 1;
  }
}
