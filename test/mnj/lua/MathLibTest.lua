-- $Header$
function test1()
  assert(type(math) == 'table')
  assert(math.abs(math.cos(5)) < 1)
  assert(math.abs(math.cos(2)) > 0)
  assert(math.sin(2) > 0)
  assert(math.sin(5) < 0)
  assert(math.tan(1) > 1)

  assert(math.abs(math.rad(180) - math.pi) < .1)
  assert(math.abs(math.deg(math.pi/2) - 90) < 1)
end

function test2()
  assert(math.ceil(0) == 0)
  assert(math.ceil(math.pi) == 4)
  assert(math.ceil(-1.2) == -1)
  assert(math.floor(0) == 0)
  assert(math.floor(math.pi) == 3)
  assert(math.floor(-1.2) == -2)

  assert(math.huge > 1e6)
  assert(math.fmod(3.5, 2) == 1.5)
  local a,b = math.modf(3.5)
  assert(a == 3)
  assert(b == 0.5)

  assert(math.max(3,2,1,5) == 5)
  assert(math.min(3,2,1,5) == 1)
end

function test3()
  assert(math.pow(2, 2) > 2)
  -- 0.693... is the natural logarithm of 2.
  assert(math.abs(math.exp(0.69314718055994529) - 2) < 0.1)
end
