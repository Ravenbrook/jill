-- $Header$
function foo(t)
  t.f = function() return t end
end
t = {}
foo(t)
return t.f()==t
