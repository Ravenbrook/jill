-- The Computer Language Shootout
-- http://shootout.alioth.debian.org/
-- contributed by Mike Pall

local n = tonumber((...)) or 100000
local function pr(fmt, x) print(string.format(fmt, x)) end

local a1, a2, a3, a4, a5, a6, a7, a8, a9, alt = 1, 0, 0, 0, 0, 0, 0, 0, 0, 1
local sqrt, sin, cos = math.sqrt, math.sin, math.cos
for k=1,n do
  local k2, sk, ck = k*k, sin(k), cos(k)
  local k3 = k2*k
  a1 = a1 + (2/3)^k
  a2 = a2 + 1/sqrt(k)
  a3 = a3 + 1/(k2+k)
  a4 = a4 + 1/(k3*sk*sk)
  a5 = a5 + 1/(k3*ck*ck)
  a6 = a6 + 1/k
  a7 = a7 + 1/k2
  a8 = a8 + alt/k
  a9 = a9 + alt/(k+k-1)
  alt = -alt
end
pr("%.9f\t(2/3)^k", a1)
pr("%.9f\tk^-0.5", a2)
pr("%.9f\t1/k(k+1)", a3)
pr("%.9f\tFlint Hills", a4)
pr("%.9f\tCookson Hills", a5)
pr("%.9f\tHarmonic", a6)
pr("%.9f\tRiemann Zeta", a7)
pr("%.9f\tAlternating Harmonic", a8)
pr("%.9f\tGregory", a9)

