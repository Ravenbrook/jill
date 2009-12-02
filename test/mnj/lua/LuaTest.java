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
// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestSuite;

// Uses some of the same ancillary files as LoaderTest.
// LuaTest0.luc - compiled version of (example code in the plan):
//   a = 7
//   b = {a, (a+1)/2, "foo"}
//   c = b[1] .. b[3]
//   return c

/**
 * J2MEUnit tests for Jill's public API.  DO NOT SUBCLASS.  public
 * access granted only because j2meunit makes it necessary.
 */
public class LuaTest extends JiliTestCase implements Hook
{
  private int n;        // used by luaHook
  private boolean error;        // used by luaHook

  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner LuaTest</code>
   */
  public LuaTest() { }

  /** Clones constructor from superclass.  */
  private LuaTest(String name)
  {
    super(name);
  }

  /** Constructor used when Hook is required. */
  private LuaTest(boolean error)
  {
    this.error = error;
  }

  /** From Hook interface. */
  public int luaHook(Lua L, Debug ar)
  {
    ++n;
    if (error)
    {
      return L.error("spong in hook");
    }
    return 0;
  }

  /** Tests that we can create a Lua state. */
  public void testLua0()
  {
    Lua L = new Lua();
  }

  /** Helper used by testLua1. */
  private void simpleScript(String filename)
  {
    Lua L = new Lua();
    loadFile(L, filename);
    int top = L.getTop();
    assertTrue("TOS == 1", 1 == top);
    L.call(0, 0);
    top = L.getTop();
    assertTrue("TOS == 0", 0 == top);
    loadFile(L, filename);
    L.call(0, 1);
    top = L.getTop();
    assertTrue("1 result", 1 == top);
    Object r = L.value(1);
    assertTrue("result is 99.0", ((Double)r).doubleValue() == 99.0);
  }

  /** Test loading and executition of simple file.  LoaderTest0.luc and
   * LoaderTest3.luc are compiled from the same Lua source, but '0' is
   * compiled on a big-endian architecture, and '3' is compiled on a
   * little-endian architecture.
   */
  public void testLua1()
  {
    simpleScript("LoaderTest0");
    simpleScript("LoaderTest3");
  }

  /** Tests that a Lua Java function can be called. */
  public void testLua2()
  {
    Lua L = new Lua();
    final Object[] v = new Object[1];
    final Object MAGIC = new Object();
    class Mine extends LuaJavaCallback
    {
      int luaFunction(Lua L)
      {
        v[0] = MAGIC;
        return 0;
      }
    }
    L.push(new Mine());
    L.call(0, 0);
    assertTrue("Callback got called", v[0] == MAGIC);
  }

  /** Tests that the Lua script in the plan can be executed. */
  public void testLua3()
  {
    Lua L = new Lua();
    loadFile(L, "LuaTest0");
    L.call(0, 1);
    System.out.println(L.value(1));
    assertTrue("Result is 7foo", "7foo".equals(L.value(1)));
  }

  /**
   * Test that we can set a table entry to nil.  Because the API is not
   * yet complete this uses {@link Lua#setGlobal} instead of the more obvious
   * Lua.setTable.  This is indicative for Ravenbrook job001451.
   */
  public void testLua4()
  {
    Lua L = new Lua();
    System.out.println("testLua4");
    boolean good = false;
    try
    {
      L.setGlobal("x", Lua.NIL);
      good = true;
    }
    catch (Exception e_)
    {
    }
    assertTrue("x = nil was okay", good);
  }

  /**
   * Test that a corner case of upvalues works.  This is indicative for
   * Ravenbrook job001457.
   */
  public void testLua5()
  {
    Lua L = new Lua();
    loadFile(L, "LuaTest5");
    L.call(0, 1);
    assertTrue("result was true",
      L.valueOfBoolean(true).equals(L.value(1)));
  }

  /**
   * Tests that an error in a hook does not prevent subsequent hooks
   * from running.
   */
  public void testlua6()
  {
    System.out.println(getName());
    Lua L = new Lua();
    BaseLib.open(L);
    MathLib.open(L);
    OSLib.open(L);
    StringLib.open(L);
    TableLib.open(L);

    L.setHook(new LuaTest(true), Lua.MASKCOUNT, 100);
    L.loadFile("speed/fannkuch.lua");
    int status = L.pcall(0, 0, null);
    assertTrue("status not 0", status != 0);
    assertTrue("status not Lua.YIELD", status != Lua.YIELD);
    assertTrue("error value is a string",
        L.value(-1) instanceof String);
    String s = (String)L.value(-1);
    assertTrue("error message contains spong", s.indexOf("spong") >= 0);

    LuaTest hook = new LuaTest(false);
    L.setHook(hook, Lua.MASKCOUNT, 100);
    L.loadFile("speed/fannkuch.lua");
    status = L.pcall(0, 0, null);
    assertTrue("status is 0", status == 0);
    assertTrue("hook ran many times", hook.n > 99);
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new LuaTest("testLua0")
      {
        public void runTest() { testLua0(); }
      });
    suite.addTest(new LuaTest("testLua1")
      {
        public void runTest() { testLua1(); }
      });
    suite.addTest(new LuaTest("testLua2")
      {
        public void runTest() { testLua2(); }
      });
    suite.addTest(new LuaTest("testLua3")
      {
        public void runTest() { testLua3(); }
      });
    suite.addTest(new LuaTest("testLua4")
      {
        public void runTest() { testLua4(); }
      });
    suite.addTest(new LuaTest("testLua5")
      {
        public void runTest() { testLua5(); }
      });
    suite.addTest(new LuaTest("testlua6")
      {
        public void runTest() { testlua6(); }
      });
    return suite;
  }
}
