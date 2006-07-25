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
