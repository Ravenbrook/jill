// $Header$

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
