class C extends D (f)
  m(p)
    this.g := this.g + 1;
  	return p + this.f;
  end
end

class D (g)
    rule n(p)
        return p + p;
    end
end

do
  v  := new C(5, 4);
  q := new D(1);
  w := v.m(2);
  w := v.m(2);
  w := v.m(2);
  w := v.m(2);
  print(w);
  print(v.f);
od