class List (next, content, previous)
   append(node)
      if this.next = null then
         this.next := node;
         node.previous := this;
      else
        this.next.append(node);
      end
      return this;
   end

   insert_after(node)
        node.next := this.next;
        node.previous := this;
        if this.next <> null then
            this.next.previous := node;
            this.next := node;
        else skip; end
        return this;
   end


   remove()
        if this.next <> null then
            this.next.previous := this.previous;
        else skip; end
        if this.previous <> null then
            this.previous.next := this.next;
        else skip; end
        this.next := null;
        this.previous := null;
        return this;
   end


   remove_unclean()
        if this.next <> null then
            this.next.previous := this.previous;
        else skip; end
        if this.previous <> null then
            this.previous.next := this.next;
        else skip; end
        this.next := null;
        return this;
   end
end


main
  a := new List(null, 1, null);
  b := new List(null, 2, null);
  c := new List(null, 4, null);
  d := new List(null, 3, null);
  a.append(b);
  a.append(c);
  b.insert_after(d);
  c.remove();
end