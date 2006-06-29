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


/**
 * Common superclass for all Lua Java Functions.  A Lua function that
 * is implemented in Java is called a Lua Java Function.  Each such
 * function corresponds to an indirect instance of this class.  If you
 * wish to implement your own Lua Java Function then you'll need to
 * subclass this class and have one instance for each function that you
 * need.  It is recommended that you extend the class with at least one
 * member so that you can distinguish the different instances.  Whilst
 * it is possible to implement each different Lua Java Function by
 * having a new subclass for each one, this is not recommended as it
 * will increase the size of the resulting <code>.jar</code> file by a
 * large amount.
 */
public abstract class LuaJavaCallback
{
  abstract int luaFunction(Lua L);
}
