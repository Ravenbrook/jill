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
// MetaTest.lua - Lua source
// MetaTest.luc - MetaTest.lua compiled.


/**
 * J2MEUnit tests for Jill's metamethods.  DO NOT SUBCLASS.
 * public access granted only because j2meunit makes it necessary.
 */
public class MetaTest extends JiliTestCase
{
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner MetaTest</code>
   */
  public MetaTest() { }

  /** Clones constructor from superclass.  */
  private MetaTest(String name)
  {
    super(name);
  }

  /**
   * Takes a Lua state, calls a global function, and returns the Lua state.
   * @param name  name of function to call.
   * @param n     number of results expected from function.
   */
  private Lua luaGlobal(Lua L, String name, int n)
  {
    // :todo: push into superclass
    loadFile(L, "MetaTest");
    L.call(0, 0);
    System.out.println(name);
    L.push(L.getGlobal(name));
    L.call(0, n);
    return L;
  }

  /**
   * Opens the base library into a fresh Lua state, then calls {@link
   * MetaTest#luaGlobal(Lua, String, int)}.
   */
  private Lua luaGlobal(String name, int n)
  {
    Lua L = new Lua();
    BaseLib.open(L);
    return luaGlobal(L, name, n);
  }

  /**
   * Checks that <var>n</var> results are all true.
   */
  private void nTrue(Lua L, int n)
  {
    for (int i=1; i<=n; ++i)
    {
      assertTrue("Result " + i + " is true",
	  L.valueOfBoolean(true).equals(L.value(i)));
    }
  }

  /**
   * Calls a global lua function and checks that <var>n</var> results
   * are all true.
   */
  private void nTrue(String name, int n)
  {
    Lua L = luaGlobal(name, n);
    nTrue(L, n);
  }

  public void testmetaindex0()
  {
    nTrue("testmetaindex0", 4);
  }

  public void testmetaindex1()
  {
    nTrue("testmetaindex1", 4);
  }

  public void testmetaindex2()
  {
    nTrue("testmetaindex2", 2);
  }

  public void testmetanewindex0()
  {
    nTrue("testmetanewindex0", 5);
  }

  public void testmetanewindex1()
  {
    nTrue("testmetanewindex1", 4);
  }

  public void testmetanewindex2()
  {
    nTrue("testmetanewindex2", 3);
  }

  public void testmetacall()
  {
    nTrue("testmetacall", 3);
  }

  public void testmetalt()
  {
    nTrue("testmetalt", 6);
  }

  public void testmetale()
  {
    nTrue("testmetale", 6);
  }

  public void testmetaadd()
  {
    nTrue("testmetaadd", 2);
  }

  public void testmetasub()
  {
    nTrue("testmetasub", 2);
  }

  public void testmetamul()
  {
    nTrue("testmetamul", 2);
  }

  public void testmetadiv()
  {
    nTrue("testmetadiv", 2);
  }

  public void testmetamod()
  {
    nTrue("testmetamod", 2);
  }

  public void testmetapow()
  {
    nTrue("testmetapow", 2);
  }

  public void testmetaconcat()
  {
    nTrue("testmetaconcat", 2);
  }

  public void testmetaunm()
  {
    nTrue("testmetaunm", 2);
  }

  public void testmetaconst()
  {
    nTrue("testmetaconst", 9);
  }

  public void testmetaeq()
  {
    nTrue("testmetaeq", 3);
  }

  public void testmetalen()
  {
    System.out.println("testmetalen");
    Lua L = new Lua();
    BaseLib.open(L);
    LuaTable mt = new LuaTable();
    L.setMetatable(L.valueOfNumber(0), mt);
    int n = 3;
    nTrue(luaGlobal(L, "testmetalen", n), n);
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new MetaTest("testmetaindex0")
      {
        public void runTest() { testmetaindex0(); }
      });
    suite.addTest(new MetaTest("testmetaindex1")
      {
        public void runTest() { testmetaindex1(); }
      });
    suite.addTest(new MetaTest("testmetaindex2")
      {
        public void runTest() { testmetaindex2(); }
      });
    suite.addTest(new MetaTest("testmetanewindex0")
      {
        public void runTest() { testmetanewindex0(); }
      });
    suite.addTest(new MetaTest("testmetanewindex1")
      {
        public void runTest() { testmetanewindex1(); }
      });
    suite.addTest(new MetaTest("testmetanewindex2")
      {
        public void runTest() { testmetanewindex2(); }
      });
    suite.addTest(new MetaTest("testmetacall")
      {
        public void runTest() { testmetacall(); }
      });
    suite.addTest(new MetaTest("testmetalt")
      {
        public void runTest() { testmetalt(); }
      });
    suite.addTest(new MetaTest("testmetale")
      {
        public void runTest() { testmetale(); }
      });
    suite.addTest(new MetaTest("testmetaadd")
      {
        public void runTest() { testmetaadd(); }
      });
    suite.addTest(new MetaTest("testmetasub")
      {
        public void runTest() { testmetasub(); }
      });
    suite.addTest(new MetaTest("testmetamul")
      {
        public void runTest() { testmetamul(); }
      });
    suite.addTest(new MetaTest("testmetadiv")
      {
        public void runTest() { testmetadiv(); }
      });
    suite.addTest(new MetaTest("testmetamod")
      {
        public void runTest() { testmetamod(); }
      });
    suite.addTest(new MetaTest("testmetapow")
      {
        public void runTest() { testmetapow(); }
      });
    suite.addTest(new MetaTest("testmetaunm")
      {
        public void runTest() { testmetaunm(); }
      });
    suite.addTest(new MetaTest("testmetalen")
      {
        public void runTest() { testmetalen(); }
      });
    suite.addTest(new MetaTest("testmetaconcat")
      {
        public void runTest() { testmetaconcat(); }
      });
    suite.addTest(new MetaTest("testmetaconst")
      {
        public void runTest() { testmetaconst(); }
      });
    suite.addTest(new MetaTest("testmetaeq")
      {
        public void runTest() { testmetaeq(); }
      });
    return suite;
  }
}
