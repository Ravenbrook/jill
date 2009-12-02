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
