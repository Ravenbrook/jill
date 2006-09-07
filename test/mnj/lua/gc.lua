print('testing garbage collection')

collectgarbage()

_G["while"] = 234

limit = 5000



contCreate = 0

print('tables')
while contCreate <= limit do
  local a = {}; a = nil
  contCreate = contCreate+1
end

a = "a"

contCreate = 0
print('strings')
while contCreate <= limit do
  a = contCreate .. "b";
  a = string.gsub(a, '(%d%d*)', string.upper)
  a = "a"
  contCreate = contCreate+1
end


contCreate = 0

a = {}

print('functions')
function a:test ()
  while contCreate <= limit do
    loadstring(string.format("function temp(a) return 'a%d' end", contCreate))()
    assert(temp() == string.format('a%d', contCreate))
    contCreate = contCreate+1
  end
end

a:test()

-- collection of functions without locals, globals, etc.
do local f = function () end end


print("functions with errors")
prog = [[
do
  a = 10;
  function foo(x,y)
    a = sin(a+0.456-0.23e-12);
    return function (z) return sin(%x+z) end
  end
  local x = function (w) a=a+w; end
end
]]
do
  local step = 1
  if rawget(_G, "_soft") then step = 13 end
  for i=1, string.len(prog), step do
    for j=i, string.len(prog), step do
      pcall(loadstring(string.sub(prog, i, j)))
    end
  end
end

print('long strings')
x = "01234567890123456789012345678901234567890123456789012345678901234567890123456789"
assert(string.len(x)==80)
s = ''
n = 0
k = 300
while n < k do s = s..x; n=n+1; j=tostring(n)  end
assert(string.len(s) == k*80)
s = string.sub(s, 1, 20000)
s, i = string.gsub(s, '(%d%d%d%d)', math.sin)
assert(i==20000/4)
s = nil
x = nil

assert(_G["while"] == 234)


lim = 15
a = {}
-- fill a with `collectable' indices
for i=1,lim do a[{}] = i end
b = {}
for k,v in pairs(a) do b[k]=v end
-- remove all indices and collect them
for n in pairs(b) do
  a[n] = nil
  assert(type(n) == 'table' and next(n) == nil)
  collectgarbage()
end
b = nil
collectgarbage()
for n in pairs(a) do error'cannot be here' end
for i=1,lim do a[i] = i end
for i=1,lim do assert(a[i] == i) end


if not rawget(_G, "_soft") then
  print("deep structures")
  local a = {}
  for i = 1,200000 do
    a = {next = a}
  end
  collectgarbage()
end

-- create many threads with self-references and open upvalues
local thread_id = 0
local threads = {}

function fn(thread)
    local x = {}
    threads[thread_id] = function()
                             thread = x
                         end
    coroutine.yield()
end

while thread_id < 1000 do
    local thread = coroutine.create(fn)
    coroutine.resume(thread, thread)
    thread_id = thread_id + 1
end


print('OK')
