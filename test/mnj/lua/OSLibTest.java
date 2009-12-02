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
// OSLibTest.lua - Lua source for test.
// OSLibTest.luc - compiled version of OSLibTest.lua

/**
 * J2MEUnit tests for Jill's OSLib (os library).  DO NOT SUBCLASS.
 * public access granted only because j2meunit makes it necessary.
 */
public class OSLibTest extends JiliTestCase
  {
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner OSLibTest</code>
   */
  public OSLibTest() { }

  /** Clones constructor from superclass.  */
  private OSLibTest(String name)
  {
    super(name);
  }

  /**
   * Tests OSLib.
   */
  public void testOSLib()
  {
    System.out.println("OSLibTest.testOSLib()");
    Lua L = new Lua();

    OSLib.open(L);

    Object lib = L.getGlobal("os");
    assertTrue("os table defined", L.isTable(lib));
  }

  /**
   * Opens the base and os libraries into a fresh Lua state,
   * calls a global function, and returns the Lua state.
   * @param name  name of function to call.
   * @param n     number of results expected from function.
   */
  private Lua luaGlobal(String name, int n)
  {
    Lua L = new Lua();
    BaseLib.open(L);
    OSLib.open(L);
    loadFile(L, "OSLibTest");
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

  public void testclock()
  {
    nTrue("testclock", 2);
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new OSLibTest("testOSLib")
      {
        public void runTest() { testOSLib(); }
      });
    suite.addTest(new OSLibTest("testclock")
      {
        public void runTest() { testclock(); }
      });

    return suite;
  }
}
