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

import java.io.InputStream;

// The LoaderTest uses ancillary files:
// LoaderTest0.luc - compiled lua chunk (compiled on big-endian machine),
// result of:
//   luac -s - <<'EOF'
//   return 99
//   EOF
// LoaderTest1.luc - as LoaderTest0.luc but with debug info:
//    luac - <<'EOF'
//    return 99
//  EOF
// LoaderTest2.luc - test/fib.lua from PUC-Rio Lua 5.1: luac fib.lua
// LoaderTest3.luc - as LoaderTest0.luc but compiled on little-endian
// machine.


/**
 * J2MEUnit tests for Jill's internal Loader class.  DO NOT SUBCLASS.  public
 * access granted only because j2meunit makes it necessary.
 */
public class LoaderTest extends TestCase
{
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner LoaderTest</code>
   */
  public LoaderTest() { }

  /** Clones constructor from superclass.  */
  private LoaderTest(String name)
  {
    super(name);
  }

  /**
   * @param filename  filename without '.luc' extension.
   */
  private Proto loadFile(String filename)
  {
    filename += ".luc";
    InputStream in = null;

    System.out.println(filename);

    try
    {
      in = this.getClass().getResourceAsStream(filename);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    assertNotNull("Opened file " + filename, in);

    boolean loaded = false;
    Loader loader = new Loader(in, filename);

    Proto proto = null;
    try
    {
      proto = loader.undump();
      loaded = true;
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    assertTrue("Loaded okay", loaded);

    return proto;
  }

  /** Tests LoaderTest0.luc.  */
  public void testLoader0()
  {
    Proto p = loadFile("LoaderTest0");
    assertNotNull(p);
  }

  /** Tests LoaderTest1.luc.  */
  public void testLoader1()
  {
    loadFile("LoaderTest1");
  }

  /** Tests LoaderTest2.luc.  */
  public void testLoader2()
  {
    loadFile("LoaderTest2");
  }

  /** Tests LoaderTest3.luc. */
  public void testLoader3()
  {
    loadFile("LoaderTest3");
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new LoaderTest("testLoader0")
    {
        public void runTest() { testLoader0(); } });
    suite.addTest(new LoaderTest("testLoader1")
        {
        public void runTest() { testLoader1(); } });
    suite.addTest(new LoaderTest("testLoader2")
        {
        public void runTest() { testLoader2(); } });
    suite.addTest(new LoaderTest("testLoader3")
        {
        public void runTest() { testLoader3(); } });
    return suite;
  }
}
