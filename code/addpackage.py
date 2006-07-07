#!/usr/bin/env python
# $Header$
# Created using python 2.3.3 documentation as reference.
# Add package declaration to java files

import fileinput
import sys

def doit(name) :
  f = fileinput.input(name, inplace=1)
  packaged = False
  while True :
    l = f.readline()
    if l == '' :
      break
    sys.stdout.write(l)
    if not packaged and l == '\n' :
      sys.stdout.write('package mnj.lua;\n')
      packaged = True
  f.close()

def main() :
  for name in sys.argv[1:] :
    doit(name)

main()
