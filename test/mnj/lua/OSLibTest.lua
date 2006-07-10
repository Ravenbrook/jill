-- $Header$
-- See OSLib.java

function testclock()
  local t = os.clock()
  return type(t) == "number", t >= 0
end
function testdate()
  local d = os.date()
  return type(d) == 'string'
end
