// $Header$

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

/**
 * J2MEUnit tests for Jili's Syntax module.  DO NOT SUBCLASS.  public
 * access granted only because j2meunit makes it necessary.
 */
public class SyntaxTest extends TestCase {
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner SyntaxTest</code>
   */
  public SyntaxTest() { }

  /** Clones constructor from superclass.  */
  private SyntaxTest(String name) {
    super(name);
  }

  private int dostring(Lua L, String s) {
    System.out.println("[[" + s + "]]");
    return L.doString(s);
  }

  public void testSyntax0() {
    System.out.println("Syntax0");
    Lua L = new Lua();
    Object o = null;
    try {
      o = Syntax.parser(L, Lua.stringReader(""), "Syntax0");
    } catch (Exception e_) {
    }
    assertNotNull("Parse result", o);
  }

  public void testSyntax1() {
    System.out.println("Syntax1");
    Lua L = new Lua();
    L.load(Lua.stringReader(""), "Syntax1");
    L.call(0, 0);
  }

  public void testSyntax2() {
    System.out.println("Syntax2");
    Lua L = new Lua();
    assertTrue("script 1 okay", 0 == dostring(L, ""));
    assertTrue("script 2 okay", 0 == dostring(L, " \t"));
    assertTrue("script 3 okay", 0 == dostring(L, "\n\n"));
    assertTrue("script 4 okay", 0 == dostring(L, "return 99"));
    assertTrue("script 5 okay", 0 == dostring(L, "return -99"));
  }

  /** Test that function calls are compiled. */
  public void testSyntax3() {
    System.out.println("Syntax3");
    Lua L = new Lua();
    BaseLib.open(L);
    assertTrue("script 1 okay", 0 == dostring(L, "print'hello'"));
  }

  public Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new SyntaxTest("testSyntax0") {
        public void runTest() { testSyntax0(); } });
    suite.addTest(new SyntaxTest("testSyntax1") {
        public void runTest() { testSyntax1(); } });
    suite.addTest(new SyntaxTest("testSyntax2") {
        public void runTest() { testSyntax2(); } });
    suite.addTest(new SyntaxTest("testSyntax3") {
        public void runTest() { testSyntax3(); } });
    return suite;
  }
}
