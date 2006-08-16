-- The Computer Language Shootout
-- http://shootout.alioth.debian.org/
-- contributed by Mike Pall

local function ack(m, n)
  if m == 0 then return n+1 end
  if n == 0 then return ack(m-1, 1) end
  return ack(m-1, (ack(m, n-1))) -- The parentheses are deliberate.
end

local function fib(n)
  if n < 2 then return 1 end
  return fib(n-2) + fib(n-1)
end

local function tak(x, y, z)
  if y >= x then return z end
  return tak(tak(x-1, y, z), tak(y-1, z, x), (tak(z-1, x, y)))
end

local format = string.format
local n = (tonumber((...)) or 3)- 1
print(format("Ack(3,%d): %d", n+1, ack(3, n+1)))
print(format("Fib(%.1f): %.1f", n+28.0, fib(n+28.0)))
print(format("Tak(%d,%d,%d): %d", 3*n, 2*n, n, tak(3*n, 2*n, n)))
print(format("Fib(3): %d", fib(3)))
print(format("Tak(3.0,2.0,1.0): %.1f", tak(3.0, 2.0, 1.0)))

