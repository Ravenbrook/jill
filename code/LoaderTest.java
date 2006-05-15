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
// LoaderTest1.luc - as LoaderTest0.luc but with debug info:
//    luac - <<'EOF'
//    return 99
//  EOF
// LoaderTest2.luc - test/fib.lua from PUC-Rio Lua 5.1: luac fib.lua

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
   * @param filename  filename without '.luc' extension.
   */
  private Proto loadFile(String filename) {
    filename += ".luc";
    InputStream in = null;

    System.out.println(filename);

    try {
      in = new FileInputStream(filename);
    } catch(Exception e) {
      e.printStackTrace();
    }
    assertNotNull("Opened file " + filename, in);

    boolean loaded = false;
    Loader loader = new Loader(in, filename);

    Proto proto = null;
    try {
      proto = loader.undump();
      loaded = true;
    } catch(Exception e) {
      e.printStackTrace();
    }
    assertTrue("Loaded okay", loaded);

    return proto;
  }

  /** Tests LoaderTest0.luc.  */
  public void testLoader0() {
    Proto p = loadFile("LoaderTest0");
    assertNotNull(p);
  }

  /** Tests LoaderTest1.luc.  */
  public void testLoader1() {
    loadFile("LoaderTest1");
  }

  /** Tests LoaderTest2.luc.  */
  public void testLoader2() {
    loadFile("LoaderTest2");
  }

  public Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new LoaderTest("testLoader0") {
        public void runTest() { testLoader0(); } });
    suite.addTest(new LoaderTest("testLoader1") {
        public void runTest() { testLoader1(); } });
    suite.addTest(new LoaderTest("testLoader2") {
        public void runTest() { testLoader2(); } });
    return suite;
  }
}
