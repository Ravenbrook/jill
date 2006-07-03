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


import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

// these for compiler_debug only:
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.File;

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
      int c = -1;

      // The following logic is a bit tricky.  If there is a reader
      // defined, then use that for compilation from source, otherwise
      // (there must be a stream defined) try loading as binary, then
      // compiling from source.

      if (stream != null)
      {
        // :todo: consider using markSupported
        stream.mark(1);
        c = stream.read();
        stream.reset();
      }

      if (c == Loader.HEADER[0])
      {
        // assert stream != null
        Loader l = new Loader(stream, chunkname);
        p = l.undump();
      }
      else
      {
        if (reader == null)
        {
          try { reader = new InputStreamReader(stream, "UTF-8"); }
          catch (UnsupportedEncodingException uee) {}
        }
        p = Syntax.parser(L, reader, chunkname);
        debug_compiler (L, p, false) ; // :todo: take this out
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
    }
    return 0;
  }


  // :todo: take this out
  static int seq = 0 ;
  static void debug_compiler (Lua L, Proto p, boolean strip)
  {

    OutputStream out = null ;
    try
    {
      out = new FileOutputStream (new File ("compiler.chunk"+(seq++)+".luc")) ;
 p.source = "@"+p.source ;
      L.uDump (p, out, strip) ;
    }
    catch (Exception e)
    {
      System.out.println ("Problem dumping compiler chunk to compiler.chunk.output "+e.getMessage()) ;
    }
    finally
    {
      if (out != null)
      { try { out.close () ; } catch (IOException io) {}}
    }

  }
}
