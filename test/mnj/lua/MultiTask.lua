-- $Header$
-- See MultiTask.java which uses this script (to illustrate multi-tasking)

-- This script excepts to receive two arguments:
-- The first argument is a thread identifier,
-- the second argument is used to create a port (passed to mkport).
-- This script uses the read and mkport functions defined by the
-- MultiTask.java host code.

local id, p = ...

print("Script: " .. id)
local port = mkport(p)

local a = '' -- accumulates all the strings returned by read()
while true do
  local s = read(port)
  if s == '' then
    break
  end
  a = a .. s
  print("Script: " .. id .. string.rep(' ', 8*id) .. " got [" .. s .. "]")
end
return a
