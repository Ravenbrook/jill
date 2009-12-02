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

-- DO NOT INSERT ANYTHING ABOVE THIS LINE.  Because...
-- the function testerrormore relies on knowing its own line numbers.
-- (secret: you can insert stuff above the line, as long as you fix the
-- line number in this test function)
function testerrormore()
  local function f(x)
    if x ~= nil then
      error('spong', 1) -- this line number should appear in the output
    end
  end
  local a,b = pcall(function()f(6)end)
  print(b)
  return a==false, b=='BaseLibTest.lua:31: spong'
end
function testprint()
  print()
  print(7, 'foo', {}, nil, function()end, true, false, -0.0)
end
function testtostring()
  return '7' == tostring(7),
      'foo' == tostring'foo',
      'nil' == tostring(nil),
      'true' == tostring(true),
      'false' == tostring(false)
end
function testtonumber()
  return 1 == tonumber'1',
      nil == tonumber'',
      nil == tonumber{},
      nil == tonumber(false),
      -2.5 == tonumber'-2.5'
end
function testtype()
  return type(nil) == 'nil',
      type(1) == 'number',
      type'nil' == 'string',
      type{} == 'table',
      type(function()end) == 'function',
      type(type==type) == 'boolean'
end
function testselect()
  return select(2, 6, 7, 8) == 7,
      select('#', 6, 7, 8) == 3
end
function testunpack()
  a,b,c = unpack{'foo', 'bar', 'baz'}
  return a == 'foo', b == 'bar', c == 'baz'
end
function testpairs()
  local t = {'alderan', 'deneb', 'vega'}
  local u = {}
  local x = 0
  for k,v in pairs(t) do
    u[v] = true
    x = x + k
  end
  return x==6, u.alderan, u.deneb, u.vega
end
function testnext()
  local t = {'alderan', 'deneb', 'vega'}
  local u = {}
  local x = 0
  for k,v in next, t, nil do
    u[v] = true
    x = x + k
  end
  return x==6, u.alderan, u.deneb, u.vega
end
function testipairs()
  local t = {'a', 'b', 'c', foo = 'bar' }
  local u = {}
  for k,v in ipairs(t) do
    u[k] = v
  end
  return u[1]=='a', u[2]=='b', u[3]=='c', u.foo==nil
end
function testrawequal()
  local eq = rawequal
  return eq(nil, nil),
      eq(1, 1),
      eq('foo', "foo"),
      eq(true, true),
      not eq(nil, false),
      not eq({}, {}),
      not eq(1, 2)
end
function testrawget()
  local t = {a='foo'}
  return rawget(t, 'a')=='foo', rawget(t, 'foo')==nil
end
function testrawset()
  local t = {}
  rawset(t, 'b', 'bar')
  return t.b=='bar', t.bar==nil
end
function testgetfenv()
  return type(getfenv(type))=='table'
end
function testsetfenv()
  x='global'
  local function f()return function()return x end end
  local f1 = f()
  local f2 = f()
  local f3 = f()
  local a,b,c = (f1()=='global'), (f2()=='global'), (f3()=='global')
  setfenv(f2, {x='first'})
  setfenv(f3, {x='second'})
  local d,e,f = (f1()=='global'), (f2()=='first'), (f3()=='second')
  return a,b,c,d,e,f
end
function testpcall()
  return pcall(function()return true end)
end
function testerror()
  local a,b = pcall(function()error('spong',0)end)
  return a==false, b=='spong'
end
function testmetatable()
  local t,m={},{}
  local r1 = getmetatable(t)==nil
  setmetatable(t, m)
  return r1, getmetatable(t)==m
end
function test__metatable()
  local t,f,m={},{},{}
  m.__metatable=f
  setmetatable(t, m)
  return (pcall(function()setmetatable(t, m)end))==false,
    getmetatable(t)==f
end
function test__tostring()
  local t,m={},{}
  m.__tostring = function()return'spong'end
  setmetatable(t, m)
  return tostring(t)=='spong'
end
function testcollectgarbage() -- very weak test
  collectgarbage'collect'
  return type(collectgarbage'count') == 'number'
end
function testassert()
  local a,b = pcall(function()assert(false)end)
  local c,d = pcall(function()return assert(1)end)
  return a==false, type(b)=='string', c==true, d==1
end
function testloadstring()
  local f = loadstring'return 99'
  return f()==99
end
testloadfilename='BaseLibTestLoadfile.luc'
function testloadfile()
  local f = loadfile(testloadfilename)
  return f()==99
end
function loader(s) -- helper for testload
  return function()local x=s s=nil return x end
end
function testload()
  local f = load(loader'return 99')
  return f()==99
end
function testdofile()
  return dofile(testloadfilename)==99
end
function testxpcall()
  local function anerror()return {}..{}end
  local function seven()return 7 end
  local a,b = xpcall(anerror, nil)
  local c,d = xpcall(anerror, seven)
  local e,f = xpcall(seven, anerror)
  return a == false, c == false, d == 7, e == true, f == 7
end
function testpcall2()
  local a,b,c,d = pcall(pcall, function()return 1+{}end)
  return a == true, b == false, type(c) == 'string', d == nil
end
function testpcall3()
  local a,b = pcall(function()end)
  return a == true, b == nil
end
-- more of a test of error generation itself than of pcall
function testpcall4()
  local a,b = pcall(function()return 1>nil end)
  assert(a == false)
  assert(type(b) == 'string')
  return true
end
function testpcall5()
  local c,d = pcall(function()return pcall>_VERSION end)
  assert(c == false)
  assert(type(d) == 'string')
  return true
end
function testunpackbig()
  local a = {}
  for i = 1,2000 do
    a[i] = i
  end
  local x = {unpack(a)}
  return x[2000] == 2000
end
function testloaderr()
  local a,b = loadstring("'spong'", '')
  assert(a == nil)
  assert(type(b) == 'string')
  return true
end
-- Test using NaN as a table index.  It's here for entirely bogus
-- reasons of convenience.
function testnanindex()
  local t = {}
  local nan = 0/0
  assert(pcall(function()t[nan]=''end) == false)
  return true
end
-- Test that 'a99' + '11' is an error.
function testhexerror()
  assert(pcall(function()return'a99'+'11'end) == false)
  return true
end
