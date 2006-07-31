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

import java.util.Random;

/**
 * Contains Lua's math library.
 * The library can be opened using the {@link MathLib#open} method.
 * Because this library is implemented on top of CLDC 1.1 it is not as
 * complete as the PUC-Rio math library.  Trigononmetric inverses
 * (EG <code>acos</code>) and hyperbolic trigonometric functions (EG
 * <code>cosh</code>) are not provided.
 */
public final class MathLib extends LuaJavaCallback
{
  // Each function in the library corresponds to an instance of
  // this class which is associated (the 'which' member) with an integer
  // which is unique within this class.  They are taken from the following
  // set.

  private static final int ABS = 1;
  //private static final int acos = 2;
  //private static final int asin = 3;
  //private static final int atan2 = 4;
  //private static final int atan = 5;
  private static final int CEIL = 6;
  //private static final int cosh = 7;
  private static final int COS = 8;
  private static final int DEG = 9;
  private static final int EXP = 10;
  private static final int FLOOR = 11;
  private static final int FMOD = 12;
  //private static final int frexp = 13;
  //private static final int ldexp = 14;
  //private static final int log = 15;
  private static final int MAX = 16;
  private static final int MIN = 17;
  private static final int MODF = 18;
  private static final int POW = 19;
  private static final int RAD = 20;
  private static final int RANDOM = 21;
  private static final int RANDOMSEED = 22;
  //private static final int sinh = 23;
  private static final int SIN = 24;
  private static final int SQRT = 25;
  //private static final int tanh = 26;
  private static final int TAN = 27;

  private static final Random rng = new Random();

  /**
   * Which library function this object represents.  This value should
   * be one of the "enums" defined in the class.
   */
  private int which;

  /** Constructs instance, filling in the 'which' member. */
  private MathLib(int which)
  {
    this.which = which;
  }

  /**
   * Implements all of the functions in the Lua math library.  Do not
   * call directly.
   * @param L  the Lua state in which to execute.
   * @return number of returned parameters, as per convention.
   */
  public int luaFunction(Lua L)
  {
    switch (which)
    {
      case ABS:
        return abs(L);
      case CEIL:
        return ceil(L);
      case COS:
        return cos(L);
      case DEG:
        return deg(L);
      case EXP:
        return exp(L);
      case FLOOR:
        return floor(L);
      case FMOD:
        return fmod(L);
      case MAX:
        return max(L);
      case MIN:
        return min(L);
      case MODF:
        return modf(L);
      case POW:
        return pow(L);
      case RAD:
        return rad(L);
      case RANDOM:
        return random(L);
      case RANDOMSEED:
        return randomseed(L);
      case SIN:
        return sin(L);
      case SQRT:
        return sqrt(L);
      case TAN:
        return tan(L);
    }
    return 0;
  }

  /**
   * Opens the library into the given Lua state.  This registers
   * the symbols of the library in the global table.
   * @param L  The Lua state into which to open.
   */
  public static void open(Lua L)
  {

    LuaTable t = L.newTable();
    L.setGlobal("math", t);

    r(L, "abs", ABS);
    r(L, "ceil", CEIL);
    r(L, "cos", COS);
    r(L, "deg", DEG);
    r(L, "exp", EXP);
    r(L, "floor", FLOOR);
    r(L, "fmod", FMOD);
    r(L, "max", MAX);
    r(L, "min", MIN);
    r(L, "modf", MODF);
    r(L, "pow", POW);
    r(L, "rad", RAD);
    r(L, "random", RANDOM);
    r(L, "randomseed", RANDOMSEED);
    r(L, "sin", SIN);
    r(L, "sqrt", SQRT);
    r(L, "tan", TAN);

  }

  /** Register a function. */
  private static void r(Lua L, String name, int which)
  {
    MathLib f = new MathLib(which);
    L.setField(L.getGlobal("math"), name, f);
  }

  private static int abs(Lua L)
  {
    L.pushNumber(Math.abs(L.checkNumber(1)));
    return 1;
  }

  private static int ceil(Lua L)
  {
    L.pushNumber(Math.ceil(L.checkNumber(1)));
    return 1;
  }

  private static int cos(Lua L)
  {
    L.pushNumber(Math.cos(L.checkNumber(1)));
    return 1;
  }

  private static int deg(Lua L)
  {
    L.pushNumber(Math.toDegrees(L.checkNumber(1)));
    return 1;
  }

  private static int exp(Lua L)
  {
    // CLDC 1.1 has Math.E but no exp, pow, or log.  Bizarre.
    L.pushNumber(Lua.iNumpow(Math.E, L.checkNumber(1)));
    return 1;
  }

  private static int floor(Lua L)
  {
    L.pushNumber(Math.floor(L.checkNumber(1)));
    return 1;
  }

  private static int fmod(Lua L)
  {
    L.pushNumber(L.checkNumber(1) % L.checkNumber(2));
    return 1;
  }

  private static int max(Lua L)
  {
    int n = L.getTop(); // number of arguments
    double dmax = L.checkNumber(1);
    for (int i=2; i<=n; ++i)
    {
      double d = L.checkNumber(i);
      dmax = Math.max(dmax, d);
    }
    L.pushNumber(dmax);
    return 1;
  }

  private static int min(Lua L)
  {
    int n = L.getTop(); // number of arguments
    double dmin = L.checkNumber(1);
    for (int i=2; i<=n; ++i)
    {
      double d = L.checkNumber(i);
      dmin = Math.min(dmin, d);
    }
    L.pushNumber(dmin);
    return 1;
  }

  private static int modf(Lua L)
  {
    double x = L.checkNumber(1);
    double fp = x % 1;
    double ip = x - fp;
    L.pushNumber(ip);
    L.pushNumber(fp);
    return 2;
  }

  private static int pow(Lua L)
  {
    L.pushNumber(Lua.iNumpow(L.checkNumber(1), L.checkNumber(2)));
    return 1;
  }

  private static int rad(Lua L)
  {
    L.pushNumber(Math.toRadians(L.checkNumber(1)));
    return 1;
  }

  private static int random(Lua L)
  {
    // It would seem better style to associate the java.util.Random
    // instance with the Lua instance (by implementing and using a
    // registry for example).  However, PUC-rio uses the ISO C library
    // and so will share the same random number generator across all Lua
    // states.  So we do too.
    switch (L.getTop()) // check number of arguments
    {
      case 0:   // no arguments
        L.pushNumber(rng.nextDouble());
        break;
      
      case 1:   // only upper limit
        {
          int u = L.checkInt(1);
          L.argCheck(1<=u, 1, "interval is empty");
          L.pushNumber(rng.nextInt(u) + 1);
        }
        break;

      case 2:   // lower and upper limits
        {
          int l = L.checkInt(1);
          int u = L.checkInt(2);
          L.argCheck(l<=u, 2, "interval is empty");
          L.pushNumber(rng.nextInt(u) + l);
        }
        break;

      default:
        return L.error("wrong number of arguments");
    }
    return 1;
  }

  private static int randomseed(Lua L)
  {
    rng.setSeed((long)L.checkNumber(1));
    return 0;
  }

  private static int sin(Lua L)
  {
    L.pushNumber(Math.sin(L.checkNumber(1)));
    return 1;
  }

  private static int sqrt(Lua L)
  {
    L.pushNumber(Math.sqrt(L.checkNumber(1)));
    return 1;
  }

  private static int tan(Lua L)
  {
    L.pushNumber(Math.tan(L.checkNumber(1)));
    return 1;
  }
}
