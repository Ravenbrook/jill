-- $Header$
-- Copyright (c) 2006 Nokia Corporation and/or its subsidiary(-ies).
-- All rights reserved.
-- 
-- Permission is hereby granted, free of charge, to any person obtaining
-- a copy of this software and associated documentation files (the
-- "Software"), to deal in the Software without restriction, including
-- without limitation the rights to use, copy, modify, merge, publish,
-- distribute, sublicense, and/or sell copies of the Software, and to
-- permit persons to whom the Software is furnished to do so, subject
-- to the following conditions:
-- 
-- The above copyright notice and this permission notice shall be
-- included in all copies or substantial portions of the Software.
-- 
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
-- EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
-- MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
-- IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
-- ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
-- CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
-- WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

function test1()
  assert(require'PackageLibTest' == 7)
end

function foof()
  module'foo'
end

function test2()
  foof()
  assert(type(foo) == 'table')
end

local DIR = "libs/"


local files
local oldpath

function test3()
  AA = nil

  oldpath = package.path

  package.path = string.gsub("D/?.lua;D/?.lc;D/?;D/??x?;D/L", "D/", DIR)

  local try = function (p, n, r)
    NAME = nil
    local rr = require(p)
    assert(NAME == n)
    assert(REQUIRED == p)
    assert(rr == r)
  end

  assert(require"C" == 25)
  assert(require"C" == 25)
  AA = nil
  try('B', 'B.lua', true)
  assert(package.loaded.B)
  assert(require"B" == true)
  assert(package.loaded.A)
  package.loaded.A = nil
  try('B', nil, true)   -- should not reload package
  try('A', 'A.lua', true)
  package.loaded.A = nil
  --os.remove(DIR..'A.lua')
  AA = {}
  --try('A', 'A.lc', AA)  -- now must find second option
  assert(require("A") == AA)
  AA = false
  try('K', 'L', false)     -- default option
  try('K', 'L', false)     -- default option (should reload it)
  assert(rawget(_G, "_REQUIREDNAME") == nil)

  AA = "x"
  try("X", "XXxX", AA)
  package.path = oldpath
end


function test4()
  -- testing require of sub-packages

  oldpath = package.path
  package.path = string.gsub("D/?.lua;D/?/init.lua", "D/", DIR)

  AA = 0

  local m = assert(require"P1")
  assert(m == P1 and m._NAME == "P1" and AA == 0 and m.AA == 10)
  assert(require"P1" == P1 and P1 == m)
  assert(require"P1" == P1)
  assert(P1._PACKAGE == "")

  local m = assert(require"P1.xuxu")
  assert(m == P1.xuxu and m._NAME == "P1.xuxu" and AA == 0 and m.AA == 20)
  assert(require"P1.xuxu" == P1.xuxu and P1.xuxu == m)
  assert(require"P1.xuxu" == P1.xuxu)
  assert(require"P1" == P1)
  assert(P1.xuxu._PACKAGE == "P1.")
  assert(P1.AA == 10 and P1._PACKAGE == "")
  assert(P1._G == _G and P1.xuxu._G == _G)
  package.path = oldpath
end


function test5()
  oldpath = package.path
  package.path = ""
  assert(not pcall(require, "file_does_not_exist"))
  package.path = "??\0?"
  assert(not pcall(require, "file_does_not_exist1"))

  package.path = oldpath
end

function test6()
  -- check 'require' error message
  local fname = "file_does_not_exist2"
  local m, err = pcall(require, fname)
  for t in string.gmatch(package.path, "[^;]+") do
    t = string.gsub(t, "?", fname)
    assert(string.find(err, t, 1, true))
  end
end


function test7()
  local function import(...)
    local f = {...}
    return function (m)
      for i=1, #f do m[f[i]] = _G[f[i]] end
    end
  end

  local assert, module, package = assert, module, package
  X = nil; x = 0; assert(_G.x == 0)   -- `x' must be a global variable
  module"X"; x = 1; assert(_M.x == 1)
  module"X.a.b.c"; x = 2; assert(_M.x == 2)
  module("X.a.b", package.seeall); x = 3
  assert(X._NAME == "X" and X.a.b.c._NAME == "X.a.b.c" and X.a.b._NAME == "X.a.b")
  assert(X._M == X and X.a.b.c._M == X.a.b.c and X.a.b._M == X.a.b)
  assert(X.x == 1 and X.a.b.c.x == 2 and X.a.b.x == 3)
  assert(X._PACKAGE == "" and X.a.b.c._PACKAGE == "X.a.b." and
         X.a.b._PACKAGE == "X.a.")
  assert(_PACKAGE.."c" == "X.a.c")
  assert(X.a._NAME == nil and X.a._M == nil)
  module("X.a", import("X")) ; x = 4
  assert(X.a._NAME == "X.a" and X.a.x == 4 and X.a._M == X.a)
  module("X.a.b", package.seeall); assert(x == 3); x = 5
  assert(_NAME == "X.a.b" and X.a.b.x == 5)

  assert(X._G == nil and X.a._G == nil and X.a.b._G == _G and X.a.b.c._G == nil)
end

-- setfenv(1, _G)

function test8()
  x = 1
  assert(not pcall(module, "x"))
  assert(not pcall(module, "math.sin"))
end

function test9()
  -- testing preload
  local p = package
  package = {}
  p.preload.pl = function (...)
    module(...)
    function xuxu (x) return x+20 end
  end

  require"pl"
  assert(require"pl" == pl)
  assert(pl.xuxu(10) == 30)

  package = p
  assert(type(package.path) == "string")
end

function test10()
  assert(type(package.path) == "string")
  assert(type(package.loaded) == "table")
  assert(type(package.preload) == "table")
end

function test11()
  assert(require"string" == string)
  assert(require"math" == math)
  assert(require"table" == table)
  -- assert(require"io" == io)
  assert(require"os" == os)
  -- assert(require"debug" == debug)
  assert(require"coroutine" == coroutine)
end

return 7
