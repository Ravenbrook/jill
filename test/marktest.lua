function mark (a)
  local function fred (b)
    local function jim (c)
      return function (d) return a+b+c+d end ;
      end;
    return jim (1000) ;
  end ;
  local x = fred (23*a) ;
  return  x(1.5) ;
end;

print (mark (1000000)) ;