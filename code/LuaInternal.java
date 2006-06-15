// $Header$

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Class used to implement internal callbacks.  Currently there is only
 * one callback used, one that parses or loads a Lua chunk into binary
 * form.
 */
final class LuaInternal extends LuaJavaCallback {
  private InputStream stream;
  private Reader reader;
  private String chunkname;

  LuaInternal(InputStream in, String chunkname) {
    this.stream = in;
    this.chunkname = chunkname;
  }

  LuaInternal(Reader in, String chunkname) {
    this.reader = in;
    this.chunkname = chunkname;
  }

  public int luaFunction(Lua L) {
    try {
      Proto p = null;
      int c = -1;

      // The following logic is a bit tricky.  If there is a reader
      // defined, then use that for compilation from source, otherwise
      // (there must be a stream defined) try loading as binary, then
      // compiling from source.

      if (stream != null) {
        // :todo: consider using markSupported
        stream.mark(1);
        c = stream.read();
        stream.reset();
      }

      if (c == Loader.goldenHeader[0]) {
        // assert stream != null
        Loader l = new Loader(stream, chunkname);
        p = l.undump();
      } else {
        if (reader == null) {
          reader = new InputStreamReader(stream);
        }
        p = Syntax.parser(L, reader, chunkname);
      }
      L.push(new LuaFunction(p,
          new UpVal[0],
          L.getGlobals()));
      return 1;
    } catch (IOException e) {
      L.push("cannot read " + chunkname + ": " + e.toString());
      L.dThrow(Lua.ERRFILE);
    }
    return 0;
  }
}
