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
  q := new D(2);
od