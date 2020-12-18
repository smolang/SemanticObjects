class Pair(key, value) end

class Node(dataL, dataR, childL, childM, childR, parent)
    add(kv)
        res := null;
        if(this.childL = null) then //this is a leaf
            if(this.dataL = null)  then this.dataL := kv;
            else
                if (this.dataR = null) then
                     if (this.dataL.key <= kv.key) then this.dataR := kv;
                     else
                        this.dataR := this.dataL;
                        this.dataL := kv;
                     end
                else
                    res := this.createNew(kv);
                end
            end
        else                    //this is an internal node
            if(kv.key <= this.dataL.key) then res := this.childL.add(kv);
            else
                if(this.dataR = null) then res := this.childM.add(kv); //this is a 2-node
                else                                        //this is a 3-node
                    if(kv.key <= this.dataR.key) then res := this.childM.add(kv);
                    else res := this.childR.add(kv);
                    end
                end
             end
        end
        return res;
    end


    createNew(kv)
        //find middle
        if(kv.key <= this.dataL.key) then
            middle := this.dataL;
            left := new Node(kv, null, null, null, null, null);
            right := new Node(this.dataR, null, null, null, null, null);
        else
            if (kv.key >= this.dataR.key) then
                middle := this.dataR;
                left := new Node(this.dataL, null, null, null, null, null);
                right := new Node(kv, null, null, null, null, null);
            else
                middle := kv;
                left := new Node(this.dataL, null, null, null, null, null);
                right := new Node(this.dataR, null, null, null, null, null);
            end
         end
        newTop := this.repairOrReroot(middle, left, right);
        return newTop;
    end

    repair(left, middle, right, from)
        if(this.dataR = null) then  //internal 2-node
            if(from = this.childL) then
                this.dataR := this.dataL;
                this.dataL := middle;
                this.childR := this.childM;
                this.childM := right;
                this.childL := left;
                right.parent := this;
                left.parent := this;
            else
                this.dataR := middle;
                this.childR := right;
                this.childM := left;
                right.parent := this;
                left.parent := this;
            end
            return null;
        else                        //internal 3-node
            if(from = this.childL) then
                 newLeft := new Node(middle, null, left, right, null, null);
                 left.parent := newLeft;
                 right.parent := newLeft;
                 newRight := new Node(this.dataR, null, this.childM, this.childR, null, null);
                 this.childM.parent := newRight;
                 this.childR.parent := newRight;
                 res := this.repairOrReroot(this.dataR, newLeft, newRight);
            else
                if(from = this.childM) then
                     newLeft := new Node(this.dataL, null, this.childL, left, null, null);
                     this.childL.parent := newLeft;
                     left.parent := newLeft;
                     newRight := new Node(this.dataR, null, right, this.childR, null, null);
                     right.parent := newRight;
                     this.childR.parent := newRight;
                     res := this.repairOrReroot(this.dataR, newLeft, newRight);
                else
                     newLeft := new Node(this.dataL, null, this.childL, this.childM, null, null);
                     this.childL.parent := newLeft;
                     this.childM.parent := newLeft;
                     newRight := new Node(middle, null, left, right, null, null);
                     left.parent := newRight;
                     right.parent := newRight;
                     res := this.repairOrReroot(this.dataR, newLeft, newRight);
                end
            end
        return res;
        end
    end

    repairOrReroot(middle, left, right)
        if(this.parent = null) then //root
            newTop := new Node(middle, null, left, right, null, null);
            left.parent := newTop;
            right.parent := newTop;
        else
            newTop := this.parent.repair(left, middle, right, this);
            this.parent := null;
        end
        return newTop;
    end
    retrieve(k)
       if(this.dataL = null) then res := null; return res; end
       if(k = this.dataL.key) then res := this.dataL.value; return res; end
       if(k <= this.dataL.key) then res := this.childL.retrieve(k); return res; end
       if(this.dataR = null) then res := this.childM.retrieve(k); return res; end
       if(k = this.dataR.key) then res := this.dataR.value; return res; end
       if(k <= this.dataR.key) then res := this.childM.retrieve(k); return res; end
       res := this.childR.retrieve(k);
       return res;
    end
end

class Tree(root)
    add(kv)
        res := this.root.add(kv);
        if(res <> null) then this.root := res; end
        return 0;
    end
    retrieve(k)
       res := this.root.retrieve(k);
       return res;
    end
end

class Wrap(ttt, key)
    rule lookup()
        v := this.ttt.retrieve(this.key);
        return v;
    end
end

class List(content, next)
    length()
        if(this.next = null) then return 1;
        else n := this.next.length(); return n + 1; end
    end
end

main
    root := new Node(null, null, null, null, null, null);
    tree := new Tree(root);
    pair1 := new Pair(1,"a");
    tree.add(pair1);
    i := 2;
    while( i <= 1000 ) do
    	pair2 := new Pair(i,"b");
	    tree.add(pair2);
	    i := i + 1;
    end

    ww := new Wrap(tree, 1);
    res := ww.lookup();
    res := access("SELECT ?obj WHERE {?sth :Wrap_lookup_builtin_res ?obj}");
    print(res.content);

    res := tree.retrieve(1);
    print(res);

    print("done");
end
