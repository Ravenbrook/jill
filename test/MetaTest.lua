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
