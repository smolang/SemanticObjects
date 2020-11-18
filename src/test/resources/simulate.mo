class D (g)
    rule n()
        v := new E();
        res := v.m(this.g);
        return this.g + res;
    end
end

class E()
    m(p)
        return p + p;
    end
end

do
  a := new D(2);
  b := new D(3);
od