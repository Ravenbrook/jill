// $Header$
// Copyright (c) 2006 Nokia Corporation and/or its subsidiary(-ies).
// All rights reserved.
// 
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject
// to the following conditions:
// 
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
// ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
// CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package mnj.lua;
import java.util.Vector;

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

/**
 * J2MEUnit tests for Jill's Object Model.  DO NOT SUBCLASS.  public
 * access granted only because j2meunit makes it necessary.
 */
public class ObjectModelTest extends TestCase
{
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner ObjectModelTest</code>
   */
  public ObjectModelTest() { }

  /** Clones constructor from superclass.  */
  private ObjectModelTest(String name)
  {
    super(name);
  }

  /**
   * Tests basic facts about LuaTable.
   */
  public void testTable()
  {
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
  public void testTableMeta()
  {
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
   * Tests basic facts about LuaFunction.
   */
  public void testFunction()
  {
    System.out.println("ObjectModelTest.testFunction()");

    Proto p = new Proto(new Slot[0],
        new int[0],
        new Proto[0],
        0,
        0,
        false,
        0);
    Vector v;
    v = new Vector();
    v.setSize(1);

    LuaFunction f = new LuaFunction(p, new UpVal[0], new LuaTable());
    assertNotNull(f);

    // Check that the type is correct, according to the API.
    assertTrue(Lua.isFunction(f));
    assertTrue(!Lua.isJavaFunction(f));
    assertTrue(!Lua.isNil(f));
    assertTrue(!Lua.isBoolean(f));
    assertTrue(!Lua.isNumber(f));
    assertTrue(!Lua.isString(f));
    assertTrue(!Lua.isTable(f));
    assertTrue(!Lua.isUserdata(f));

    assertSame("{Proto passed, Proto returned}", p, f.proto());

    LuaTable e = new LuaTable();
    f.setEnv(e);
    assertSame("{env set, env returned}", e, f.getEnv());
  }

  /**
   * Tests basic facts about Userdata.
   */
  public void testUserdata()
  {
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
  public void testUserdataStore()
  {
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

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new ObjectModelTest("testTable")
    {
        public void runTest() { testTable(); } });
    suite.addTest(new ObjectModelTest("testTableMeta")
        {
        public void runTest() { testTableMeta(); } });
    suite.addTest(new ObjectModelTest("testFunction")
        {
        public void runTest() { testFunction(); } });
    suite.addTest(new ObjectModelTest("testUserdata")
        {
        public void runTest() { testUserdata(); } });
    suite.addTest(new ObjectModelTest("testUserdataStore")
        {
        public void runTest() { testUserdataStore(); } });
    return suite;
  }
}
