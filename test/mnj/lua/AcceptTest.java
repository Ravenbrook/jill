// $Header$

// The Intuwave acceptance tests.

package mnj.lua;

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestSuite;

public class AcceptTest extends JiliTestCase
{
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner AcceptTest</code>
   */
  public AcceptTest() { }

  /** Clones constructor from superclass.  */
  private AcceptTest(String name)
  {
    super(name);
  }

  public void runTest()
  {
    // loads and runs a test script in the accept-basic directory
    Lua L = new Lua();
    BaseLib.open(L);
    PackageLib.open(L);
    MathLib.open(L);
    OSLib.open(L);
    StringLib.open(L);
    TableLib.open(L);

    System.out.println(getName());
    int status = L.loadFile("accept-basic/" + getName() + ".lua");
    assertTrue(status == 0);
    L.call(0, 0);
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new AcceptTest("bisect"));
    suite.addTest(new AcceptTest("cf"));
    suite.addTest(new AcceptTest("factorial"));
    suite.addTest(new AcceptTest("fib"));
    suite.addTest(new AcceptTest("fibfor"));
    suite.addTest(new AcceptTest("life"));
    suite.addTest(new AcceptTest("readonly"));
    suite.addTest(new AcceptTest("sieve"));
    suite.addTest(new AcceptTest("sort"));

    return suite;
  }
}
