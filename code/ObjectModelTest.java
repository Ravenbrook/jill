// $Header$

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

/**
 * J2MEUnit tests for Jili's Object Model.  DO NOT SUBLCLASS.  public
 * access granted only because j2meunit makes it necesary.
 */
public class ObjectModelTest extends TestCase {
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner ObjectModelTest</code>
   */
  public ObjectModelTest() { }

  /** Clones constructor from superclass.  */
  private ObjectModelTest(String name) {
    super(name);
  }

  /**
   * Tests basic facts about LuaTable.
   */
  public void testTable() {
    System.out.println("ObjectModelTest.testTable()");
    LuaTable table = new LuaTable();
    assertNotNull(table);
    assertTrue("table isTable", Lua.isTable(table));
    assertTrue("table not isNil", !Lua.isNil(table));
  }

  public Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new ObjectModelTest("testTable") {
        public void runTest() { testTable(); } });
    return suite;
  }
}
