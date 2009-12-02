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
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

/**
 * Compendium of all Jill's tests that run in the J2ME environment.
 */
public class METest extends TestCase
{
  public Test suite()
  {
    TestSuite suite = new TestSuite();
    TestCase t;

    t = new ObjectAllTest();
    suite.addTest(t.suite());
    t = new LoaderTest();
    suite.addTest(t.suite());
    t = new LuaTest();
    suite.addTest(t.suite());
    t = new VMTest();
    suite.addTest(t.suite());
    t = new BaseLibTest();
    suite.addTest(t.suite());
    t = new StringLibTest();
    suite.addTest(t.suite());
    t = new SyntaxTest();
    suite.addTest(t.suite());
    t = new MetaTest();
    suite.addTest(t.suite());
    t = new OSLibTest();
    suite.addTest(t.suite());
    t = new TableLibTest();
    suite.addTest(t.suite());
    t = new CoroTest();
    suite.addTest(t.suite());
    t = new PackageLibTest();
    suite.addTest(t.suite());
    t = new MathLibTest();
    suite.addTest(t.suite());

    return suite;
  }
}
