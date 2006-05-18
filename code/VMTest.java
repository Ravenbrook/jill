// $Header$

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

import java.io.FileInputStream;
import java.io.InputStream;

// The VMTest uses ancillary files:
// VMTestLoadbool.luc - a binary chunk containing OP_LOADBOOL.
//   luac -o VMTestLoadbool.luc - << 'EOF'
//   return x==nil
//   EOF
// VMTestLoadnil.luc - a binary chunk containing OP_LOADNIL.
//   luac -o VMTestLoadnil.luc - << 'EOF'
//   local a,b,c; a,b,c="foo","bar","baz"; a,b,c=7; return a,b,c
//   EOF


/**
 * J2MEUnit tests for Jili's VM execution.  DO NOT SUBCLASS.  public
 * access granted only because j2meunit makes it necessary.
 * This test does not run in CLDC 1.1.
 */
public class VMTest extends TestCase {
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner VMTest</code>
   */
  public VMTest() { }

  /** Clones constructor from superclass.  */
  private VMTest(String name) {
    super(name);
  }

  /**
   * @param L         Lua state in which to load file.
   * @param filename  filename without '.luc' extension.
   */
  private LuaFunction loadFile(Lua L, String filename) {
    filename += ".luc";
    LuaFunction f = null;
    try {
      f = L.load(new FileInputStream(filename), filename);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return f;
  }

  /**
   * Tests execution of the OP_LOADBOOL opcode.  This opcode is
   * generated when the results of a boolean expression are used for its
   * value (as opposed to inside an "if").  Our test is "return x==nil".
   * This generates both the skip and non-skip forms.
   */
  public void testVMLoadbool() {
    System.out.println("LOADBOOL");
    Lua L = new Lua();
    LuaFunction f;
    f = loadFile(L, "VMTestLoadbool");
    L.push(f);
    L.call(0, 1);
    Object res = L.value(1);
    Boolean b = (Boolean)res;
    assertTrue("Result is true", b.booleanValue());
    L.rawset(L.getGlobals(), "x", "foo");
    L.push(f);
    L.call(0, 1);
    res = L.value(-1);
    b = (Boolean)res;
    assertTrue("Result is false", b.booleanValue() == false);
  }

  /**
   * Tests execution of OP_LOADNIL opcode.  This opcode is generated for
   * assignment sequences like "a,b,c=7".
   */
  public void testVMLoadnil() {
    System.out.println("LOADNIL");
    Lua L = new Lua();
    LuaFunction f;
    f = loadFile(L, "VMTestLoadnil");
    L.push(f);
    L.call(0, 3);
    Object first = L.value(1);
    Double d = (Double)first;
    assertTrue("First result is 7", d.doubleValue() == 7);
    assertTrue("Second result is nil", L.value(2) == L.NIL);
    assertTrue("Third result is nil", L.value(3) == L.NIL);
  }

  public Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new VMTest("testVMLoadbool") {
        public void runTest() { testVMLoadbool(); } });
    suite.addTest(new VMTest("testVMLoadnil") {
        public void runTest() { testVMLoadnil(); } });
    return suite;
  }
}
