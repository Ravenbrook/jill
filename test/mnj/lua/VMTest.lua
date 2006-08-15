-- $Header$
function testvarlist(...)
  return {...}
end

function testliferule()
  local x = 0
  for me = 0,1 do
    for neighbours = 0,8 do
      local v = ((neighbours==2) and me) or ((neighbours==3) and 1) or 0
      x = x*2 + v
    end
  end
  -- There are 18 executions of the inner statements in the above loops.
  -- For each execution v should be either 0 or 1.
  -- Numbering the executions from 0 to 17, v should be 1 just for
  -- executions with number: 3, 11, 12.
  -- Therefore x should be 2^14 + 2^6 + 2^5 = 16480
  return x
end

-- Test that nil can be used as a index when reading.
function testindexnil()
  local t = {1,2,3}
  return t[nil]
end
