class Pair(key, value) end

class Node(dataL, dataR, childL, childM, childR, parent)
    add(kv)
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
                    print("I should crate a new node");
                end
            end
        else                    //this is an internal node
            if(kv.value <= this.dataL) then this.childL.add(kv);
            else
                if(this.dataR = null) then this.childM.add(kv); //this is a 2-node
                else                                        //this is a 3-node
                    if(kv.value <= this.dataR) then this.childM.add(kv);
                    else this.childR.add(kv);
                    end
                end
             end
        end
        return 0;
    end

end

class Tree(root)
    add(kv)
        this.root.add(kv);
        return 0;
    end
end


main
    root := new Node(null, null, null, null, null, null);
    tree := new Tree(root);
    pair1 := new Pair(1,"a");
    pair2 := new Pair(2,"b");
    pair3 := new Pair(3,"c");
    /*pair4 := new Pair(4,"d");
    pair5 := new Pair(5,"e");
    pair6 := new Pair(6,"f");
    pair7 := new Pair(7,"g");*/
    tree.add(pair3);
    tree.add(pair2);
    tree.add(pair1);
    print("done");
    breakpoint;

end