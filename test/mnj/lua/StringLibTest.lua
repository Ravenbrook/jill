-- $Header$

function testlen()
  return string.len''==0, string.len'foo'==3
end
function testlower()
  return string.lower'Foo'=='foo', string.lower'foo'=='foo',
    string.lower' !801"'==' !801"'
end
function testrep()
  return string.rep('foo', 3)=='foofoofoo',
    string.rep('foo', 1)=='foo',
    string.rep('foo', 0)==''
end
function testupper()
  return string.upper'Foo'=='FOO', string.upper'FOO'=='FOO',
    string.upper' !801"'==' !801"'
end
function testsub()
  return string.sub('foobar', 4)=='bar',
    string.sub('foobar', 4, 4)=='b',
    string.sub('foobar', -5, -2)=='ooba'
end
function testmeta()
  local s='foobar'
  return s:len()==6, s:upper()=='FOOBAR'
end
function testreverse()
  return string.reverse'foo'=='oof',
    string.reverse''==''
end
function testbyte()
  local a,b,c = string.byte('ebcdic', 2, 4)
  return string.byte'foo'==102, string.byte('bar', 2)==97,
    a==98, b==99, c==100
end
function testchar()
  return string.char()=='',  string.char(102, 111, 111)=='foo'
end
function testfind()
  local a,b = string.find('food', 'foo')
  local c,d = string.find('Spong', '%l+')
  local s = '|a-|zap\t789foo!@#$%     XY0Bop9###Floo'
  local p =  'a.|%a*%c%d+%l-[%p]+[%s]*%u[%w]+#+%x'
  local e,f = string.find(s, p)
  return a==1,b==3,c==2,d==5,e==2,f==35
end
function testmatch()
  local a,b = string.match('the quick brown fox jumps over the lazy dog',
      'br(%a+).-o(%w%w+)%s')
  return a == 'own', b == 'ver'
end
-- from [LUA 2006-03-26] strings.lua
function testformat()
  x = '"?lo"\n\\'
  assert(string.format('%q%s', x, x) == '"\\"?lo\\"\\\n\\\\""?lo"\n\\')
  assert(string.format('%q', "\0") == [["\000"]])
  assert(string.format("\0%c\0%c%x\0", string.byte("?"), string.byte("b"), 140) ==
                "\0?\0b8c\0")
  assert(string.format('') == "")
  assert(string.format("%c",34)..string.format("%c",48)..string.format("%c",90)..string.format("%c",100) ==
         string.format("%c%c%c%c", 34, 48, 90, 100))
  assert(string.format("%s\0 is not \0%s", 'not be', 'be') == 'not be\0 is not \0be')
  assert(string.format("%%%d %010d", 10, 23) == "%10 0000000023")
  assert(tonumber(string.format("%f", 10.3)) == 10.3)
  x = string.format('"%-50s"', 'a')
  assert(#x == 52)
  assert(string.sub(x, 1, 4) == '"a  ')

  assert(string.format("-%.20s.20s", string.rep("%", 2000)) == "-"..string.rep("%", 20)..".20s")
  assert(string.format('"-%20s.20s"', string.rep("%", 2000)) ==
         string.format("%q", "-"..string.rep("%", 2000)..".20s"))
  return true
end
-- This one fails as of 2006-07-17
function testformatmore()
  -- longest number that can be formated
  assert(string.len(string.format('%99.99f', -1e308)) >= 100)
  return true
end
-- returns the positive difference
function err(a, b)
  a = a+0
  b = b+0
  if a < b then return b-a else return a-b end
end
function testformatx1()
  local x = 257/256
  local s = string.format("%.2f", x)
  print(s)
  assert(err(x, s) <= 0.01)
  assert(#s == 4)
  s = string.format("%7.3f", x)
  print(s)
  assert(err(x, s) <= 0.001)
  assert(#s == 7)
  assert(s:sub(1, 2) == string.rep(' ', 2))
  s = string.format("%08.4f", x)
  print(s)
  assert(err(x, s) <= 0.0001)
  assert(#s == 8)
  assert(s:sub(1, 2) == string.rep('0', 2))
  return true
end
function testformatx2()
  local x = 1234567890
  local s = string.format('%.2f', x)
  print(s)
  assert(err(x, s)==0)
  assert(#s == 13)
  assert(s:sub(1, 1) == '1')
  return true
end
function testformatx3()
  local x = 1/65536
  local s = string.format('%.4f', x)
  print(s)
  assert(err(x, s) <= 0.0001)
  assert(#s == 6)
  return true
end
function testformatx4()
  local x = -0.0003
  local s = string.format('%.7f', x)
  print(s)
  assert(err(x, s) <= 1e-4)
  assert(#s == 10)
  s = string.format('%07.2f', x)
  print(s)
  assert(err(x, s) <= 0.01)
  assert(#s == 7)
  assert(s:sub(1,5) == '-000.')
  return true
end
function testformatx5()
  local s = string.format('%i', 7)
  assert(s == '7')
  s = string.format('%u', 7)
  assert(s == '7')
  return true
end
-- from [LUA 2006-03-26] pm.lua
function testgsub()
  assert(string.gsub('  alo alo  ', '^%s*(.-)%s*$', '%1') == 'alo alo') -- double trim
  assert(string.gsub('alo  alo  \n 123\n ', '%s+', ' ') == 'alo alo 123 ')
  assert(string.gsub('alo alo', '()[al]', '%1') == '12o 56o')
  assert(string.gsub("abc=xyz", "(%w*)(%p)(%w+)", "%3%2%1-%0") ==
                "xyz=abc-abc=xyz")
  assert(string.gsub("abc", "%w", "%1%0") == "aabbcc")
  assert(string.gsub("abc", "%w+", "%0%1") == "abcabc")
  assert(string.gsub('', '^', 'r') == 'r')
  assert(string.gsub('', '$', 'r') == 'r')
  assert(string.gsub("um (dois) tres (quatro)", "(%(%w+%))", string.upper) ==
              "um (DOIS) tres (QUATRO)")
  return true
end
function testgsub2()
  assert(string.gsub('foo', '', 'x') == 'xfxoxox')
  return true
end
-- from [LUA 2006-03-26] pm.lua
-- treated to remove references to table.*
function testgmatch()
  local a = 0
  for i in string.gmatch('abcde', '()') do assert(i == a+1); a=i end
  assert(a==6)

  t = {n=0}
  for w in string.gmatch("first second word", "%w+") do
        t.n=t.n+1; t[t.n] = w
  end
  assert(t[1] == "first" and t[2] == "second" and t[3] == "word")

  t = {3, 6, 9}
  a = 1
  for i in string.gmatch ("xuxx uu ppar r", "()(.)%2") do
    assert(i == t[a])
    a = a+1
  end
  assert(a == 4)

  t = {}
  for i,j in string.gmatch("13 14 10 = 11, 15= 16, 22=23", "(%d+)%s*=%s*(%d+)") do
    t[i] = j
  end
  a = 0
  for k,v in pairs(t) do assert(k+1 == v+0); a=a+1 end
  assert(a == 3)
  return true
end
function testdump()
  local f = function()return 7 end
  local s = string.dump(f)
  assert(type(s) == 'string')
  assert(type(loadstring(s)) == 'function')
  assert(loadstring(s)() == 7)
  return true
end
