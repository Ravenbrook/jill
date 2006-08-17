// $Header$

package mnj.lua;

public final class Speed
{
  public static void main(String[] arg)
  {

    String[] script =  {
        "fannkuch",
        "nbody",
        "nsieve",
        "partialsums",
        "recursive",
        "spectralnorm",
    };

    long t[] = new long[script.length];

    for (int i=0; i < script.length; ++i)
    {
      t[i] = time(script[i]);
    }

    long total = 0;
    for (int i=0; i < script.length; ++i)
    {
      total += t[i];
      System.out.println(script[i] + ": " + t[i]);
    }
    System.out.println("Total: " + total);
  }

  /**
   * @return execution time in milliseconds.
   */
  private static long time(String name)
  {
    Lua L = new Lua();
    BaseLib.open(L);
    PackageLib.open(L);
    MathLib.open(L);
    OSLib.open(L);
    StringLib.open(L);
    TableLib.open(L);

    L.loadFile("speed/" + name + ".lua");
    long t0 = System.currentTimeMillis();
    int status = L.pcall(0, 0, new AddWhere());
    if (status != 0)
    {
      System.out.println(L.value(-1));
    }
    long t1 = System.currentTimeMillis();
    return t1-t0;
  }
}
