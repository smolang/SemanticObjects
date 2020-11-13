class D (g)
    rule n(p)
        return p + p;
    end
end

do
  q := new D(1);
od