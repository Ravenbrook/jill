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

// Auxiliary files
// CoroTest.lua

/**
 * J2MEUnit tests for Jill's coroutine functionality.  DO NOT SUBCLASS.
 * public access granted only because j2meunit makes it necessary.
 */
public class CoroTest extends JiliTestCase
  {
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner CoroTest</code>
   */
  public CoroTest() { }

  /** Clones constructor from superclass.  */
  private CoroTest(String name)
  {
    super(name);
  }

  /** Ordinary completion of a thread. */
  public void test1()
  {
    Lua L = new Lua();
    BaseLib.open(L);
    L.push(L.getGlobal("tostring"));
    L.push("hello");
    int status = L.resume(1);
    assertTrue(status == 0);
    assertTrue(L.getTop() == 1);
  }

  /** Thread that yields. */
  public void test2()
  {
    final boolean[] yielded = { false };
    Lua L = new Lua();
    BaseLib.open(L);
    L.push(new LuaJavaCallback()
      {
        public int luaFunction(Lua L)
        {
          System.out.println("Yielding");
          yielded[0] = true;
          return L.yield(0);
        }
      });
    int status = L.resume(0);
    assertTrue("Status is YIELD", status == Lua.YIELD);
    assertTrue("Thread yielded", yielded[0]);
    assertTrue("Stack top", L.getTop() == 0);
  }

  /** Thread that yields using coroutine.yield. */
  public void test3()
  {
    Lua L = new Lua();
    BaseLib.open(L);
    L.push(L.getField(L.getGlobal("coroutine"), "yield"));
    int status = L.resume(0);
    assertTrue("Status is YIELD", status == Lua.YIELD);
    assertTrue("Stack top", L.getTop() == 0);
  }

  /** Yielding in a Lua script. */
  public void test4()
  {
    Lua L = new Lua();
    BaseLib.open(L);
    L.loadFile("CoroTest.lua");
    L.call(0, 0);
    L.push(L.getGlobal("test4"));
    final int n = 4;
    for (int i=0; i<n; ++i)
    {
      int status = L.resume(0);
      if (i < n-1)
      {
        assertTrue("Status is YIELD", status == Lua.YIELD);
      }
      else
      {
        assertTrue("Status is 0", status == 0);
      }
      double v = L.toNumber(L.getGlobal("v"));
      assertTrue("v is " + i, v == i);
    }
  }

  /** Yielding by throw exception. */
  public void test10()
  {
    System.out.println("CoroTest." + getName());
    Lua L = new Lua();
    L.loadFile("CoroTest.lua");
    L.call(0, 0);
    L.push(L.getGlobal(getName()));
    L.push(new LuaJavaCallback()
      {
        public int luaFunction(Lua L)
        {
          return L.yield(0);
        }
      });
    L.push(new LuaJavaCallback()
      {
        public int luaFunction(Lua L)
        {
          throw new RuntimeException("spong") { };
        }
      });
    boolean start = true;
    int status = 0;
    int n = 0;
    int k = 0;
    while (true)
    {
      int nargs = 0;
      if (start)
      {
        nargs = 2;
        start = false;
      }
      else
      {
        nargs = 1;
        L.pushNumber(n);
      }
      ++n;
      try
      {
        status = L.resume(nargs);
        if (status != Lua.YIELD)
        {
          break;
        }
      }
      catch (RuntimeException e)
      {
        ++k;
        assertTrue("e.getMessage()",
            e.getMessage().indexOf("spong") >= 0);
      }
    }
    if (status != 0)
    {
      System.out.println(L.value(-1));
    }
    assertTrue("status is 0", status == 0);
    assertTrue("k is 4", k == 4);
    assertTrue("first return value is 16", L.toNumber(L.value(-2)) == 16);
    assertTrue("second return value is 20", L.toNumber(L.value(-1)) == 20);
  }

  public void runTest()
  {
    // loads CoroTest.lua and calls a function defined therein
    Lua L = new Lua();
    BaseLib.open(L);
    StringLib.open(L);  // string.find is required.
    TableLib.open(L);   // table.insert is required.
    System.out.println("CoroTest." + getName());
    L.loadFile("CoroTest.lua");
    L.call(0, 0);
    L.push(L.getGlobal(getName()));
    int status = L.pcall(0, 1, new AddWhere());
    if (status != 0)
    {
      System.out.println(L.value(-1));
    }
    assertTrue(status == 0);
    assertTrue("Result is true",
          L.valueOfBoolean(true).equals(L.value(-1)));
  }

  /**
   * Thread that raises error.
   */
  public void test11()
  {
    System.out.println("test11");
    Lua main = new Lua();
    Lua co = main.newThread();
    BaseLib.open(co);
    co.loadString("error('test11 error ' .. ...)", "@test11");
    co.pushString("foo");
    int status = co.resume(1);
    System.out.println("status = " + status);
    assertTrue("status is ERRRUN", status == Lua.ERRRUN);
    if (status != 0 && status != Lua.YIELD)
    {
      Object o = co.value(-1);
      assertTrue(o instanceof String);
      String s = (String)o;
      assertTrue("Result contains 'test11 error'",
          s.indexOf("test11 error") >= 0);
      System.out.println("Expected error: " + co.value(-1));
    }
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new CoroTest("test1")
      {
        public void runTest() { test1(); }
      });
    suite.addTest(new CoroTest("test2")
      {
        public void runTest() { test2(); }
      });
    suite.addTest(new CoroTest("test3")
      {
        public void runTest() { test3(); }
      });
    suite.addTest(new CoroTest("test4")
      {
        public void runTest() { test4(); }
      });
    suite.addTest(new CoroTest("test5"));
    suite.addTest(new CoroTest("test6"));
    suite.addTest(new CoroTest("test7"));
    suite.addTest(new CoroTest("test8"));
    suite.addTest(new CoroTest("test9"));
    suite.addTest(new CoroTest("test10")
      {
        public void runTest() { test10(); }
      });
    suite.addTest(new CoroTest("test11")
      {
        public void runTest() { test11(); }
      });

    return suite;
  }
}
