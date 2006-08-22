// $Header$

package mnj.lua;

/**
 * Common speed measuring code used in JSE (by invoking the {@link
 * Speed#main} method directly and in JME by the {@link SpeedMIDlet}
 * class.
 */
final class Speed
{
  public static void main(String[] arg)
  {
    System.out.println(Speed.report());
  }

  static final String[] script = {
        "fannkuch",
        "nbody",
        "nsieve",
        "partialsums",
        "recursive",
        "spectralnorm",
    };

  static String report()
  {
    long t[] = new long[script.length];

    for (int i=0; i < script.length; ++i)
    {
      t[i] = time(script[i]);
    }

    StringBuffer b = new StringBuffer();

    long total = 0;
    for (int i=0; i < script.length; ++i)
    {
      total += t[i];
      b.append(script[i]);
      b.append(": ");
      b.append(t[i]);
      b.append("\n");
    }
    b.append("Total: ");
    b.append(total);
    b.append("\n");

    return b.toString();
  }

  /**
   * @return execution time in milliseconds.
   */
  static long time(String name)
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
