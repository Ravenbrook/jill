// $Header$

// The Intuwave advanced acceptance tests.

package mnj.lua;

// For j2meunit see http://j2meunit.sourceforge.net/
import j2meunit.framework.Test;
import j2meunit.framework.TestSuite;

public class AdvancedTest extends JiliTestCase
{
  /** void constructor, necessary for running using
   * <code>java j2meunit.textui.TestRunner AdvancedTest</code>
   */
  public AdvancedTest() { }

  /** Clones constructor from superclass.  */
  protected AdvancedTest(String name)
  {
    super(name);
  }

  public void runTest()
  {
    Lua L = new Lua();
    BaseLib.open(L);
    PackageLib.open(L);
    MathLib.open(L);
    OSLib.open(L);
    StringLib.open(L);
    TableLib.open(L);
    loadFile(L, "accept-advanced/" + getName());
    int status = L.pcall(0, 0, new AddWhere());
    if (status != 0)
    {
      System.out.println(L.toString(L.value(-1)));
    }
  }

  public Test suite()
  {
    TestSuite suite = new TestSuite();

    String[] script = new String[]
      {
        "attrib",
        "big",
        "calls",
        "checktable",
        "closure",
        "constructs",
        "events",
        "gc",
        "literals",
        "locals",
        "nextvar",
        "pm",
        "sort",
        "strings",
        "vararg",
      };

    // In order to test the compiler separately from the interpreter
    // all the tests are executed in both lua source and compiled form.
    for (int i=0; i<script.length; ++i)
    {
      suite.addTest(new AdvancedTest(script[i] + ".luc"));
      suite.addTest(new AdvancedTest(script[i] + ".lua"));
    }

    return suite;
  }
}
