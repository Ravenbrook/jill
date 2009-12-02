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
// The purpose of this test is to determine whether enforcing the
// Checkstyle ParameterAssignment rule increases class file size.

// The "test" is currently executed by hand.
// - compile this java file into class files.
// - compare the size of C1.class and C2.class.  If they are a different
//   size (assume C2 is bigger) then avoiding assigning to parameters
//   affects the size of the class file.
// - run the classfiles through ProGuard.  On Mac OS X the
//   ParameterAssignTest.pro file can be used, on other platforms the
//   -libraryjars option will need changing.
// - compare the size of the two class files.  Use the .map file to see
//   which obfuscated class file is which plain class file.

final class C1
{
  static int sum(int n)
  {
    int x = 0;
    for (int i=n; i>=0; --i)
    {
      x += i;
    }
    return x;
  }
}

final class C2
  {
  static int sum(int n)
  {
    int x = 0;
    for (; n >= 0; --n)
    {
      x += n;
    }
    return x;
  }
}

public final class ParameterAssignTest
  {
  public static void main(String[] arg)
  {
    System.out.println(C1.sum(10));
    System.out.println(C2.sum(10));
  }
}
