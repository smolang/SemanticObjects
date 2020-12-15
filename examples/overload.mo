class List(content, next)
    length()
        if this.next = null then return 1;
        else n := this.next.length; return n + 1;
        end
    end
    contains(elem)
        if this.content = elem then return True; end
        if this.next = null then return False; end
        b := this.next.contains(elem);
        return b;
    end
end

class Task(name) end

class Server(taskList)
    excessive()
        ret := this.taskList.content;
        this.taskList := this.taskList.next;
        return ret;
    end
    add(task)
        this.taskList := new List(task, this.taskList);
        return 0;
    end
end
class Scheduler(serverList)
    reschedule()
        over := access("SELECT ?obj WHERE{?obj a :Overloaded }");
        //breakpoint;
        tasks := this.collectExcessiveTasks(over);
        //breakpoint;
        tasks := this.rescheduleTasks(tasks, over);
        //breakpoint;
        while tasks <> null do
            l := new List(tasks.content,null);
            n := new Server(l);
            this.serverList := new List(n, this.serverList);
            tasks := tasks.next;
        end
        return 0;
    end

    collectExcessiveTasks(overloaded)
       plats := this.serverList;
       exc := null;
       while plats <> null do
            b := overloaded.contains(plats.content);
            //breakpoint;
            if b then
                localExc := plats.content.excessive();
                exc := new List(localExc, exc);
            end
            plats := plats.next;
       end
       return exc;
    end

    rescheduleTasks(tasks, overloaded)
       plats := this.serverList;
       while plats <> null do
            b := overloaded.contains(plats.content);
            if b then
                skip;
            else
                plats.content.add(tasks.content);
                tasks := tasks.next;
            end
            plats := plats.next;
            if tasks = null then return null; end
       end
       return tasks;
    end
end

main
    task1 := new Task("t1");
    task2 := new Task("t2");
    task3 := new Task("t3");
    task4 := new Task("t4");
    task5 := new Task("t5");
    task6 := new Task("t6");

    l1 := new List(task1, null);
    l2 := new List(task2, null);
    l3 := new List(task3, l2);
    l4 := new List(task4, null);
    l5 := new List(task5, l4);
    l6 := new List(task6, l5);

    dummy := new Server(null);
    server1 := new Server(l1);
    server2 := new Server(l3);
    server3 := new Server(l6);
    sl1 := new List(server3, null);
    sl2 := new List(server2, sl1);
    sl3 := new List(server1, sl2);

    sch := new Scheduler(sl3);
    breakpoint;
    sch.reschedule();
end
