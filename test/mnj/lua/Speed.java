// $Header$
// Copyright (c) 2006 Nokia Corporation and/or its subsidiary(-ies).
// All rights reserved.
// 
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject
// to the following conditions:
// 
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
// ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
// CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

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
