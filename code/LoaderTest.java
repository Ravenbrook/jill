// $Header$

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

import java.io.FileInputStream;
import java.io.InputStream;

// The LoaderTest uses ancillary files:
// LoaderTest0.luc - compiled lua chunk, result of:
//   luac -s - <<'EOF'
//   return 99
//   EOF

/**
 * J2MEUnit tests for Jili's internal Loader class.  DO NOT SUBCLASS.  public
 * access granted only because j2meunit makes it necessary.
 * This test does not run in CLDC 1.1.
 */
public class LoaderTest extends TestCase {
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner LoaderTest</code>
   */
  public LoaderTest() { }

  /** Clones constructor from superclass.  */
  private LoaderTest(String name) {
    super(name);
  }

  /**
   * Tests the loader.
   */
  public void testLoader() {
    String filename = "LoaderTest0";
    InputStream in = null;
    try {
      in = new FileInputStream(filename + ".luc");
    } catch(Exception e) {
      e.printStackTrace();
    }
    assertNotNull("Opened file", in);

    boolean loaded = false;
    Loader loader = new Loader(in, filename);

    try {
      loader.undump();
      loaded = true;
    } catch(Exception e) {
      e.printStackTrace();
    }
    assertTrue("Loaded okay", loaded);
  }

  public Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new LoaderTest("testLoader") {
        public void runTest() { testLoader(); } });
    return suite;
  }
}
