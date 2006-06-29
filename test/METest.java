// $Header$

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

/**
 * Compendium of all Jili's tests that run in the J2ME environment.
 */
public class METest extends TestCase
{
  public Test suite()
  {
    TestSuite suite = new TestSuite();
    TestCase t;

    t = new ObjectAllTest();
    suite.addTest(t.suite());
    t = new LoaderTest();
    suite.addTest(t.suite());
    t = new LuaTest();
    suite.addTest(t.suite());
    t = new VMTest();
    suite.addTest(t.suite());
    t = new BaseLibTest();
    suite.addTest(t.suite());
    t = new StringLibTest();
    suite.addTest(t.suite());
    t = new SyntaxTest();
    suite.addTest(t.suite());

    return suite;
  }
}
