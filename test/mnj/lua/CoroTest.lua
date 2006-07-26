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

