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

/*
** nodes for block list (list of active blocks)
*/
final class BlockCnt
{
  BlockCnt previous;  /* chain */
  int breaklist;      /* list of jumps out of this loop */
  int nactvar;        /* # active locals outside the breakable structure */
  boolean upval;      /* true if some variable in the block is an upvalue */
  boolean isbreakable;/* true if `block' is a loop */
}
