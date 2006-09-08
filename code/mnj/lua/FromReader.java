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
import java.io.IOException;
import java.io.Reader;

/**
 * Takes a {@link Reader} and converts to an {@link InputStream} by
 * reversing the transformation performed by <code>string.dump</code>.
 * Similar to {@link DumpedInput} which does the same job for {@link
 * String}.  This class is used by {@link BaseLib}'s load in order to
 * load binary chunks.
 */
final class FromReader extends InputStream
{
  // :todo: consider combining with DumpedInput.  No real reason except
  // to save space in JME.

  private Reader reader;

  FromReader(Reader reader)
  {
    this.reader = reader;
  }

  public void mark(int readahead)
  {
    try
    {
      reader.mark(readahead);
    }
    catch (Exception e_)
    {
    }
  }

  public void reset() throws IOException
  {
    reader.reset();
  }

  public int read() throws IOException
  {
    int c = reader.read();
    if (c == -1)
    {
      return c;
    }
    return c & 0xff;
  }
}
