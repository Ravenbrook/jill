-- $Header$
-- Auxiliary file of Lua source for MetaTest.java
-- When you change this file, please compile it into MetaTest.luc and
-- submit both.

-- test __index for plain table case
function testmetaindex0()
  local t = {}
  local mt = {__index={foo='bar'}}
  setmetatable(t, mt)
  local a, b = t.a == nil, t.foo == 'bar'
  t.foo = 7
  local c = t.foo == 7
  t.foo = nil
  local d = t.foo == 'bar'
  return a,b,c,d
end
-- test __index for function case
function testmetaindex1()
  local t = {}
  local mt = {__index = function(t, k) return k*k end}
  setmetatable(t, mt)
  local a, b = t[3] == 9, t[7] == 49
  t[3] = 7
  local c = t[3] == 7
  t[3] = nil
  local d = t[3] == 9
  return a,b,c,d
end
-- Returns a function that when applied to a table returns new a new
-- table who (new) metatable has a field set to its original table
-- argument.  The field is variable and can be specified.
function newmeta(field)
  return function(x)
    local mt = {[field]=x}
    local t = {}
    setmetatable(t, mt)
    return t
  end
end
    
-- returns a new table whose (new) metatable has an __index value set to
-- argument
newmetaindex = newmeta('__index')
-- returns a new table whose (new) metatable has a __newindex values set
-- argument
newmetanewindex = newmeta('__newindex')
-- applies f to x n times, so returns f(f(f(...f(x)...)))
function niter(f, x, n)
  while n > 0 do
    x = f(x)
    n = n-1
  end
  return x
end
-- test __index for nested table case
function testmetaindex2()
  local deepindex = { foo='bar' }
  local t = niter(newmetaindex, deepindex, 20)
  local mt = getmetatable(t)
  local a = mt ~= getmetatable(mt.__index)
  local b = t.foo == 'bar'
  return a,b
end
-- test __newindex for table case
function testmetanewindex0()
  local t = {bar='spong'}
  local nit = {}
  local mt = { __newindex = nit }
  setmetatable(t, mt)
  t.foo = 'bar'
  t.bar = 'foo'
  local a,b = nit.foo == 'bar', t.foo == nil
  local c,d = nit.bar == nil, t.bar == 'foo'
  mt.__newindex = nil
  t.foo = 'spong'
  local e = t.foo == 'spong'
  return a,b,c,d,e
end
-- test __newindex for function case
function testmetanewindex1()
  local t = {}
  local f
  local update = {}
  do
    local i = 1
    f = function(t, k, v) update[i] = { [k] = v }; i=i+1; end
  end

  local mt = { __newindex = f }
  setmetatable(t, mt)
  t.foo = 'bar'
  t[3] = 7
  return t.foo == nil, t[3] == nil,
      update[1].foo == 'bar', update[2][3] == 7
end
-- test __newindex for nested table case
function testmetanewindex2()
  local deepnewindex = {}
  local t = niter(newmetanewindex, deepnewindex, 20)
  local mt = getmetatable(t)
  local a = mt ~= getmetatable(mt.__newindex)
  t.foo = 'bar'
  return t.foo == nil, deepnewindex.foo == 'bar', a
end
-- test __call
function testmetacall()
  local foocalled, beforef, afterf
  function around(f, g)
    local t = {}
    local mt = { __call = function()return g(f)end }
    setmetatable(t, mt)
    return t
  end
  function foo()foocalled = true end
  function a(f)beforef = true f() afterf = true end
  around(foo, a)()
  return foocalled, beforef, afterf
end
-- test __lt
function testmetalt()
  do
    local mt = { __lt = function(t1, t2) return #t1 < #t2 end }
    function addmt(t)
      setmetatable(t, mt)
      return t
    end
  end

  local a = addmt{}
  local b = addmt{'foo'}
  local c = addmt{'bar'}
  local d = addmt{a, b, c}

  local t = { a < b, b < a, b < c, c < b, c < d, d < c }  
  t[2] = not t[2]
  t[3] = not t[3]
  t[4] = not t[4]
  t[6] = not t[6]
  return unpack(t)
end
-- test __le
function testmetale()
  do
    local mt = { __le = function(t1, t2) return #t1 <= #t2 end }
    function addmt(t)
      setmetatable(t, mt)
      return t
    end
  end

  local a = addmt{}
  local b = addmt{'foo'}
  local c = addmt{'bar'}
  local d = addmt{a, b, c}

  local t = { a <= b, b <= a, b <= c, c <= b, c <= d, d <= c }  
  t[2] = not t[2]
  t[6] = not t[6]
  return unpack(t)
end
-- test __len
-- Assumes that numbers have been given a metatable and that this
-- metatable is accessible via getmetatable(0).
function testmetalen()
  local mt = getmetatable(0)
  mt.__len = function(x) return #(x..'') end
  return #3 == 1, #333 == 3, #1001 == 4
end
-- test __unm
function testmetaunm()
  local mt = { __unm = function(x) return x.unm end }
  local t = {}
  setmetatable(t, mt)
  local a = -t
  t.unm = 7
  local b = -t
  return a == nil, b == 7
end

-- create a metatable with lots of binary metamethods
do
  -- Adds a binary metamethod for the operator op, to the table t.
  local function mm(op, t)
    t['__'..op] = function(x, y) return x.x .. op .. y.x end
  end
  local mt = {}
  mm('add', mt)
  mm('sub', mt)
  mm('mul', mt)
  mm('div', mt)
  mm('mod', mt)
  mm('pow', mt)
  mm('concat', mt)
  binarymt = mt
end

local t = { x = 1 }
local u = { x = 2 }
setmetatable(t, binarymt)
setmetatable(u, binarymt)

-- test __add
function testmetaadd()
  return t+u == '1add2', u+t == '2add1'
end
-- test __sub
function testmetasub()
  return t-u == '1sub2', u-t == '2sub1'
end
-- test __mul
function testmetamul()
  return t*u == '1mul2', u*t == '2mul1'
end
-- test __div
function testmetadiv()
  return t/u == '1div2', u/t == '2div1'
end
-- test __mod
function testmetamod()
  return t%u == '1mod2', u%t == '2mod1'
end
-- test __pow
function testmetapow()
  return t^u == '1pow2', u^t == '2pow1'
end
-- test __concat
function testmetaconcat()
  return t..u == '1concat2', u..t == '2concat1'
end
-- test constant meta operands
do
  -- Adds a binary metamethod for the operator op, to the table t.
  local function mm(op, t)
    t['__'..op] = function(x, y) return{op,x,y}end
  end
  local mt = {}
  mm('add', mt)
  mm('sub', mt)
  mm('mul', mt)
  mm('div', mt)
  mm('mod', mt)
  mm('pow', mt)
  mm('concat', mt)
  otherbinarymt = mt
end
-- Indicative for Ravenbrook job001499
function testmetaconst()
  local t = {}
  setmetatable(t, otherbinarymt)
  local x
  x = t+7
  local a,b,c = x[1] == 'add', x[2] == t, x[3] == 7
  x = t/3
  local d,e,f = x[1] == 'div', x[2] == t, x[3] == 3
  x = t..'x'
  local g,h,i = x[1] == 'concat', x[2] == t, x[3] == 'x'
  return a,b,c,d,e,f,g,h,i
end
-- test __eq
function testmetaeq()
  local mt = { __eq = function(x, y) return #x == #y end }
  local t = {}
  local u = {'foo'}
  local v = {'bar'}
  setmetatable(t, mt)
  setmetatable(u, mt)
  setmetatable(v, mt)
  return t == t, t ~= u, u == v
end
