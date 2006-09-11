/*  $Header$
 *  (c) Copyright 2006, Intuwave Ltd. All Rights Reserved.
 *
 *  Although Intuwave has tested this program and reviewed the documentation,
 *  Intuwave makes no warranty or representation, either expressed or implied,
 *  with respect to this software, its quality, performance, merchantability,
 *  or fitness for a particular purpose. As a result, this software is licensed
 *  "AS-IS", and you are assuming the entire risk as to its quality and
 *  performance.
 *
 *  You are granted license to use this code as a basis for your own
 *  application(s) under the terms of the separate license between you and
 *  Intuwave.
 */

package mnj.lua;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Class used to implement internal callbacks.  Currently there is only
 * one callback used, one that parses or loads a Lua chunk into binary
 * form.
 */
final class LuaInternal extends LuaJavaCallback
{
  private InputStream stream;
  private Reader reader;
  private String chunkname;

  LuaInternal(InputStream in, String chunkname)
  {
    this.stream = in;
    this.chunkname = chunkname;
  }

  LuaInternal(Reader in, String chunkname)
  {
    this.reader = in;
    this.chunkname = chunkname;
  }

  public int luaFunction(Lua L)
  {
    try
    {
      Proto p = null;

      // In either the stream or the reader case there is a way of
      // converting the input to the other type.
      if (stream != null)
      {
        stream.mark(1);
        int c = stream.read();
        stream.reset();
        
        // Convert to Reader if looks like source code instead of
        // binary.
        if (c == Loader.HEADER[0])
        {
          Loader l = new Loader(stream, chunkname);
          p = l.undump();
        }
        else
        {
          reader = new InputStreamReader(stream, "UTF-8");
          p = Syntax.parser(L, reader, chunkname);
        }
      }
      else
      {
        // Convert to Stream if looks like binary (dumped via
        // string.dump) instead of source code.
        if (reader.markSupported())
        {
          reader.mark(1);
          int c = reader.read();
          reader.reset();

          if (c == Loader.HEADER[0])
          {
            stream = new FromReader(reader);
            Loader l = new Loader(stream, chunkname);
            p = l.undump();
          }
          else
          {
            p = Syntax.parser(L, reader, chunkname);
          }
        }
        else
        {
          p = Syntax.parser(L, reader, chunkname);
        }
      }

      L.push(new LuaFunction(p,
          new UpVal[0],
          L.getGlobals()));
      return 1;
    }
    catch (IOException e)
    {
      L.push("cannot read " + chunkname + ": " + e.toString());
      L.dThrow(Lua.ERRFILE);
      return 0;
    }
  }
}
