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
-- from [LUA 2006-03-26] strings.lua
function testmatch()
  local a,b = string.match('the quick brown fox jumps over the lazy dog',
      'br(%a+).-o(%w%w+)%s')
  return a == 'own', b == 'ver'
end
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


  -- longest number that can be formated
  assert(string.len(string.format('%99.99f', -1e308)) >= 100)
end

