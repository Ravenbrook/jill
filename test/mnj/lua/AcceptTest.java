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

// The Intuwave acceptance tests.

package mnj.lua;

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestSuite;

public class AcceptTest extends JiliTestCase
{
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner AcceptTest</code>
   */
  public AcceptTest() { }

  /** Clones constructor from superclass.  */
  protected AcceptTest(String name)
  {
    super(name);
  }

  public void runTest()
  {
    // loads and runs a test script in the accept-basic directory
    Lua L = new Lua();
    BaseLib.open(L);
    PackageLib.open(L);
    MathLib.open(L);
    OSLib.open(L);
    StringLib.open(L);
    TableLib.open(L);

    System.out.println(getName());
    int status = L.loadFile("accept-basic/" + getName() + ".lua");
    assertTrue(status == 0);
    L.call(0, 0);
  }

  /**
   * This test has separate Java code because the correct test outcome
   * is to generate an error.
   */
  public void testreadonly()
  {
    Lua L = new Lua();
    BaseLib.open(L);
    PackageLib.open(L);
    MathLib.open(L);
    OSLib.open(L);
    StringLib.open(L);
    TableLib.open(L);

    System.out.println(getName());
    int status = L.loadFile("accept-basic/" + getName() + ".lua");
    assertTrue(status == 0);
    status = L.pcall(0, 0, null);
    assertTrue("Script raised error", status != 0);
    String s = (String)L.value(-1);
    // Error message should contain text from the test.
    assertTrue("error looks plausible", s.indexOf("redefine") >= 0);
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new AcceptTest("bisect"));
    suite.addTest(new AcceptTest("cf"));
    suite.addTest(new AcceptTest("factorial"));
    suite.addTest(new AcceptTest("fib"));
    suite.addTest(new AcceptTest("fibfor"));
    // The output from life.lua is too voluminous to run routinely.  See
    // LifeTest.
    // suite.addTest(new AcceptTest("life"));
    suite.addTest(new AcceptTest("readonly")
      {
        public void runTest() { testreadonly(); }
      });
    suite.addTest(new AcceptTest("sieve"));
    suite.addTest(new AcceptTest("sort"));

    return suite;
  }
}
