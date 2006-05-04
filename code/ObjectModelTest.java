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
    // Check that the type is correct, according to the API.
    assertTrue(Lua.isTable(table));
    assertTrue(!Lua.isNil(table));
    assertTrue(!Lua.isBoolean(table));
    assertTrue(!Lua.isNumber(table));
    assertTrue(!Lua.isString(table));
    assertTrue(!Lua.isFunction(table));
    assertTrue(!Lua.isUserdata(table));

    LuaTable another = new LuaTable();
    assertTrue(another != table);
  }

  /**
   * Tests Metatable of LuaTable.
   */
  public void testTableMeta() {
    System.out.println("ObjectModelTest.testTableMeta()");

    LuaTable table = new LuaTable();
    LuaTable meta = new LuaTable();
    LuaTable another = new LuaTable();
    LuaTable anotherMeta = new LuaTable();

    table.setMetatable(meta);
    assertSame("{Metatable set, returned}", meta, table.getMetatable());

    another.setMetatable(anotherMeta);
    assertTrue("Tables metatables are not same",
        table.getMetatable() != another.getMetatable());
  }

  /**
   * Tests basic facts about Userdata.
   */
  public void testUserdata() {
    System.out.println("ObjectModelTest.testUserdata()");

    Object o = new Object();
    LuaUserdata u = new LuaUserdata(o);
    assertNotNull(u);
    // Check that the type is correct, according to the API.
    assertTrue(Lua.isUserdata(u));
    assertTrue(!Lua.isNil(u));
    assertTrue(!Lua.isBoolean(u));
    assertTrue(!Lua.isNumber(u));
    assertTrue(!Lua.isString(u));
    assertTrue(!Lua.isTable(u));
    assertTrue(!Lua.isFunction(u));

    LuaUserdata another = new LuaUserdata(o);
    assertNotNull(another);
  }

  /**
   * Tests storage facilities of Userdata.
   */
  public void testUserdataStore() {
    System.out.println("ObjectModelTest.testUserdataStore()");

    Object o = new Object();
    LuaUserdata u = new LuaUserdata(o);
    LuaUserdata another = new LuaUserdata(o);

    assertSame("{Object stored, returned}",
        u.getUserdata(), o);
    assertSame("{u.getUserData(), another.getUserdata()}",
        u.getUserdata(), another.getUserdata());

    LuaTable t = new LuaTable();
    u.setMetatable(t);
    assertSame("{Metatable set, returned}", u.getMetatable(), t);

    LuaTable e = new LuaTable();
    u.setEnv(e);
    assertSame("{Environment set, returned}", u.getEnv(), e);
  }

  public Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new ObjectModelTest("testTable") {
        public void runTest() { testTable(); } });
    suite.addTest(new ObjectModelTest("testTableMeta") {
        public void runTest() { testTableMeta(); } });
    suite.addTest(new ObjectModelTest("testUserdata") {
        public void runTest() { testUserdata(); } });
    suite.addTest(new ObjectModelTest("testUserdataStore") {
        public void runTest() { testUserdataStore(); } });
    return suite;
  }
}
