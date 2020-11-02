class C extends D (f)
  m(p)
    this.g := this.g + 1;
  	return p + this.f;
  end
end

class D (g)
end

do
  v  := new C(5, 4);
  w := v.m(2);
  w := v.m(2);
  breakpoint;
  w := v.m(2);
  w := v.m(2);
  print(w);
  print(v.f);
od













/*
PROGRAM test;
FIELDS i;
VARIABLES j;
INVARIANT this.i = 0;
CODE

this.i = 1;
while (j > 0) preserves (this.i = 1) do
	this.i = 0;
	await 1 = 1;
	this.i = 1;
	
	
	j = j - 1
od
this.i = 0;
return 0
*/
