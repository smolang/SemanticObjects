Getting Started
===============

.. highlight:: java

This section shows how to run a simple SMOL program.

SMOL itself is started from the command line and is typically used via an
interactive prompt (:term:`REPL`).  SMOL needs Java (version 11 or later) to
be installed.  A command like the following starts a SMOL REPL in the terminal
(assuming the current directory is the root of the smol source tree and smol
has been built)::

  java -jar build/libs/smol-0.2-all.jar

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

Starting SMOL
-------------

(describe how to compile and start the jar ...)

::

   Options:
     -e, -l, --execute, --load
     -b, --back PATH            path to a .ttl file that contains OWL class
                                definitions as background knowledge.
     -d, --domain TEXT          prefix for domain:.
     -i, --input PATH           path to a .smol file which is loaded on startup.
     -r, --replay PATH          path to a file containing a series of shell
                                commands.
     -o, --outdir PATH          path to a directory used to create data
                                files.
     -v, --verbose              Verbose output.
     -m, --materialize          Materialize triples and dump to file.
     -p, --prefixes VALUE       Extra prefixes, given as a list PREFIX=URI
     -h, --help                 Show this message and exit


.. _repl:

The SMOL interactive REPL
-------------------------

SMOL programs are run and queried via the :term:`REPL`.  Currently, the
REPL offers the following commands:

General Commands
^^^^^^^^^^^^^^^^

.. list-table::
   :header-rows: 1
   :align: left
   :widths: auto

   * - Command
     - Description
     - Parameters
   * - ``exit``
     - exits the REPL
     -
   * - ``verbose`` *enabled*
     - Sets verbose output to on or off
     - *enabled*: ``true`` or ``false``
   * - ``outpath`` *path*
     - Sets or prints the directory where SMOL write data files
     - *path*: a directory name; if omitted, print the current value

Commands for Running SMOL
^^^^^^^^^^^^^^^^^^^^^^^^^

.. list-table::
   :header-rows: 1
   :align: left
   :widths: auto

   * - Command
     - Description
     - Parameters
   * - ``read`` *file*
     - reads a SMOL file
     - *file*: Path to the ``.smol`` file
   * - ``reada`` *file*
     - reads and runs the given file
     - *file*: Path to the ``.smol`` file
   * - ``auto``
     - starts or continues execution of the currently-loaded smol file until
       the next breakpoint
     -
   * - ``step``
     - executes the next statement
     -

.. _querying-smol:

Commands for Querying SMOL
^^^^^^^^^^^^^^^^^^^^^^^^^^

.. list-table::
   :header-rows: 1
   :align: left
   :widths: auto

   * - Command
     - Description
     - Parameters
   * - ``eval`` *expression*
     - evaluates a smol expression in the current program state
     - *expression*: a smol expression
   * - ``query`` *query*
     - executes a SPARQL query in the current program state
     - *query*: The SPARQL query to execute
   * - ``source`` *source* *enabled*
     - Set which sources to include (true) or exclude (false) when querying
     - - *source*: ``heap`` or ``staticTable`` or ``vocabularyFile`` or
         ``externalOntology``
       - *enabled*: ``true`` or ``false``
   * - ``reasoner`` *reasoner*
     - Specify which Jena reasoner to use, or turn it off
     - *reasoner*: ``off`` or ``rdfs`` or ``owl``
   * - ``class`` *class*
     - List all members of a class
     - *class*: class expression in Manchester Syntax, e.g., ``<smol:Class>``
   * - ``plot`` *role* *port* *from* *to*
     - Plots data from the given output port of an FMO in the given interval.
       In order to use this command, gnuplot must be installed.
     - - *role*: The FMO to plot data from, as named by its ``role`` field.
       - *port*: The output port of the FMO to be plotted
       - *from*: (optional) the starting time of the plot
       - *to*: (optional) the end time of the plot
   * - ``dump`` *file*
     - Create file in ``outpath`` containing the current heap state in TRTL
       format
     - *file* (optional): the file to create; default ``output.ttl``

Diagnostic Commands
^^^^^^^^^^^^^^^^^^^

.. list-table::
   :header-rows: 1
   :align: left
   :widths: auto

   * - Command
     - Description
     - Parameters
   * - ``consistency``
     - Print all classes and check that the internal ontology is consistent
     -
   * - ``info``
     - Print static information in internal format
     -
   * - ``examine``
     - Print state in internal format
     -
   * - ``guards`` *guard* *enabled*
     - Enables/disables guard clauses when searching for triples in the heap
       or the static table.  This command is mainly used for debugging and
       performance measuring.
     - - *guard*: ``heap`` or ``staticTable``
       - *enabled*: ``true`` or ``false``
   * - ``virtual`` *guard* *enabled*
     - Enables/disables virtualization searching for triples in the heap or
       the static table.  This command is mainly used for debugging and
       performance measuring.
     - - *guard*: ``heap`` or ``staticTable``
       - *enabled*: ``true`` or ``false``
