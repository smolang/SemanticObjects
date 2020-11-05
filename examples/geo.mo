class GeoElement (sealing)

end

class GeoUnitList(content, next)

    append(last)
        if this.next = null then
            this.next := last;
            return 0;
        else
            this.next.append(last);
            return 0;
        end
    end

    get(i)
        res := this.content;
        if i >= 1 then
            res := this.next.get(i-1);
        else skip; end
        return res;
    end

    contains(element)
        if this.content = element then
            return 1;
        else
            if this.next = null then
                return 0;
            else
                res := this.next.contains(element);
                return res;
            end
        end
    end
end

class GeoManager (list)
    createNew(seal)
        tmp := new GeoUnit(seal, null, null, null, null);
        this.list := new GeoUnitList(tmp, this.list);
        return tmp;
    end

    connectLR(g1, g2)
        cn1 := new Touch(0, g1, g2);
        g1.right := cn1;
        g2.left := cn1;
        return 0;
    end


    connectTB(g1, g2)
        cn1 := new Touch(0, g1, g2);
        g1.bottom := cn1;
        g2.top := cn1;
        return 0;
    end

    startEarthquake(sealing, gu)
        known := this.list.contains(gu);
        if known = 1 then
            while gu.top <> null do
                gu := gu.top.s1;
            end
            fault := new Fault(sealing, null, null);
            gu.earthquake(fault);
            return 0;
        else return 0; end
    end
end



class GeoUnit extends GeoElement (top, left, right, bottom)
    earthquake(fault)
        if this.right <> null then
            fault.replaces(this.right);
            if this.bottom <> null then
                this.bottom.s2.earthquake(fault);
            else skip; end
        else skip; end
        return 0;
    end
end

class Touch extends GeoElement (s1, s2)

end

class Fault extends GeoElement (s1, s2)
    replaces(touch)
        touch.s1.right := this;
        touch.s2.left  := this;
        this.s1 := new GeoUnitList(touch.s1, this.s1);
        this.s2 := new GeoUnitList(touch.s1, this.s2);
        return 0;
    end
end


do
    gu1 := new GeoUnit(0, null, null, null, null);
    initList := new GeoUnitList(gu1, null);
    manager := new GeoManager(initList);
    gu2 := manager.createNew(1);
    gu3 := manager.createNew(1);
    gu4 := manager.createNew(0);
    gu5 := manager.createNew(0);
    gu6 := manager.createNew(1);
    manager.connectLR(gu1, gu2);
    manager.connectLR(gu2, gu3);
    manager.connectLR(gu4, gu5);
    manager.connectLR(gu5, gu6);
    manager.connectTB(gu1, gu4);
    manager.connectTB(gu2, gu5);
    manager.connectTB(gu3, gu6);
    manager.startEarthquake(1, gu4);
od
