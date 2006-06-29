#!/usr/bin/env python
# $Header$
# Python 2.3.3
# Script to move braces

import os
import re
import string
import sys

# See [JLS2] 3.9
javaKeyword = [
  'abstract',    'default',    'if',            'private',      'this',
  'boolean',     'do',         'implements',    'protected',    'throw',
  'break',       'double',     'import',        'public',       'throws',
  'byte',        'else',       'instanceof',    'return',       'transient',
  'case',        'extends',    'int',           'short',        'try',
  'catch',       'final',      'interface',     'static',       'void',
  'char',        'finally',    'long',          'strictfp',     'volatile',
  'class',       'float',      'native',        'super',        'while',
  'const',       'for',        'new',           'switch',
  'continue',    'goto',       'package',       'synchronized',
]

def javaKeywordP(x) :
  return x in javaKeyword

def doit(f, out) :
  '''Process input file object f and emit to output file object out.'''

  n = 1
  b = None      # line to use for column position of '{'
  methodDecl = False
  for l in f.xreadlines() :
    field = l.split()
    # Find and fix '} else ...' and similar
    if (re.search(r'^[\t ]*}', l) and
        len(field) > 1 and
        javaKeywordP(field[1]) and
        field[1] != 'while') :
      # :todo: split line and emit
      m = re.match(r'^([\t ]*)}[\t *](.*)$', l)
      out.write(m.group(1) + '}\n')
      newl = m.group(1) + m.group(2) + '\n'
      l = newl
      field = l.split()
    firstWord=''
    if len(field) > 0 :
      firstWord = re.search(r'^[a-zA-Z_0-9]*', field[0]).group()
    # We store a line in b (which will be used for making the white
    # space on the new line that has just '{').  We store the line if it
    # begins with a keyword or we thinking we are starting a method
    # declaration.
    # The RE for starting a method decl is
    # just matching exactly 2 spaces at the beginning of a line and is
    # very brittle.
    if javaKeywordP(firstWord) and not methodDecl :
      b = l
    if re.search(r'^  [^\t ]', l) :
      b = l
      methodDecl = True
    suffix = ''
    # Find a '{' that is not on a line of its own.  Any trailing
    # comments on the same line as '{' get preserved on their original
    # line.  Only the '{' moves to a new line of its own.
    if re.search(r'[^\t ].*{[\t ]*((//.*)|(/\*.*))?$', l) :
      m = re.search(r'^(.*){(.*?)$', l)
      l = m.group(1) + ' ' + m.group(2) + '\n'
      l = l.rstrip() + '\n'
      if b == None :
        b = l
      suffix = re.search(r'^[\t ]*', b).group() + '{\n'
      b = None
      methodDecl = False
    out.write(l)
    out.write(suffix)
    n += 1
    
def perfile(name) :
  outname = name + '.out'
  out = file(outname, 'w')
  doit(open(name), out)
  out.close()
  os.rename(outname, name)

def main() :
  for file in sys.argv[1:] :
    perfile(file)

main()
