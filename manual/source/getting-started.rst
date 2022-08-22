Getting Started
===============

.. highlight:: java

This section shows how to run a simple SMOL program.

SMOL itself is started from the command line and is typically used via an
interactive prompt (:term:`REPL`).  SMOL needs Java (version 11 or later) to
be installed.  A command like the following starts a SMOL REPL in the
terminal::

  java -jar MicroObjects-0.2-all.jar

Here is a SMOL program that prints the canonical message::

  main
    print("Hello world!");
  end

To run this code, save it in a file ``hello-world.smol``, and run it from the
REPL::

  MO> read hello-world.smol
  MO> auto
  "Hello world!"
  MO>

Instead of separate ``read`` and ``auto`` commands, you can also use
``reada``; see :ref:`repl` for a full list of commands.

Here is a more involved example, involving classes and breakpoints::

  class Hello(String message)
   Unit say_hello()
     print(this.message);
    end
  end
  
  main
    print("Creating class ...");
    Hello hello = new Hello("Hello world!");
    breakpoint;
    hello.say_hello();
  end
  
Since the program will stop execution at the breakpoint, the runtime state can
be queried from the REPL::

  MO> reada /Users/rudi/Source/tmp/hello-world.smol
  "Creating class ..."
  MO> query SELECT ?obj ?message WHERE { ?obj a prog:Hello. ?obj prog:Hello_message ?message. }
  --------------------------------------------------------------------------------------
  | obj                                                               | message        |
  ======================================================================================
  | <https://github.com/Edkamb/SemanticObjects/Run1660809137988#obj3> | "Hello world!" |
  --------------------------------------------------------------------------------------
  MO> auto
  "Hello world!"
  MO>

Runtime state is queried using the SPARQL query language, both from the REPL
and in the program.
