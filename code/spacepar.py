#!/usr/bin/env python
# $Header$
# Python 2.3.3
# Tool to analyse and correct all the errors of the form
# "'(' is preceded with whitespace" produced by the Checkstyle tool.
# it does this by processing the style.txt file output by Checkstyle.
# Currently falls over (with an assert) if a line of source has more
# than one violation.

import re
import sys
import fileinput

currentfilename = None
currentfileinput = None

def windtoend(fi) :
  '''Copies the remainder of fi to sys.stdout and then closes both
  streams.'''
  while True :
    l = fi.readline()
    if l == '' :
      break
    sys.stdout.write(l)
  sys.stdout.close()
  fi.close()

def getfileinput(name) :
  global currentfilename
  global currentfileinput
  if currentfilename == name :
    return currentfileinput
  if currentfileinput :
    windtoend(currentfileinput)
  currentfileinput = fileinput.input(name, inplace=1)
  currentfilename = name
  return currentfileinput

def doit(inp) :
  '''Take a file object as input.  The input is the text report produced
  by Checkstyle.  It is processed for violations.'''

  filename = None
  for l in inp.xreadlines() :
    if re.search(r"'\(' is preceded with whitespace", l) :
      m = re.search(r'^(.*?):(.*?):(.*?):', l)
      assert m != None
      filename = m.group(1)
      lineno = int(m.group(2))
      # The column number is, by inspection of a typical line from
      # style.txt, the column number of the '(' character, starting from
      # 1 being the leftmost column.
      columnno = int(m.group(3))
      fi = getfileinput(filename)
      while 1 :
        x = fi.readline()
        if fi.lineno() >= lineno :
          break
        sys.stdout.write(x)
      assert fi.lineno() == lineno, (filename + ' ' + str(fi.lineno()) +
          ' ' + str(lineno))
      old = x
      firstpart = x[0:columnno-1]
      lastpart = x[columnno-1:]
      firstpart = firstpart.rstrip()
      x = firstpart + lastpart
      sys.stdout.write(x)
  windtoend(getfileinput(filename))     # close the last one.

def main() :
  doit(sys.stdin)

main()
