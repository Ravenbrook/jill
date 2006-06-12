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
    LuaFunction f= null;
    try {
      f = L.load(Lua.stringReader(""), "Syntax1");
    } catch (Exception e) {
      e.printStackTrace();
    }
    L.push(f);
    L.call(0, 0);
  }

  public Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new SyntaxTest("testSyntax0") {
        public void runTest() { testSyntax0(); } });
    suite.addTest(new SyntaxTest("testSyntax1") {
        public void runTest() { testSyntax1(); } });
    return suite;
  }
}
