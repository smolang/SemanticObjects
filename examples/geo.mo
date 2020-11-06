class GeoElement (sealing)

end

class Dummy()

end

class LeftHydrocarbonMigration (gu)
    migrate()
        started := this.gu;
        b := 1;
        while b = 1 do
            b := this.gu.migrate();
        end
            left := this.gu.left;
            if left <> null then
                left.migrateLeft(this);
            end
            if started <> this.gu then
                this.migrate();
            end
        return 0;
    end
    copy(param)
        c := new LeftHydrocarbonMigration(param);
        return c;
    end
end

class GeoUnitList(content, next)

    append(last)
        if this.next = null then
            this.next := last;
        else
            this.next.append(last);
        end
        return 0;
    end

    get(i)
        res := this.content;
        if i >= 1 then
            res := this.next.get(i-1);
        end
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
        tmp := new GeoUnit(seal, null, null, null, null, null);
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



class GeoUnit extends GeoElement (top, left, right, bottom, migration)
    earthquake(fault)
        if this.right <> null then
            fault.replaces(this.right);
            if this.bottom <> null then
                this.bottom.s2.earthquake(fault);
            end
        end
        return 0;
    end

    migrate()
        if this.top <> null then
            if this.top.s1.sealing <> 1 then
                old := this.migration.gu;
                this.top.s1.migration := this.migration;
                this.migration.gu := this.top.s1;
                old.migration := null;
                return 1;
            end
        end
        return 0;
    end

    getTopNonSealing()
        if this.sealing <> 1 then
            return this;
        else
            if this.top <> null then
                target := this.top.s1;
                res := target.getTopNonSealing();
                return res;
            else return this; end
        end
    end
end

class Touch extends GeoElement (s1, s2)
    migrateLeft(mig)
        if this.s1.sealing <> 1 then
            if this.s1.migration = null then
                old := mig.gu;
                this.s1.migration := mig;
                mig.gu := this.s1;
                old.migration := null;
                return 1;
            end
        end
        return 0;
    end
end

class Fault extends GeoElement (s1, s2)
    replaces(touch)
        touch.s1.right := this;
        touch.s2.left  := this;
        this.s1 := new GeoUnitList(touch.s1, this.s1);
        this.s2 := new GeoUnitList(touch.s2, this.s2);
        return 0;
    end

    migrateLeft(mig)
        if this.sealing <> 1 then
            if this.s1 <> null then
                currentLeft := this.s1;
                currentRight := this.s2;
                while currentRight.content <> mig.gu do
                    currentLeft := currentLeft.next;
                    currentRight := currentRight.next;
                end
                finalLeft := currentLeft.content.getTopNonSealing();
                if finalLeft.sealing <> 1 then
                    if finalLeft.migration = null then
                        old := mig.gu;
                        finalLeft.migration := mig;
                        mig.gu := finalLeft;
                        old.migration := null;
                        return 1;
                   end
                end
            end
        end
        return 0;
    end
end


do

    gu11 := new GeoUnit(1, null, null, null, null, null);
    initList := new GeoUnitList(gu11, null);
    manager := new GeoManager(initList);



    gu12 := manager.createNew(1);
    gu13 := manager.createNew(1);
    gu14 := manager.createNew(1);
    gu15 := manager.createNew(1);
    manager.connectLR(gu11, gu12);
    manager.connectLR(gu12, gu13);
    manager.connectLR(gu13, gu14);
    manager.connectLR(gu14, gu15);

    print(1);

    gu21 := manager.createNew(1);
    gu22 := manager.createNew(0);
    gu23 := manager.createNew(1);
    gu24 := manager.createNew(0);
    gu25 := manager.createNew(1);
    manager.connectTB(gu11, gu21);
    manager.connectTB(gu12, gu22);
    manager.connectTB(gu13, gu23);
    manager.connectTB(gu14, gu24);
    manager.connectTB(gu15, gu25);
    manager.connectLR(gu21, gu22);
    manager.connectLR(gu22, gu23);
    manager.connectLR(gu23, gu24);
    manager.connectLR(gu24, gu25);

    print(2);

    gu31 := manager.createNew(1);
    gu32 := manager.createNew(0);
    gu33 := manager.createNew(0);
    gu34 := manager.createNew(1);
    gu35 := manager.createNew(1);
    manager.connectTB(gu21, gu31);
    manager.connectTB(gu22, gu32);
    manager.connectTB(gu23, gu33);
    manager.connectTB(gu24, gu34);
    manager.connectTB(gu25, gu35);
    manager.connectLR(gu31, gu32);
    manager.connectLR(gu32, gu33);
    manager.connectLR(gu33, gu34);
    manager.connectLR(gu34, gu35);

    print(3);

    gu41 := manager.createNew(1);
    gu42 := manager.createNew(1);
    gu43 := manager.createNew(1);
    gu44 := manager.createNew(1);
    gu45 := manager.createNew(1);
    gu46 := manager.createNew(1);

    manager.connectTB(gu31, gu41);
    manager.connectTB(gu32, gu42);
    manager.connectTB(gu33, gu43);
    manager.connectTB(gu34, gu44);
    manager.connectTB(gu35, gu45);

    manager.connectLR(gu41, gu42);
    manager.connectLR(gu42, gu43);
    manager.connectLR(gu43, gu44);
    manager.connectLR(gu44, gu45);
    manager.connectLR(gu45, gu46);

    print(4);

    gu53 := manager.createNew(1);
    gu54 := manager.createNew(0);
    gu55 := manager.createNew(0);
    gu56 := manager.createNew(1);
    manager.connectTB(gu43, gu53);
    manager.connectTB(gu44, gu54);
    manager.connectTB(gu45, gu55);
    manager.connectTB(gu46, gu56);
    manager.connectLR(gu53, gu54);
    manager.connectLR(gu54, gu55);
    manager.connectLR(gu55, gu56);

    print(5);

    gu63 := manager.createNew(1);
    gu64 := manager.createNew(1);
    gu65 := manager.createNew(1);
    gu66 := manager.createNew(1);
    manager.connectTB(gu53, gu63);
    manager.connectTB(gu54, gu64);
    manager.connectTB(gu55, gu65);
    manager.connectTB(gu56, gu66);
    manager.connectLR(gu63, gu64);
    manager.connectLR(gu64, gu65);
    manager.connectLR(gu65, gu66);

    print(6);


    manager.startEarthquake(0, gu63);

    mig := new LeftHydrocarbonMigration(gu55);
    gu55.migration := mig;
    mig.migrate();
    breakpoint;
    breakpoint;

/*
    gu1 := new GeoUnit(0, null, null, null, null, null);
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
    manager.startEarthquake(0, gu4);
    mig := new LeftHydrocarbonMigration(gu5);
    gu5.migration := mig;
    mig.migrate();
*/
od
