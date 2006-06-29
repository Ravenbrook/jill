// $Header$

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

/**
 * Compendium of all Jili's object tests.
 */
public class ObjectAllTest extends TestCase
{
  public Test suite()
  {
    TestSuite suite = new TestSuite();
    TestCase t;

    t = new ObjectModelTest();
    suite.addTest(t.suite());
    t = new UpValTest();
    suite.addTest(t.suite());

    return suite;
  }
}
