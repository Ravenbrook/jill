// $Header$

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

// Auxiliary files
// BaseLibTest.luc - contains functions that test each of base library
// functions:
//   function testprint()
//     print()
//     print(7, 'foo', {}, nil, function()end, true, false, -0.0)
//   end
//   function testtostring()
//     return '7' == tostring(7),
//         'foo' == tostring'foo',
//         'nil' == tostring(nil),
//         'true' == tostring(true),
//         'false' == tostring(false)
//   end
//   function testtonumber()
//     return 1 == tonumber'1',
//         nil == tonumber'',
//         nil == tonumber{},
//         nil == tonumber(false),
//         -2.5 == tonumber'-2.5'
//   end
//   function testtype()
//     return type(nil) == 'nil',
//         type(1) == 'number',
//         type'nil' == 'string',
//         type{} == 'table',
//         type(function()end) == 'function',
//         type(type==type) == 'boolean'
//   end
//   function testselect()
//     return select(2, 6, 7, 8) == 7,
//         select('#', 6, 7, 8) == 3
//   end
//   function testunpack()
//     a,b,c = unpack{'foo', 'bar', 'baz'}
//     return a == 'foo', b == 'bar', c == 'baz'
//   end

/**
 * J2MEUnit tests for Jili's BaseLib (base library).  DO NOT SUBCLASS.
 * public access granted only because j2meunit makes it necessary.
 */
public class BaseLibTest extends TestCase {
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner BaseLibTest</code>
   */
  public BaseLibTest() { }

  /** Clones constructor from superclass.  */
  private BaseLibTest(String name) {
    super(name);
  }

  // :todo: consider pushing loadFile into common superclass for all
  // test classes.
  /**
   * @param L         Lua state in which to load file.
   * @param filename  filename without '.luc' extension.
   */
  private LuaFunction loadFile(Lua L, String filename) {
    filename += ".luc";
    System.out.println(filename);
    LuaFunction f = null;
    try {
      f = L.load(this.getClass().getResourceAsStream(filename), filename);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return f;
  }

  /**
   * Tests BaseLib.
   */
  public void testBaseLib() {
    System.out.println("BaseLibTest.testBaseLib()");
    Lua L = new Lua();

    BaseLib.open(L);

    // Test that each global name is defined as expected.
    String[] name = {
      "_G", "print", "select", "tonumber", "tostring", "type", "unpack"
    };
    for (int i=0; i<name.length; ++i) {
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
  private Lua luaGlobal(String name, int n) {
    Lua L = new Lua();
    BaseLib.open(L);
    LuaFunction f = loadFile(L, "BaseLibTest");
    L.push(f);
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
  private void nTrue(String name, int n) {
    Lua L = luaGlobal(name, n);
    for (int i=1; i<=n; ++i) {
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
  public void testPrint() {
    luaGlobal("testprint", 0);
  }

  public void testTostring() {
    int n = 5;
    Lua L = luaGlobal("testtostring", n);
    for (int i=1; i<=n; ++i) {
      assertTrue("Result " + i + " is true",
	  L.valueOfBoolean(true).equals(L.value(i)));
    }
  }

  public void testTonumber() {
    int n = 5;
    Lua L = luaGlobal("testtonumber", n);
    for (int i=1; i<=n; ++i) {
      assertTrue("Result " + i + " is true",
	  L.valueOfBoolean(true).equals(L.value(i)));
    }
  }

  public void testType() {
    int n = 6;
    Lua L = luaGlobal("testtype", n);
    for (int i=1; i<=n; ++i) {
      assertTrue("Result " + i + " is true",
	  L.valueOfBoolean(true).equals(L.value(i)));
    }
  }

  public void testSelect() {
    nTrue("testselect", 2);
  }

  public void testUnpack() {
    nTrue("testunpack", 1);
  }

  public Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new BaseLibTest("testBaseLib") {
        public void runTest() { testBaseLib(); } });
    suite.addTest(new BaseLibTest("testPrint") {
        public void runTest() { testPrint(); } });
    suite.addTest(new BaseLibTest("testTostring") {
        public void runTest() { testTostring(); } });
    suite.addTest(new BaseLibTest("testTonumber") {
        public void runTest() { testTonumber(); } });
    suite.addTest(new BaseLibTest("testType") {
        public void runTest() { testType(); } });
    suite.addTest(new BaseLibTest("testSelect") {
        public void runTest() { testSelect(); } });
    suite.addTest(new BaseLibTest("testUnpack") {
        public void runTest() { testUnpack(); } });
    return suite;
  }
}
