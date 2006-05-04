// $Header$

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

/**
 * J2MEUnit tests for Jili's interal UpVal class.  DO NOT SUBLCLASS.  public
 * access granted only because j2meunit makes it necesary.
 */
public class UpValTest extends TestCase {
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner UpValTest</code>
   */
  public UpValTest() { }

  /** Clones constructor from superclass.  */
  private UpValTest(String name) {
    super(name);
  }

  /**
   * Tests UpVal.
   */
  public void testUpVal() {
    System.out.println("UpValTest.testUpVal()");

    Object[] a = new Object[3];
    UpVal u = new UpVal(a, 1);

    assertNotNull(u);

    // Test that setting the underlying array affects UpVal.getValue()
    Object o = new Object();
    a[1] = o;
    assertSame("{Set via array, got via UpVal}", o, u.getValue());

    // Test that setting the UpVal affects the underlying array
    o = new Object();
    u.setValue(o);
    assertSame("{Set via UpVal, got via array}", o, a[1]);
    assertNull("a[0] not changed", a[0]);
    assertNull("a[2] not changed", a[2]);

    // Test that closing an UpVal does change its value.
    u.close();
    assertSame("{Value before closing, value after closing}", o,
        u.getValue());

    // Now the UpVal is closed the original array a, and the UpVal value are
    // independent, so test we can set one without affecting the other.

    // Set array and inspect UpVal.
    a[1] = new Object();
    assertSame("{UpVal.getValue before array changed, after array changed}",
        o, u.getValue());
    assertTrue("UpVal and array are now different", u.getValue() != a[1]);

    // Set UpVal and inspect array.
    u.setValue(new Object());
    assertTrue("a[0] is not UpVal", a[0] != u.getValue());
    assertTrue("a[1] is not UpVal", a[1] != u.getValue());
    assertTrue("a[2] is not UpVal", a[2] != u.getValue());

    // Check unused entries in array are still null
    assertNull("a[0] still not changed", a[0]);
    assertNull("a[2] still not changed", a[2]);
  }

  public Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new UpValTest("testUpVal") {
        public void runTest() { testUpVal(); } });
    return suite;
  }
}
