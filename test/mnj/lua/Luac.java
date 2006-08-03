// $Header$
// Lua script compiler for JSE environments.
// Analogous to the luac compiler provided by PUC-Rio

package mnj.lua;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class Luac
{
  public static void main(String[] arg)
  {
    try
    {
      String name = arg[0];
      InputStream in = new BufferedInputStream(new FileInputStream(name));
      Lua L = new Lua();
      int status = L.load(in, "@" + name);
      in.close();
      if (status != 0)
      {
        throw new Exception("Error compiling " + name + ": " +
            L.value(1));
      }
      String outname = name.replaceAll("\\.lua$", ".lc");
      if (outname.equals(name))
      {
        outname += ".lc";
      }
      OutputStream out = new FileOutputStream(outname);
      L.dump(L.value(1), out);
      out.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
