class Scene(scaling) getScale() return this.scaling; end end

class Rectangle(scene, w, h, name)
    rule area()
        s := this.scene.getScale();
        return s*this.w*this.h;
    end
end

main
    sc := new Scene(2);
    r := new Rectangle(sc, 5, 1, "rect1");
end