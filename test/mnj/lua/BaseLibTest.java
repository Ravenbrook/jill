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
// BaseLibTestLoadfile.luc
//   return 99
// BaseLibTest.lua - contains functions that test each of base library
// functions.  It is important (for testing "error") that this file is
// not stripped if it is loaded in binary form.

// :todo: test radix conversion for tonumber.
// :todo: test unpack with non-default arguments.
// :todo: test rawequal for things with metamethods.
// :todo: test rawget for tables with metamethods.
// :todo: test rawset for tables with metamethods.
// :todo: (when string library is available) test the strings returned
//     by assert.


/**
 * J2MEUnit tests for Jill's BaseLib (base library).  DO NOT SUBCLASS.
 * public access granted only because j2meunit makes it necessary.
 */
public class BaseLibTest extends JiliTestCase
{
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner BaseLibTest</code>
   */
  public BaseLibTest() { }

  /** Clones constructor from superclass.  */
  private BaseLibTest(String name)
  {
    super(name);
  }

  /**
   * Tests BaseLib.
   */
  public void testBaseLib()
  {
    System.out.println("BaseLibTest.testBaseLib()");
    Lua L = new Lua();

    BaseLib.open(L);

    // Test that each global name is defined as expected.
    String[] name =
    {
      "_VERSION",
      "_G", "ipairs", "pairs", "print", "rawequal", "rawget", "rawset",
      "select", "tonumber", "tostring", "type", "unpack"
    };
    for (int i=0; i<name.length; ++i)
    {
      Object o = L.getGlobal(name[i]);
      assertTrue(name[i] + " exists", !L.isNil(o));
    }
  }

  /**
   * Opens the base library into a fresh Lua state, calls a global
   * function, and returns the Lua state.
   * @param name  name of function to call.
   * @param n     number of results expected from function.
   */
  private Lua luaGlobal(String name, int n)
  {
    Lua L = new Lua();
    BaseLib.open(L);
    loadFile(L, "BaseLibTest");
    L.call(0, 0);
    System.out.println(name);
    L.push(L.getGlobal(name));
    L.call(0, n);
    return L;
  }

  /**
   * Calls a global lua function and checks that <var>n</var> results
   * are all true.
   */
  private void nTrue(String name, int n)
  {
    Lua L = luaGlobal(name, n);
    for (int i=1; i<=n; ++i)
    {
      assertTrue("Result " + i + " is true",
	  L.valueOfBoolean(true).equals(L.value(i)));
    }
  }

  /**
   * Sometimes overridden by anon subclass.
   */
  public void runTest()
  {
    nTrue(getName(), 1);
  }

  /**
   * Tests print.  Not much we can reasonably do here apart from call
   * it.  We can't automatically check that the output appears anywhere
   * or is correct.  This also tests tostring to some extent; print
   * calls tostring internally, so this tests that it can be called
   * without error, for example.
   */
  public void testPrint()
  {
    luaGlobal("testprint", 0);
  }

  public void testTostring()
  {
    nTrue("testtostring", 5);
  }

  public void testTonumber()
  {
    nTrue("testtonumber", 5);
  }

  public void testType()
  {
    nTrue("testtype", 6);
  }

  public void testSelect()
  {
    nTrue("testselect", 2);
  }

  public void testPairs()
  {
    nTrue("testpairs", 4);
  }

  public void testNext()
  {
    nTrue("testnext", 4);
  }

  public void testIpairs()
  {
    nTrue("testipairs", 4);
  }

  public void testRawequal()
  {
    nTrue("testrawequal", 7);
  }

  public void testRawget()
  {
    nTrue("testrawget", 2);
  }

  public void testRawset()
  {
    nTrue("testrawset", 2);
  }

  public void testPcall()
  {
    nTrue("testpcall", 2);
  }

  public void testError()
  {
    nTrue("testerror", 2);
  }

  public void testMetatable()
  {
    nTrue("testmetatable", 2);
  }

  public void test__metatable()
  {
    nTrue("test__metatable", 2);
  }

  /** Tests _VERSION */
  public void testVersion()
  {
    Lua L = new Lua();
    BaseLib.open(L);

    Object o = L.getGlobal("_VERSION");
    assertTrue("_VERSION exists", o != null);
    assertTrue("_VERSION is a string", L.isString(o));
  }

  public void testErrormore()
  {
    nTrue("testerrormore", 2);
  }

  public void testpcall2()
  {
    nTrue("testpcall2", 4);
  }

  public void testpcall3()
  {
    nTrue("testpcall3", 2);
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new BaseLibTest("testBaseLib")
    {
        public void runTest() { testBaseLib(); } });
    suite.addTest(new BaseLibTest("testPrint")
        {
        public void runTest() { testPrint(); } });
    suite.addTest(new BaseLibTest("testTostring")
        {
        public void runTest() { testTostring(); } });
    suite.addTest(new BaseLibTest("testTonumber")
        {
        public void runTest() { testTonumber(); } });
    suite.addTest(new BaseLibTest("testType")
        {
        public void runTest() { testType(); } });
    suite.addTest(new BaseLibTest("testSelect")
        {
        public void runTest() { testSelect(); } });
    suite.addTest(new BaseLibTest("testunpack"));
    suite.addTest(new BaseLibTest("testPairs")
        {
        public void runTest() { testPairs(); } });
    suite.addTest(new BaseLibTest("testIpairs")
        {
        public void runTest() { testIpairs(); } });
    suite.addTest(new BaseLibTest("testRawequal")
        {
        public void runTest() { testRawequal(); } });
    suite.addTest(new BaseLibTest("testRawget")
        {
        public void runTest() { testRawget(); } });
    suite.addTest(new BaseLibTest("testRawset")
        {
        public void runTest() { testRawset(); } });
    suite.addTest(new BaseLibTest("testgetfenv"));
    suite.addTest(new BaseLibTest("testsetfenv"));
    suite.addTest(new BaseLibTest("testNext")
        {
        public void runTest() { testNext(); } });
    suite.addTest(new BaseLibTest("testPcall")
        {
        public void runTest() { testPcall(); } });
    suite.addTest(new BaseLibTest("testError")
        {
        public void runTest() { testError(); } });
    suite.addTest(new BaseLibTest("testMetatable")
        {
        public void runTest() { testMetatable(); } });
    suite.addTest(new BaseLibTest("test__metatable")
        {
        public void runTest() { test__metatable(); } });
    suite.addTest(new BaseLibTest("test__tostring"));
    suite.addTest(new BaseLibTest("testcollectgarbage"));
    suite.addTest(new BaseLibTest("testassert"));
    suite.addTest(new BaseLibTest("testloadstring"));
    suite.addTest(new BaseLibTest("testloadfile"));
    suite.addTest(new BaseLibTest("testload"));
    suite.addTest(new BaseLibTest("testdofile"));
    suite.addTest(new BaseLibTest("testversion")
        {
        public void runTest() { testVersion(); } });
    suite.addTest(new BaseLibTest("testxpcall"));
    suite.addTest(new BaseLibTest("testerrormore")
        {
        public void runTest() { testErrormore(); } });
    suite.addTest(new BaseLibTest("testpcall2")
      {
        public void runTest() { testpcall2(); }
      });
    suite.addTest(new BaseLibTest("testpcall3")
      {
        public void runTest() { testpcall3(); }
      });
    suite.addTest(new BaseLibTest("testpcall4"));
    suite.addTest(new BaseLibTest("testpcall5"));
    suite.addTest(new BaseLibTest("testunpackbig"));
    suite.addTest(new BaseLibTest("testloaderr"));
    suite.addTest(new BaseLibTest("testnanindex"));
    suite.addTest(new BaseLibTest("testhexerror"));
    return suite;
  }
}
