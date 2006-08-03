// $Header$

// The life.lua test from the Intuwave acceptance tests.

package mnj.lua;

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestSuite;

public class LifeTest extends AcceptTest
{
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner LifeTest</code>
   */
  public LifeTest() { }

  /** Clones constructor from superclass.  */
  private LifeTest(String name)
  {
    super(name);
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new LifeTest("life"));

    return suite;
  }
}
