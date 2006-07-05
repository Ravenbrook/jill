local b = 7
local a = function (x) 
  local res,ires = {},0 ;
  while x<b do 
    local s = x*b ;
    function blah () s = s+1 ; return s end 
    res[ires] = blah ; ires = ires+1;
    x = x+1 ;
  end;
  return res;
end;
return a(4)
