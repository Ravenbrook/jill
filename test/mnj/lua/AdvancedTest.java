// $Header$

// The Intuwave advanced acceptance tests.

package mnj.lua;

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestSuite;

public class AdvancedTest extends JiliTestCase
{
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner AdvancedTest</code>
   */
  public AdvancedTest() { }

  /** Clones constructor from superclass.  */
  protected AdvancedTest(String name)
  {
    super(name);
  }

  public void runTest()
  {
    Lua L = new Lua();
    BaseLib.open(L);
    PackageLib.open(L);
    MathLib.open(L);
    OSLib.open(L);
    StringLib.open(L);
    TableLib.open(L);
    L.loadFile("accept-advanced/" + getName());
    int status = L.pcall(0, 0, new AddWhere());
    if (status != 0)
    {
      System.out.println(L.toString(L.value(-1)));
    }
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new AdvancedTest("attrib.lua"));
    suite.addTest(new AdvancedTest("big.lua"));
    suite.addTest(new AdvancedTest("calls.lua"));
    suite.addTest(new AdvancedTest("checktable.lua"));
    suite.addTest(new AdvancedTest("closure.lua"));
    suite.addTest(new AdvancedTest("constructs.lua"));
    suite.addTest(new AdvancedTest("db.lua"));
    suite.addTest(new AdvancedTest("events.lua"));
    suite.addTest(new AdvancedTest("gc.lua"));
    suite.addTest(new AdvancedTest("literals.lua"));
    suite.addTest(new AdvancedTest("locals.lua"));
    suite.addTest(new AdvancedTest("nextvar.lua"));
    suite.addTest(new AdvancedTest("pm.lua"));
    suite.addTest(new AdvancedTest("sort.lua"));
    suite.addTest(new AdvancedTest("strings.lua"));
    suite.addTest(new AdvancedTest("vararg.lua"));

    return suite;
  }
}
