class C extends D (Int f)
  Int m(Int p)
    this.f := this.f + 1;
  	return p + this.f;
  end
end

class D (Int g)
    rule Int n(Int p) //this is supposed to fail
        return p + p;
    end
end

main
  C v := new C(5, 4);
  D q := new D(1);
  Int w := v.m(2);
  w := v.m(2);
  w := v.m(2);
  w := v.m(2);
  print(w);
  print(v.f);
end