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

import java.io.IOException;

/**
 * Extends {@link java.io.Reader} to create a Reader from a Lua
 * function.  So that the <code>load</code> function from Lua's base
 * library can be implemented.
 */
final class BaseLibReader extends java.io.Reader
{
  private String s = "";
  private int i;        // = 0;
  private int mark = -1;
  private Lua L;
  private Object f;

  BaseLibReader(Lua L, Object f)
  {
    this.L = L;
    this.f = f;
  }

  public void close()
  {
    f = null;
  }

  public void mark(int l) throws IOException
  {
    if (l > 1)
    {
      throw new IOException("Readahead must be <= 1");
    }
    mark = i;
  }

  public int read()
  {
    if (i >= s.length())
    {
      L.push(f);
      L.call(0, 1);
      if (L.isNil(L.value(-1)))
      {
        return -1;
      }
      else if(L.isString(L.value(-1)))
      {
        s = L.toString(L.value(-1));
        if (s.length() == 0)
        {
          return -1;
        }
        if (mark == i)
        {
          mark = 0;
        }
        else
        {
          mark = -1;
        }
        i = 0;
      }
      else
      {
        L.error("reader function must return a string");
      }
    }
    return s.charAt(i++);
  }

  public int read(char[] cbuf, int off, int len)
  {
    int j = 0;  // loop index required after loop
    for (j=0; j<len; ++j)
    {
      int c = read();
      if (c == -1)
      {
        if (j == 0)
        {
          return -1;
        }
        else
        {
          return j;
        }
      }
      cbuf[off+j] = (char)c;
    }
    return j;
  }

  public void reset() throws IOException
  {
    if (mark < 0)
    {
      throw new IOException("reset() not supported now");
    }
    i = mark;
  }
}
