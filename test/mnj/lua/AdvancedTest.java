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
    loadFile(L, "accept-advanced/" + getName());
    int status = L.pcall(0, 0, new AddWhere());
    if (status != 0)
    {
      System.out.println(L.toString(L.value(-1)));
    }
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new AdvancedTest("attrib"));
    suite.addTest(new AdvancedTest("big"));
    suite.addTest(new AdvancedTest("calls"));
    suite.addTest(new AdvancedTest("checktable"));
    suite.addTest(new AdvancedTest("closure"));
    suite.addTest(new AdvancedTest("constructs"));
    suite.addTest(new AdvancedTest("db"));
    suite.addTest(new AdvancedTest("events"));
    suite.addTest(new AdvancedTest("gc"));
    suite.addTest(new AdvancedTest("literals"));
    suite.addTest(new AdvancedTest("locals"));
    suite.addTest(new AdvancedTest("nextvar"));
    suite.addTest(new AdvancedTest("pm"));
    suite.addTest(new AdvancedTest("sort"));
    suite.addTest(new AdvancedTest("strings"));
    suite.addTest(new AdvancedTest("vararg"));

    return suite;
  }
}
