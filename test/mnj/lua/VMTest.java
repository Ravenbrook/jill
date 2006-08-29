// $Header$

package mnj.lua;

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestSuite;

// The VMTest uses ancillary files:
// Mostly these are compiled versions of one line Lua scripts compiled
// in the following manner:
//   luac -s -o VMTestLoadbool.luc - << 'EOF'
//   return x==nil
//   EOF
// (Mostly they are stripped, but not all of them are)
//
// Note that because the VM is a core component of the Lua system, the
// tests are arranged so that they test the VM whilst using very few
// other facilities.  Note that in particular none of the tests rely on
// facilities implemented by any libraries (including even the base
// library).
//
// In the following list, the Lua script follows the filename.
//
// VMTestLoadbool.luc - a binary chunk containing OP_LOADBOOL.
//   return x==nil
// VMTestLoadnil.luc - a binary chunk containing OP_LOADNIL.
//   local a,b,c; a,b,c="foo","bar","baz"; a,b,c=7; return a,b,c
// VMTestAdd.luc - a binary chunk containing OP_ADD.
//   local a,b,c=3,7,8;return a+b+c
// VMTestSub.luc - a binary chunk containing OP_SUB.
//   local a,b,c = 18, 3, 5;return a - (b - c)
// VMTestConcat.luc - a binary chunk containing OP_CONCAT.
//   local a,b,c="foo","bar","baz";return a..b..c
// VMTestSettable.luc - a binary chunk containing OP_SETTABLE.
//   return {a=1, b=2, c=3}
// VMTestGettable.luc - a binary chunk containing OP_GETTABLE.
//   local a={a=2,b=3,c=23};return a.a+a.b+a.c
// VMTestSetlist.luc - a binary chunk containing OP_SETLIST.
//   local a = {13, 18, 1};return a[1]+a[2]+a[3]
// VMTestCall.luc - a binary chunk containing OP_CALL.
//   return f(7)+1
// VMTestClosure.luc - a binary chunk containing OP_CLOSURE.
//   return function(x)return "foo"..x end 
// VMTestCall1.luc - a binary chunk containing both OP_CLOSURE and OP_CALL.
//   function f(x)return'f'..x end;return f'x'..f'y'
// VMTestJmp.luc - a binary chunk containing OP_JMP (and OP_LT).
//   local a=0;while a < 10 do a=a*2+1 end;return a
// VMTestJmp1.luc - more interesting chunk with OP_JMP
//   function f(x) 
//     local y=0
//     while x~=1 do y=y+1;if x%2==0 then x=x/2 else x=3*x+1 end end 
//     return y end
//   return(f(27)) -- avoid unimplemented OP_TAILCALL
// VMTestUpval.luc - creates and uses UpVals.
//   local a=0;return function()a=a+1;return a;end
// VMTestLe.luc - contains OP_LE
//   local a=0;while a<=10 do a=a+1 end;return a
// VMTestTest.luc - contains OP_TEST
//   if x then return 1 else return 2 end
// VMTestTestset.luc - contains OP_TESTSET
//   local x=x;return x or y,x and y
// VMTestUnm.luc - contains OP_UNM
//   local a,b=7;b=-a;return -b,-a
// VMTestNot.luc - contains OP_NOT
//   local a,b,c,d=nil,false,true,"";return not a, not b, not c, not d
// VMTestLen.luc - contains OP_LEN
//   return #{},#'',#{'bob','rolly','dizzy'},#'length'
// VMTestClose.luc - contains OP_CLOSE
//   local a,f=0 
//   do 
//     local a=0 
//     f=function() a=a+1;return a end 
//   end 
//   return f,function()a=a+1;return a end
// VMTestVararg.luc - contains OP_VARARG
//   function f(a, ...) local b,c,d=... return a,c,d end
// VMTestVararg1.luc - Used to call f defined in VMTestVararg.luc
//   local a,b,c = f('one', 'two', 'three', 'four', 'five', 'six', 'seven')
//   return a,b,c
// VMTestSelf.luc - contains OP_SELF
//   local t={foo='bar'}function t:f(k)return self[k]end return(t:f'foo')
// VMTestTailcall.luc - contains OP_TAILCALL
//   x=0
//   function f(a, ...)
//     if a then x=x+1 return f(...) end
//     return x
//   end
//   return f(6, 7, 8)
// VMTestConvert.luc - contains a number to string conversion.
//   local x = 1 - 1e-16; return 1==x,x..''
// VMTestConvert1.luc - contains several string to number conversions.
//   return (-'1'+'2'-'3'*'7'/'8')%'1'
// VMTestConvert2.luc - contains hexadecimal string to number
// conversion.
//   return '0x99' + '0x66'
// VMTestFor.luc - contains a for loop
//   local x=0 for i=1,10 do x = x + i end return x
// VMTestFor1.luc - contains a generator based for loop
//   function range(i,j)
//     local function upto(s, v) if v == nil then return i end
//         v=v+1
//         if v<=j then return v end
//       end
//     return upto
//   end
//   local x=0
//   for i in range(1,10) do x = x + i end
//   return x
// VMTestRet.luc - test that OP_RET pads out correctly with nils
//   local a,b,c=1,2,function()end a,b,c=c() return a,b,c
// VMTest.lua - contains more tests

/**
 * J2MEUnit tests for Jili's VM execution.  DO NOT SUBCLASS.  public
 * access granted only because j2meunit makes it necessary.
 */
public class VMTest extends JiliTestCase
{
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner VMTest</code>
   */
  public VMTest() { }

  /** Clones constructor from superclass.  */
  private VMTest(String name)
  {
    super(name);
  }

  public void runTest()
  {
    Lua L = new Lua();
    L.loadFile("VMTest.lua");
    L.call(0, 0);
    System.out.println(getName());
    L.push(L.getGlobal(getName()));
    L.call(0, 0);
    assertTrue("Completed", true);
  }

  /**
   * Tests execution of the OP_LOADBOOL opcode.  This opcode is
   * generated when the results of a boolean expression are used for its
   * value (as opposed to inside an "if").  Our test is "return x==nil".
   * This generates both the skip and non-skip forms.
   */
  public void testVMLoadbool()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestLoadbool");
    L.call(0, 1);
    Object res = L.value(1);
    Boolean b = (Boolean)res;
    assertTrue("Result is true", b.booleanValue());
    L.rawSet(L.getGlobals(), "x", "foo");
    loadFile(L, "VMTestLoadbool");
    L.call(0, 1);
    res = L.value(-1);
    b = (Boolean)res;
    assertTrue("Result is false", b.booleanValue() == false);
  }

  /**
   * Tests execution of OP_LOADNIL opcode.  This opcode is generated for
   * assignment sequences like "a,b,c=7".
   */
  public void testVMLoadnil()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestLoadnil");
    L.call(0, 3);
    Object first = L.value(1);
    Double d = (Double)first;
    assertTrue("First result is 7", d.doubleValue() == 7);
    assertTrue("Second result is nil", L.value(2) == L.NIL);
    assertTrue("Third result is nil", L.value(3) == L.NIL);
  }

  /** Tests execution of OP_ADD opcode. */
  public void testVMAdd()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestAdd");
    L.call(0, 1);
    Object res = L.value(1);
    Double d = (Double)res;
    assertTrue("Result is 18", d.doubleValue() == 18);
  }

  /** Tests execution of OP_SUB opcode. */
  public void testVMSub()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestSub");
    L.call(0, 1);
    Object res = L.value(1);
    Double d = (Double)res;
    assertTrue("Result is 20", d.doubleValue() == 20);
  }

  /** Tests execution of OP_CONCAT opcode. */
  public void testVMConcat()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestConcat");
    L.call(0, 1);
    Object res = L.value(1);
    String s = (String)res;
    assertTrue("Result is foobarbaz", s.equals("foobarbaz"));
  }

  /** Tests execution of OP_SETTABLE opcode. */
  public void testVMSettable()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestSettable");
    L.call(0, 1);
    Object t = L.value(1);
    Double d;
    d = (Double)L.rawGet(t, "a");
    assertTrue("t.a == 1", d.doubleValue() == 1);
    d = (Double)L.rawGet(t, "b");
    assertTrue("t.b == 2", d.doubleValue() == 2);
    d = (Double)L.rawGet(t, "c");
    assertTrue("t.c == 3", d.doubleValue() == 3);
    assertTrue("t.d == nil", L.isNil(L.rawGet(t, "d")));
  }

  /** Tests execution of OP_GETTABLE opcode. */
  public void testVMGettable()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestGettable");
    L.call(0, 1);
    Double d = (Double)L.value(1);
    assertTrue("Result is 28", d.doubleValue() == 28);
  }

  /**
   * Tests execution of OP_SETLIST opcode.
   * :todo: There are special cases in SETLIST that are currently untested:
   * When field B is 0 (all elements up to TOS are set);
   * when field C is 0 (starting index is loaded from next opcode).
   * The former is presumably generated when a list initialiser has at
   * least 25,600 (512*50) elements (!).  The latter is generated when an
   * expression like "{...}" is used.
   */
  public void testVMSetlist()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestSetlist");
    L.call(0, 1);
    Double d = (Double)L.value(1);
    assertTrue("Result is 32", d.doubleValue() == 32);
  }

  /** Tests execution of OP_CALL opcode (when calling Lua Java function). */
  public void testVMCall()
  {
    Lua L = new Lua();
    // Create a Lua Java function and install it in the global 'f'.
    class Mine extends LuaJavaCallback
    {
      int luaFunction(Lua L)
      {
        double d = L.toNumber(L.value(1));
        L.pushNumber(d*3);
        return 1;
      }
    }
    L.rawSet(L.getGlobals(), "f", new Mine());
    loadFile(L, "VMTestCall");
    L.call(0, 1);
    assertTrue("Result is 22", L.toNumber(L.value(1)) == 22);
  }

  /** Tests execution of OP_CLOSURE opcode.  Generated when functions
   * are defined. */
  public void testVMClosure()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestClosure");
    L.call(0, 1);
    Object o = L.value(1);
    L.push("bar");
    L.call(1, 1);
    assertTrue("Result is foobar", "foobar".equals(L.value(-1)));
  }

  /** Tests execution of OP_CALL for calling Lua. */
  public void testVMCall1()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestCall1");
    L.call(0, 1);
    Object o = L.value(1);
    assertTrue("Result is fxfy", "fxfy".equals(L.value(-1)));
  }

  /** Tests execution of OP_JMP opcode. */
  public void testVMJmp()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestJmp");
    L.call(0, 1);
    Object o = L.value(1);
    assertTrue("Result is 15", o.equals(new Double(15)));
  }

  /**
   * Tests execution of OP_JMP opcode some more.  The function is taken
   * from the "3x+1" problem.  It computes elements from Sloane's
   * sequence A006577
   * (see http://www.research.att.com/~njas/sequences/A006577 ).
   */
  public void testVMJmp1()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestJmp1");
    L.call(0, 1);
    Object o = L.value(1);
    assertTrue("Result is 111", o.equals(new Double(111)));
  }

  /**
   * Tests execution of Upvalues.  This tests OP_GETUPVAL, OP_SETUPVAL,
   * that OP_CLOSURE creates UpVals, and that OP_RET closes UpVals.
   */
  public void testVMUpval()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestUpval");
    L.call(0, 1);
    LuaFunction f = (LuaFunction)L.value(1);
    L.push(f);
    L.call(0, 0);
    L.push(f);
    L.call(0, 0);
    L.push(f);
    L.call(0, 1);
    Object o = L.value(-1);
    assertTrue("Result is 3", o.equals(new Double(3)));
  }

  /** Tests execution of OP_LE opcode.  */
  public void testVMLe()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestLe");
    L.call(0, 1);
    Object o = L.value(1);
    assertTrue("Result is 11", o.equals(new Double(11)));
  }

  /** Tests execution of OP_TEST opcode.  */
  public void testVMTest()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestTest");
    L.call(0, 1);
    Object o = L.value(-1);
    assertTrue("Result is 2", o.equals(new Double(2)));
    L.rawSet(L.getGlobals(), "x", L.valueOfBoolean(false));
    loadFile(L, "VMTestTest");
    L.call(0, 1);
    o = L.value(-1);
    assertTrue("Result is 2", o.equals(new Double(2)));
    L.rawSet(L.getGlobals(), "x", L.valueOfBoolean(true));
    loadFile(L, "VMTestTest");
    L.call(0, 1);
    o = L.value(-1);
    assertTrue("Result is 1", o.equals(new Double(1)));
  }

  /** Tests execution of OP_TESTSET opcode.  */
  public void testVMTestset()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestTestset");
    L.rawSet(L.getGlobals(), "y", L.valueOfNumber(7));
    L.call(0, 2);
    Object o = L.value(-2);
    Object p = L.value(-1);
    assertTrue("x or y is 7", o.equals(new Double(7)));
    assertTrue("x and y is nil", L.isNil(p));
    L.rawSet(L.getGlobals(), "x", L.valueOfBoolean(true));
    loadFile(L, "VMTestTestset");
    L.call(0, 2);
    o = L.value(-2);
    p = L.value(-1);
    assertTrue("x or y is true", o.equals(L.valueOfBoolean(true)));
    assertTrue("x and y is 7", p.equals(new Double(7)));
  }

  /** Tests execution of OP_UNM opcode.  */
  public void testVMUnm()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestUnm");
    L.call(0, 2);
    Object o = L.value(1);
    Object p = L.value(2);
    assertTrue("First result is 7", o.equals(new Double(7)));
    assertTrue("Second result is -7", p.equals(new Double(-7)));
  }

  /** Tests execution of OP_NOT opcode.  */
  public void testVMNot()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestNot");
    L.call(0, 4);
    Object[] o = new Object[] { L.value(1),
        L.value(2),
        L.value(3),
        L.value(4) };
    assertTrue("First result is true", o[0].equals(L.valueOfBoolean(true)));
    assertTrue("Second result is true", o[1].equals(L.valueOfBoolean(true)));
    assertTrue("Third result is false", o[2].equals(L.valueOfBoolean(false)));
    assertTrue("Fourth result is false", o[3].equals(L.valueOfBoolean(false)));
  }

  /** Tests execution of OP_LEN opcode.  */
  public void testVMLen()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestLen");
    L.call(0, 4);
    Object[] o = new Object[] { L.value(1),
        L.value(2),
        L.value(3),
        L.value(4) };
    assertTrue("First result is 0", o[0].equals(L.valueOfNumber(0)));
    assertTrue("Second result is 0", o[1].equals(L.valueOfNumber(0)));
    assertTrue("Third result is 3", o[2].equals(L.valueOfNumber(3)));
    assertTrue("Fourth result is 6", o[3].equals(L.valueOfNumber(6)));
  }

  /** Tests execution of OP_CLOSE opcode.  */
  public void testVMClose()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestClose");
    L.call(0, 2);
    Object f = L.value(1);
    Object g = L.value(2);
    // Check that f and g have different upvalues by calling them
    // different numbers of times and checking their results.
    for (int i=0; i<3; ++i)
    {
      L.push(f);
      L.call(0,0);
    }
    L.push(g);
    L.call(0, 1);
    assertTrue("g's result is 1", L.value(-1).equals(L.valueOfNumber(1)));
    L.push(f);
    L.call(0, 1);
    assertTrue("f's result is 4", L.value(-1).equals(L.valueOfNumber(4)));
  }

  /** Tests execution of OP_VARARG opcode.  */
  public void testVMVararg()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestVararg");
    L.call(0, 0);  // side-effect, defines global 'f'
    L.push(L.rawGet(L.getGlobals(), "f"));
    int narg = 7;
    for (int i=0; i<narg; ++i)
    {
      L.push(Integer.toString(i));
    }
    L.call(narg, 3);
    assertTrue("First result is '0'", "0".equals(L.value(-3)));
    assertTrue("Second result is '2'", "2".equals(L.value(-2)));
    assertTrue("Third result is '3'", "3".equals(L.value(-1)));

    // Same, but call f from Lua this time.
    loadFile(L, "VMTestVararg1");
    L.call(0, 3);
    assertTrue("First result is one", "one".equals(L.value(-3)));
    assertTrue("Second result is three", "three".equals(L.value(-2)));
    assertTrue("Third result is four", "four".equals(L.value(-1)));
  }

  /** Tests execution of OP_SELF opcode.  Generated when functions
   * are called using the colon syntax (t:f()). */
  public void testVMSelf()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestSelf");
    L.call(0, 1);
    assertTrue("Result is bar", "bar".equals(L.value(-1)));
  }

  /** Tests execution of OP_TAILCALL opcode.  Both when b==0 and when b
   * > 0. */
  public void testVMTailcall()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestTailcall");
    L.call(0, 1);
    assertTrue("Result is 3", L.valueOfNumber(3).equals(L.value(-1)));
  }

  /** Tests conversion of numbers to string.  Pretty weak, but
   * illustrates a difference between PUC-Rio Lua 5.1 and Jili.
   */
  public void testVMConvert()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestConvert");
    L.call(0, 2);
    assertTrue("First result is false",
      L.valueOfBoolean(false).equals(L.value(-2)));
    System.out.println("VMTestConvert conversion: " + L.value(-1));
  }

  /** Tests conversion of strings to numbers.  */
  public void testVMConvert1()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestConvert1");
    L.call(0, 1);
    assertTrue("Result is 0.375", L.valueOfNumber(0.375).equals(L.value(-1)));
  }

  /** Tests conversion of a hexadecimal string to number.  */
  public void testVMConvert2()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestConvert2");
    L.call(0, 1);
    assertTrue("Result is 255", L.valueOfNumber(255).equals(L.value(-1)));
  }

  /** Tests for loop. */
  public void testVMFor()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestFor");
    L.call(0, 1);
    assertTrue("Result is 55", L.valueOfNumber(55).equals(L.value(-1)));
  }

  /** Tests generator based for loop. */
  public void testVMFor1()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestFor1");
    L.call(0, 1);
    assertTrue("Result is 55", L.valueOfNumber(55).equals(L.value(-1)));
  }

  /** Tests that OP_RET pads out correctly with nils. */
  public void testVMRet()
  {
    Lua L = new Lua();
    loadFile(L, "VMTestRet");
    L.call(0, 3);
    for (int i=1; i<=3; ++i)
    {
      assertTrue("Result " + i + " is nil",
          L.isNil(L.value(i)));
    }
  }

  /** Tests that the rule of Conway's life is correctly implemented. */
  public void testliferule(String suffix)
  {
    Lua L = new Lua();
    loadFileAndRun(L, "VMTest" + suffix, "testliferule", 1);
    double v = L.toNumber(L.value(-1));
    System.out.println("" + v);
    assertTrue("Result is 16480", v == 16480);
  }

  public void testindexnil()
  {
    Lua L = new Lua();
    loadFileAndRun(L, "VMTest.lua", getName(), 1);
    assertTrue("Result is nil", L.isNil(L.value(-1)));
  }

  public void testlentable()
  {
    Lua L = new Lua();
    loadFileAndRun(L, "VMTest.lua", getName(), 1);
    assertTrue("Result is 9", L.toNumber(L.value(-1)) == 9);
  }

  public void testrehash()
  {
    Lua L = new Lua();
    loadFileAndRun(L, "VMTest.lua", getName(), 1);
    assertTrue("Result is true",
        L.value(-1).equals(L.valueOfBoolean(true)));
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new VMTest("testVMLoadbool")
    {
        public void runTest() { testVMLoadbool(); } });
    suite.addTest(new VMTest("testVMLoadnil")
        {
        public void runTest() { testVMLoadnil(); } });
    suite.addTest(new VMTest("testVMAdd")
        {
        public void runTest() { testVMAdd(); } });
    suite.addTest(new VMTest("testVMSub")
        {
        public void runTest() { testVMSub(); } });
    suite.addTest(new VMTest("testVMConcat")
        {
        public void runTest() { testVMConcat(); } });
    suite.addTest(new VMTest("testVMSettable")
        {
        public void runTest() { testVMSettable(); } });
    suite.addTest(new VMTest("testVMGettable")
        {
        public void runTest() { testVMGettable(); } });
    suite.addTest(new VMTest("testVMSetlist")
        {
        public void runTest() { testVMSetlist(); } });
    suite.addTest(new VMTest("testVMCall")
        {
        public void runTest() { testVMCall(); } });
    suite.addTest(new VMTest("testVMClosure")
        {
        public void runTest() { testVMClosure(); } });
    suite.addTest(new VMTest("testVMCall1")
        {
        public void runTest() { testVMCall1(); } });
    suite.addTest(new VMTest("testVMJmp")
        {
        public void runTest() { testVMJmp(); } });
    suite.addTest(new VMTest("testVMJmp1")
        {
        public void runTest() { testVMJmp1(); } });
    suite.addTest(new VMTest("testVMUpval")
        {
        public void runTest() { testVMUpval(); } });
    suite.addTest(new VMTest("testVMLe")
        {
        public void runTest() { testVMLe(); } });
    suite.addTest(new VMTest("testVMTest")
        {
        public void runTest() { testVMTest(); } });
    suite.addTest(new VMTest("testVMTestset")
        {
        public void runTest() { testVMTestset(); } });
    suite.addTest(new VMTest("testVMUnm")
        {
        public void runTest() { testVMUnm(); } });
    suite.addTest(new VMTest("testVMNot")
        {
        public void runTest() { testVMNot(); } });
    suite.addTest(new VMTest("testVMLen")
        {
        public void runTest() { testVMLen(); } });
    suite.addTest(new VMTest("testVMClose")
        {
        public void runTest() { testVMClose(); } });
    suite.addTest(new VMTest("testVMVararg")
        {
        public void runTest() { testVMVararg(); } });
    suite.addTest(new VMTest("testVMSelf")
        {
        public void runTest() { testVMSelf(); } });
    suite.addTest(new VMTest("testVMTailcall")
        {
        public void runTest() { testVMTailcall(); } });
    suite.addTest(new VMTest("testVMConvert")
        {
        public void runTest() { testVMConvert(); } });
    suite.addTest(new VMTest("testVMConvert1")
        {
        public void runTest() { testVMConvert1(); } });
    suite.addTest(new VMTest("testVMConvert2")
        {
        public void runTest() { testVMConvert2(); } });
    suite.addTest(new VMTest("testVMFor")
        {
        public void runTest() { testVMFor(); } });
    suite.addTest(new VMTest("testVMFor1")
        {
        public void runTest() { testVMFor1(); } });
    suite.addTest(new VMTest("testVMRet")
        {
        public void runTest() { testVMRet(); } });
    suite.addTest(new VMTest("testvarlist"));
    suite.addTest(new VMTest("testliferule")
      {
        public void runTest() { testliferule(".lua"); }
      });
    suite.addTest(new VMTest("testliferule2")
      {
        public void runTest() { testliferule(".luc"); }
      });
    suite.addTest(new VMTest("testindexnil")
      {
        public void runTest() { testindexnil(); }
      });
    suite.addTest(new VMTest("testlentable")
      {
        public void runTest() { testlentable(); }
      });
    suite.addTest(new VMTest("testrehash")
      {
        public void runTest() { testrehash(); }
      });
    return suite;
  }
}
