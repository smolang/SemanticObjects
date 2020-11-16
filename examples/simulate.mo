class D (g)
    rule n()
        return this.g + this.g;
    end
end

do
  q := new D(2);
od