// $Header$

package mnj.lua;

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestSuite;

// Auxiliary files
// MathLibTest.lua

/**
 * J2MEUnit tests for Jili's math library.  DO NOT SUBCLASS.
 * public access granted only because j2meunit makes it necessary.
 */
public class MathLibTest extends JiliTestCase
  {
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner MathLibTest</code>
   */
  public MathLibTest() { }

  /** Clones constructor from superclass.  */
  private MathLibTest(String name)
  {
    super(name);
  }

  public void runTest()
  {
    // loads MathLibTest.lua and calls a function defined therein
    Lua L = new Lua();
    BaseLib.open(L);
    MathLib.open(L);

    loadFileAndRun(L, "MathLibTest.lua", getName(), 0);
    assertTrue(true);
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new MathLibTest("test1"));
    suite.addTest(new MathLibTest("test2"));
    suite.addTest(new MathLibTest("test3"));

    return suite;
  }
}
