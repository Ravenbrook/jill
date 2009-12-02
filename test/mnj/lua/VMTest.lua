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

-- Test #t a bit harder.
function testlentable(N)
  N = N or 999
  local t={}
  for i=1,N do
    t[i]=i
  end
  for i=10,N do
    t[i] = nil
  end
  return #t
end

-- Test table rehashing
function testrehash(N)
  local a = {}
  N = N or 99
  for i=N,-N,-1 do a[i] = i; end
  a[10e30] = "alo"; a[true] = 10; a[false] = 20
  for i=N,-N,-1 do
    if a[i] ~= i then
      return i
    end
  end
  return true
end
