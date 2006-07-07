// $Header$

package mnj.lua;
import java.io.File ;
import java.io.FileOutputStream ;
import java.io.IOException ;
import java.util.Hashtable;

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

/**
 * J2MEUnit tests for Jili's Syntax module.  DO NOT SUBCLASS.  public
 * access granted only because j2meunit makes it necessary.
 */
public class SyntaxTest extends JiliTestCase
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

    assertTrue("script 2 okay", 0 == dostring(L, "local foo; foo = function (a) return a end")) ;
    L.setTop(0) ;

    assertTrue("script 3 okay", 0 == dostring(L, "local foo = function (a) return a*a end ; return foo")) ;
    L.setTop(0) ;

    assertTrue("script 4 okay", 0 == dostring(L, "local foo = function (a) return a*a end ; return foo(4)")) ;
    assertTrue("script 4 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == 16.0) ;
    L.setTop(0) ;

    assertTrue("script 5 okay", 0 == dostring(L, "return function (a, b, ...) local f = function (a) return a end ; local c = a*b ; return f(c),... end")) ;
    L.setTop(0) ;

    assertTrue("script 6 okay", 0 == dostring(L, "local foo = function (a, b, ...) local c = a*b ; return c,... end")) ;
    L.setTop(0) ;

    assertTrue("script 7 okay", 0 == dostring(L, "local foo = function (a) return a*a end ; return foo(foo(4))")) ;
    assertTrue("script 7 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == 256.0) ;
    L.setTop(0) ;

  }

  public void describe_stack (Lua L)
  {
    System.out.println ("STACK:") ;
    for (int i = 1 ; i <= L.getTop() ; i++)
      System.out.println ("stack["+i+"] = "+L.value(i)) ;
  }

  public void testSyntax6()
  {
    System.out.println ("Syntax6") ;
    Lua L = new Lua();
    BaseLib.open(L);

    L.setTop(0) ;
    compileLoadFile(L, "marktest2");
    //describe_stack(L) ;
    assertTrue (L.value(1) instanceof LuaFunction) ;

    L.setTop(0) ;
    compileLoadFile(L, "MetaTest");
    assertTrue (L.value(1) instanceof LuaFunction) ;

    L.setTop(0) ;
    compileLoadFile(L, "ChunkSpy");
    assertTrue (L.value(1) instanceof LuaFunction) ;
  }

  public void testSyntax7()
  {
    System.out.println ("Syntax7") ;
    Lua L = new Lua();
    BaseLib.open(L);

    assertTrue("if 1 okay", 0 == dostring(L,
        "local a, b = 2, 5 ; if a+3 == b then return a*b-(b/a) else return false end"
        ));
    assertTrue("if 1 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == 7.5) ;
    L.setTop(0) ;

    assertTrue("if 2 okay", 0 == dostring(L,
        "local a,b,c=true,false,nil if b or c then return -2 elseif a and not b then return 42.0 else return -1 end"
        ));
    assertTrue("if 2 result test", L.value(1) instanceof Double && ((Double)L.value(1)).doubleValue() == 42.0) ;
    L.setTop(0) ;

    assertTrue("if 3 okay", 0 == dostring(L,
        "local a = 1 local b if a then b = a*a else b = a+a end if b == 1 then return true else return nil end"
        ));
    assertTrue("if 3 result test", L.value(1) instanceof Boolean && ((Boolean)L.value(1)).booleanValue()) ;
    L.setTop(0) ;

    assertTrue("if 4 okay", 0 == dostring(L,
        "local a,b,c,d,e,f = false, true, 7, nil, nil, true ;"+
        "if (a or (b and c) and (d or not e) and f) then return 'succeed' else return nil end"
        ));
    assertTrue("if 4 result test", L.value(1) instanceof String && "succeed".equals (L.value(1)));
    L.setTop(0) ;

  }

  public void testSyntax8()
  {
    System.out.println ("Syntax8") ;
    Lua L = new Lua();
    BaseLib.open(L);

    L.setTop(0) ;
    assertTrue("table 1 okay", 0 == dostring(L,
        "local t = {} ; t.foo = 45 ; t[1] = 17 ; t[3] = 1 ; t[1] = 'laa' ; return t"
        ));
    Object res = L.value(1) ;
    assertTrue("table 1 result test", res instanceof Hashtable &&
               ((Hashtable)res).size() == 3 &&
               double_equal (((Hashtable)res).get("foo"), 45) &&
               double_equal (((Hashtable)res).get(new Double (3.0)), 1.0) &&
               "laa".equals (((Hashtable)res).get(new Double (1.0)))) ;
    
    L.setTop(0) ;
    assertTrue("table 2 okay", 0 == dostring(L,
        "local t = {} ; t.foo = 45 ; t[1] = 17 ; t[2], t[3] = 2, 1 ; t[1] = 'laa' ; t['foo'] = t t[3] = t[3] + #t; return t"
        ));
    res = L.value(1) ;
    assertTrue("table 2 result test", res instanceof Hashtable &&
               ((Hashtable)res).size() == 4 &&
               ((Hashtable)res).get("foo") == res &&
               double_equal (((Hashtable)res).get(new Double (3.0)), 4.0) &&
               "laa".equals (((Hashtable)res).get(new Double (1.0)))) ;
    
    L.setTop(0) ;

  }

  public void testSyntax9()
  {
    System.out.println ("Syntax9") ;
    Lua L = new Lua();
    BaseLib.open(L);

    L.setTop(0) ;
    assertTrue("loop 1 okay", 0 == dostring(L,
        "local a = 0 ; for i = 1, 10 do if i == 7 then break end ; a = a+10 end ; return a"
        ));
    Object res = L.value(1) ;
    assertTrue("loop 1 result test", res instanceof Double && ((Double)L.value(1)).doubleValue() == 60.0) ;

    L.setTop(0) ;
    assertTrue("loop 2 okay", 0 == dostring(L,
        "local a,i = 0,100 ; repeat if i == 7 then break end ; a,i = a+10,i-1 until i == 0 ; return a"
        ));
    res = L.value(1) ;
    assertTrue("loop 2 result test", res instanceof Double && ((Double)L.value(1)).doubleValue() == 930.0) ;

    L.setTop(0) ;
    assertTrue("loop 3 okay", 0 == dostring(L,
        "local a,i = 1,1 ; while i < 100 do i=i*1.8213 local z = 4 ; repeat a = a + 1/256 ; z = z-1 ; if z == 2 then break end until z <= 0;  a = a+a end ; return a ;"
        ));
    res = L.value(1) ;
    // does 8 outer loops and 2 inner ones... 256 +255/64
    assertTrue("loop 3 result test", res instanceof Double && ((Double)L.value(1)).doubleValue() == 259.984375) ;

    L.setTop(0) ;

  }

  /**
   * For historical reasons, this no longer tests the Syntax module
   * (compiler), but detects a bug in the VM.
   */
  public void testSyntax10()
  {
    System.out.println ("Syntax10");
    Lua L = new Lua();
    BaseLib.open(L);

    L.setTop(0) ;
    L.loadFile("SyntaxTest10.luc");
    L.call(0, 1);
    Object res = L.value(1) ;
    assertTrue("closures 1 result test", res instanceof LuaTable) ;
    int size = ((LuaTable) res).size () ;
    assertTrue("closures 1 result test#2", size == 3) ;
    
    L.setTop(0) ;
  }

  boolean double_equal (Object o, double d)
  {
    return o instanceof Double &&
      ((Double) o).doubleValue() == d ;
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
    suite.addTest(new SyntaxTest("testSyntax6")
    {
      public void runTest() { testSyntax6(); } 
    });
    suite.addTest(new SyntaxTest("testSyntax7")
    {
      public void runTest() { testSyntax7(); } 
    });
    suite.addTest(new SyntaxTest("testSyntax8")
    {
      public void runTest() { testSyntax8(); } 
    });
    suite.addTest(new SyntaxTest("testSyntax9")
    {
      public void runTest() { testSyntax9(); } 
    });
    suite.addTest(new SyntaxTest("testSyntax10")
    {
      public void runTest() { testSyntax10(); } 
    });
    return suite;
  }
}
