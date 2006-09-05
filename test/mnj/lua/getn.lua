-- $Header$
-- Test that emphasises the use of getn (as used by table.insert and the
-- # operator).  Primarily used to measure speed optimisations.
local N = ... or 9999
local t = {}
local x = 0
local t0 = os.clock()
for i = 1,N do
  table.insert(t, i)
  x = x + #t
end
local t1 = os.clock()
print('getn', t1-t0)
