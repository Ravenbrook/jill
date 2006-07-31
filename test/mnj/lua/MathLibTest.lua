-- $Header$
function test1()
  assert(type(math) == 'table')
  assert(math.max(3,2,1,5) == 5)
  assert(math.abs(math.cos(5)) < 1)
  assert(math.abs(math.cos(2)) > 0)
end

function test2()
  assert(math.pow(2, 2) > 2)
  assert(math.abs(math.rad(180) - math.pi) < .1)
  assert(math.tan(1) > 1)
end

function test3()
end
