// $Header$

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

// Auxiliary files
// BaseLibTestPrint.luc - calls print
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
   * Tests print.  Not much we can reasonably do here apart from call
   * it.  We can't automatically check that the output appears anywhere
   * or is correct.  This also tests tostring to some extent; print
   * calls tostring internally, so this tests that it can be called
   * without error, for example.
   */
  public void testPrint() {
    Lua L = new Lua();
    BaseLib.open(L);

    LuaFunction f = loadFile(L, "BaseLibTestPrint");
    L.push(f);
    L.call(0, 0);
    L.push(L.getGlobal("testprint"));
    L.call(0, 0);
  }

  public void testTostring() {
    Lua L = new Lua();
    BaseLib.open(L);
    LuaFunction f = loadFile(L, "BaseLibTestPrint");
    L.push(f);
    L.call(0, 0);
    L.push(L.getGlobal("testtostring"));
    L.call(0,5);
    for (int i=1; i<=5; ++i) {
      assertTrue("Result " + i + " is true",
	  L.valueOfBoolean(true).equals(L.value(i)));
    }
  }

  public Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new BaseLibTest("testBaseLib") {
        public void runTest() { testBaseLib(); } });
    suite.addTest(new BaseLibTest("testPrint") {
        public void runTest() { testPrint(); } });
    suite.addTest(new BaseLibTest("testTostring") {
        public void runTest() { testTostring(); } });
    return suite;
  }
}
