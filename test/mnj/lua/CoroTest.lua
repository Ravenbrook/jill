-- $Header$
-- See CoroTest.java

function test4()
  v = 0
  coroutine.yield()
  v = 1
  coroutine.yield()
  v = 2
  coroutine.yield()
  v = 3
end

-- From [LUA 2006-03-26]
function test5()
  -- tests for multiple yield/resume arguments

  local function eqtab (t1, t2)
    assert(#t1 == #t2)
    for i,v in ipairs(t1) do
      assert(t2[i] == v)
    end
  end

  local f

  _G.x = nil   -- declare x
  local function foo (a, ...)
    assert(coroutine.running() == f)
    assert(coroutine.status(f) == "running")
    local arg = {...}
    for i=1,#arg do
      _G.x = {coroutine.yield(unpack(arg[i]))}
    end
    return unpack(a)
  end

  f = coroutine.create(foo)
  assert(type(f) == "thread" and coroutine.status(f) == "suspended")
  -- assert(string.find(tostring(f), "thread"))
  local s,a,b,c,d
  s,a,b,c,d = coroutine.resume(f, {1,2,3}, {}, {1}, {'a', 'b', 'c'})
  assert(s)
  assert(a == nil)
  assert(coroutine.status(f) == "suspended")
  s,a,b,c,d = coroutine.resume(f)
  eqtab(_G.x, {})
  assert(s and a == 1 and b == nil)
  s,a,b,c,d = coroutine.resume(f, 1, 2, 3)
  eqtab(_G.x, {1, 2, 3})
  assert(s and a == 'a' and b == 'b' and c == 'c' and d == nil)
  s,a,b,c,d = coroutine.resume(f, "xuxu")
  eqtab(_G.x, {"xuxu"})
  assert(s and a == 1 and b == 2 and c == 3 and d == nil)
  assert(coroutine.status(f) == "dead")
  s, a = coroutine.resume(f, "xuxu")
  assert(not s)
  assert(string.find(a, "dead"))
  assert(coroutine.status(f) == "dead")
  return true
end

-- From [LUA 2006-03-26]
function test6()
  -- yields in tail calls
  local function foo (i) return coroutine.yield(i) end
  f = coroutine.wrap(function ()
    for i=1,10 do
      assert(foo(i) == _G.x)
    end
    return 'a'
  end)
  for i=1,10 do _G.x = i; assert(f(i) == i) end
  _G.x = 'xuxu'; assert(f('xuxu') == 'a')
  return true
end

-- From [LUA 2006-03-26]
function test7()
  -- recursive
  function pf (n, i)
    coroutine.yield(n)
    pf(n*i, i+1)
  end

  f = coroutine.wrap(pf)
  local s=1
  for i=1,10 do
    assert(f(1, 1) == s)
    s = s*i
  end
  return true
end

-- From [LUA 2006-03-26]
function test8()
  -- sieve
  function gen (n)
    return coroutine.wrap(function ()
      for i=2,n do coroutine.yield(i) end
    end)
  end


  function filter (p, g)
    return coroutine.wrap(function ()
      while 1 do
        local n = g()
        if n == nil then return end
        if n % p ~= 0 then coroutine.yield(n) end
      end
    end)
  end

  local x = gen(100)
  local a = {}
  while 1 do
    local n = x()
    if n == nil then break end
    table.insert(a, n)
    x = filter(n, x)
  end

  assert(#a == 25 and a[#a] == 97)

  return true
end

-- test that errors inside coroutines appear properly.
function test9()
  local function f() error'foobar' end
  local t = coroutine.create(f)
  local a,b = coroutine.resume(t)
  assert(a == false)
  assert(type(b) == 'string')
  assert(string.find(b, 'foobar'))
  return true
end

-- test that yielding is possible by throwing exception.
function test10(a, b)
  local sa,sb = 0,0
  for i=1,4 do
    sa = sa + a(i)
    sb = sb + b(i)
  end
  return sa, sb
end
