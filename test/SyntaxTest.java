// $Header$

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

/**
 * J2MEUnit tests for Jili's Syntax module.  DO NOT SUBCLASS.  public
 * access granted only because j2meunit makes it necessary.
 */
public class SyntaxTest extends TestCase
{
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner SyntaxTest</code>
   */
  public SyntaxTest() { }

  /** Clones constructor from superclass.  */
  private SyntaxTest(String name)
  {
    super(name);
  }

  private int dostring(Lua L, String s)
  {
    System.out.println("[[" + s + "]]");
    return L.doString(s);
  }

  public void testSyntax0()
  {
    System.out.println("Syntax0");
    Lua L = new Lua();
    Object o = null;
    try
    {
      o = Syntax.parser(L, Lua.stringReader(""), "Syntax0");
    }
    catch (Exception e_)
    {
    }
    assertNotNull("Parse result", o);
  }

  public void testSyntax1()
  {
    System.out.println("Syntax1");
    Lua L = new Lua();
    L.load(Lua.stringReader(""), "Syntax1");
    L.call(0, 0);
  }

  public void testSyntax2()
  {
    System.out.println("Syntax2");
    Lua L = new Lua();
    assertTrue("script 1 okay", 0 == dostring(L, ""));
    L.setTop(0) ;

    assertTrue("script 2 okay", 0 == dostring(L, " \t"));
    L.setTop(0) ;

    assertTrue("script 3 okay", 0 == dostring(L, "\n\n"));
    L.setTop(0) ;

    assertTrue("script 4 okay", 0 == dostring(L, "return 99"));
    assertTrue("script 4 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == 99.0) ;
    L.setTop(0) ;

    assertTrue("script 5 okay", 0 == dostring(L, "  return -99 ;  "));
    assertTrue("script 5 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == -99.0) ;
    L.setTop(0) ;

    assertTrue("script 6 okay", 0 == dostring(L, "do return 77 end"));
    assertTrue("script 6 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == 77.0) ;
    L.setTop(0) ;

    assertTrue("script 7 okay", 0 == dostring(L, "repeat do return 77 end until 5"));
    assertTrue("script 7 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == 77.0) ;
    L.setTop(0) ;

    assertTrue("script 8 okay", 0 == dostring(L, "do local f = 7 ; return f  end"));
    assertTrue("script 8 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == 7.0) ;
    L.setTop(0) ;

    assertTrue("script 9 okay", 0 == dostring(L, "  return \"This is a String\";  "));
    assertTrue("script 9 result test", L.value(1) instanceof String && ((String)L.value(1)).equals ("This is a String")) ;
    L.setTop(0) ;

    assertTrue("script 10 okay", 0 == dostring(L, "return true"));
    assertTrue("script 10 result test", L.value(1) instanceof Boolean && ((Boolean)L.value(1)).booleanValue() == true) ;
    L.setTop(0) ;

    assertTrue("script 11 okay", 0 == dostring(L, "return false"));
    assertTrue("script 11 result test", L.value(1) instanceof Boolean && ((Boolean)L.value(1)).booleanValue() == false) ;
    L.setTop(0) ;

    assertTrue("script 12 okay", 0 == dostring(L, "return nil"));
    assertTrue("script 12 result test", L.value(1) == L.NIL);
    L.setTop(0) ;


  }

  /** Test that function calls are compiled. */
  public void testSyntax3()
  {
    System.out.println("Syntax3");
    Lua L = new Lua();
    BaseLib.open(L);
    assertTrue("script 1 okay", 0 == dostring(L, "print'hello'"));
  }

  public void testSyntax4()
  {
    System.out.println ("Syntax4") ;
    Lua L = new Lua();
    BaseLib.open(L);
    assertTrue("script 1 okay", 0 == dostring(L, "local a, b = 3, 8 ; return a*b"));
    assertTrue("script 1 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == 24.0) ;
    L.setTop(0) ;

    assertTrue("script 2 okay", 0 == dostring(L, "do local a = 6 ; return 8+a end"));
    assertTrue("script 2 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == 14.0) ;
    L.setTop(0) ;

    assertTrue("script 3 okay", 0 == dostring(L, "local a = 1 ; while a < 5 do print('thing') ; a = a+1 end"));
    L.setTop(0) ;

    assertTrue("script 4 okay", 0 == dostring(L, "for a = 1, 10 do print('thing') end"));
    L.setTop(0) ;

    assertTrue("script 5 okay", 0 == dostring(L, "local a = 1 ; a = a+4 ; return a"));
    assertTrue("script 5 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == 5.0) ;
    L.setTop(0) ;

    assertTrue("script 6 okay", 0 == dostring(L, "local a,b,c,d = 4,'df',7 ; a,d = d,a ; print(a); print(b); print(c); print(d) ; return a"));
    L.setTop(0) ;
  }    

  public void testSyntax5()
  {
    System.out.println ("Syntax5") ;
    Lua L = new Lua();
    BaseLib.open(L);
    assertTrue("script 1 okay", 0 == dostring(L, "local a = { zong = 42, 100, ['foo'] = 7676 } ; print(a.zong) ; print(a) ; return a['foo']"));
    assertTrue("script 1 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == 7676.0) ;
    L.setTop(0) ;

    assertTrue("script 2x okay", 0 == dostring(L, "return function (a, b, ...) local f = function (a) return a end ; local c = a*b ; return f(c),... end")) ;
    assertTrue("script 2x okay", 0 == dostring(L, "local foo = function (a, b, ...) local c = a*b ; return c,... end")) ;

    assertTrue("script 2 okay", 0 == dostring(L, "local foo; foo = function (a) return a end")) ;
    L.setTop(0) ;

    assertTrue("script 2 okay", 0 == dostring(L, "local foo = function (a) return a*a end ; return foo")) ;
    L.setTop(0) ;
    /*

    assertTrue("script 2 okay", 0 == dostring(L, "local foo = function (a) return a*a end ; return foo(4)")) ;
    assertTrue("script 2 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == 16.0) ;
    L.setTop(0) ;

    assertTrue("script 3 okay", 0 == dostring(L, "local foo = function (a) return a*a end ; return foo(foo(4))")) ;
    assertTrue("script 3 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == 7676.0) ;
    L.setTop(0) ;
    */
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTest(new SyntaxTest("testSyntax0")
        {
            public void runTest() { testSyntax0(); } 
        });
    suite.addTest(new SyntaxTest("testSyntax1")
        {
            public void runTest() { testSyntax1(); }
        });
    suite.addTest(new SyntaxTest("testSyntax2")
        {
            public void runTest() { testSyntax2(); }
        });
    suite.addTest(new SyntaxTest("testSyntax3")
        {
            public void runTest() { testSyntax3(); }
        });
    suite.addTest(new SyntaxTest("testSyntax4")
        {
            public void runTest() { testSyntax4(); }
        });
    suite.addTest(new SyntaxTest("testSyntax5")
        {
            public void runTest() { testSyntax5(); }
        });
    return suite;
  }
}
