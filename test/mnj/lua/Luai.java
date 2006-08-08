// $Header$
// Lua script interpreter for JSE environments.
// Analogous to the lua interpreter provided by PUC-Rio

package mnj.lua;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

public final class Luai
{
  public static void main(String[] arg)
  {
    try
    {
      String name = arg[0];
      InputStream in = new BufferedInputStream(new FileInputStream(name));

      Lua L = new Lua();
      BaseLib.open(L);
      PackageLib.open(L);
      MathLib.open(L);
      OSLib.open(L);
      StringLib.open(L);
      TableLib.open(L);

      int status = L.load(in, "@" + name);
      in.close();
      if (status != 0)
      {
        throw new Exception("Error compiling " + name + ": " +
            L.value(1));
      }
      status = L.pcall(0, Lua.MULTRET, new AddWhere());
      if (status != 0)
      {
        System.out.println(L.value(-1));
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
