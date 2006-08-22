// $Header$

package mnj.lua;

/**
 */
public final class HookTest implements Hook
{
  public static void main(String[] arg)
  {
    trial();
    preempt();
  }

  static void trial()
  {
    Lua L = lua();

    int status = L.loadFile("speed/fannkuch.lua");
    if (status != 0)
    {
      System.out.println(L.value(-1));
    }
    Object script = L.value(-1);
    HookTest hook = new HookTest();
    L.setHook(hook, Lua.MASKCOUNT, 100);
    status = L.pcall(0, 0, new AddWhere());
    if (status != 0)
    {
      System.out.println(L.value(-1));
    }
    System.out.println("Hook called: " + hook.n + " times");

    hook = new HookTest(true);
    L.setHook(hook, Lua.MASKCOUNT, 1000);
    int x = 0;
    while (true)
    {
      L.setTop(0);
      L.push(script);
      status = L.resume(0);
      ++x;
      if (status != Lua.YIELD)
      {
        break;
      }
    }
    if (status != 0)
    {
      System.out.println(L.value(-1));
    }
    System.out.println("n: " + hook.n + " x: " + x);
  }

  static void preempt()
  {
    String[] script = Speed.script;
    int n = script.length;
    boolean[] finished = new boolean[n];

    Lua[] l = new Lua[n];

    for (int i=0; i<n; ++i)
    {
      l[i] = lua();
      l[i].loadFile("speed/" + script[i] + ".lua");
      l[i].setHook(new HookTest(true), Lua.MASKCOUNT, 999);
    }

    while (true)
    {
      boolean allFinished = true;
      for (int i=0; i<n; ++i)
      {
        Lua L = l[i];
        if (!finished[i])
        {
          allFinished = false;
        }
        if (finished[i])
        {
          continue;
        }
        System.out.print(i);
        int status = L.resume(0);
        if (status != Lua.YIELD)
        {
          finished[i] = true;
          System.out.println("Script " + script[i] +
              " finished.  Status: " +
              status);
        }
      } /* for */
      if (allFinished)
      {
        break;
      }
    } /* while (true) */
    System.out.println("All finished");
  }

  private static Lua lua()
  {
    Lua L = new Lua();
    BaseLib.open(L);
    MathLib.open(L);
    OSLib.open(L);
    StringLib.open(L);
    TableLib.open(L);

    return L;
  }
    

  int n;        // = 0
  boolean yield;        // = false

  public HookTest()
  {
  }

  public HookTest(boolean yield)
  {
    this.yield = yield;
  }

  public int luaHook(Lua L, Debug ar)
  {
    ++n;
    if (yield)
    {
      return L.yield(0);
    }
    return 0;
  }
}
