-- $Header$

function test1()
  assert(require'PackageLibTest' == 7)
end

function foof()
  module'foo'
end

function test2()
  foof()
  assert(type(foo) == 'table')
end

return 7
