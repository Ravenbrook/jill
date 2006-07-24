/*  $Header$
 *  (c) Copyright 2002-2006, Intuwave Ltd. All Rights Reserved.
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

/**
 * Converts a string obtained using string.dump into an
 * {@link java.io.InputStream} so that it can be passed to {@link
 * Lua#load(java.io.InputStream, java.lang.String)}.
 */
final class DumpedInput extends InputStream
{
  private String s;
  private int i;        // = 0
  int mark = -1;

  DumpedInput(String s)
  {
    this.s = s;
  }

  public int available()
  {
    return s.length() - i;
  }

  public void close()
  {
    s = null;
    i = -1;
  }

  public void mark(int readlimit)
  {
    mark = i;
  }

  public boolean markSupported()
  {
    return true;
  }

  public int read()
  {
    if (i >= s.length())
    {
      return -1;
    }
    char c = s.charAt(i);
    ++i;
    return c&0xff;
  }

  public void reset()
  {
    i = mark;
  }
}
