// $Header$

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

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

  /**
   * Tests BaseLib.
   */
  public void testBaseLib() {
    System.out.println("BaseLibTest.testBaseLib()");
    Lua L = new Lua();

    BaseLib.open(L);
  }

  public Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new BaseLibTest("testBaseLib") {
        public void runTest() { testBaseLib(); } });
    return suite;
  }
}
