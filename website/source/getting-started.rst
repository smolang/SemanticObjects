Getting Started
===============

.. highlight:: java

Installation
------------

Currently, SMOL is best installed from source.

SMOL needs a Java JDK (version 11 or later) to be installed.  The smol source
code can be checked out via::

  git clone https://github.com/smolang/SemanticObjects.git

or by downloading and unpacking the zip archive from
https://github.com/smolang/SemanticObjects/archive/refs/heads/master.zip.

After obtaining the source code, change into that directory and execute the command::

  ./gradlew assemble

After a successful build, the SMOL REPL can be started in that directory via::

  java -jar build/libs/smol.jar

Editor Support
^^^^^^^^^^^^^^

There is basic support for SMOL for the following editors:

- `Atom <https://github.com/atom/atom>`_

  Supports syntax highlighting. Source, installation instructions at https://github.com/smolang/SemanticObjects/tree/master/editor-support/atom

- `Emacs <https://www.gnu.org/software/emacs/>`_

  Adds support for syntax highlighting and for running a SMOL REPL inside Emacs. Source, installation instructions at https://github.com/smolang/SemanticObjects/tree/master/editor-support/emacs

- `Visual Studio Code <https://code.visualstudio.com>`_

  Supports syntax highlighting. Source, installation instructions at https://github.com/smolang/smol-vs-code. Latest release at https://github.com/smolang/smol-vs-code/releases/latest.

Running a Simple SMOL Program
-----------------------------

SMOL itself is started from the command line and is typically used via an
interactive prompt (a.k.a. :term:`REPL`).

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

SMOL Command-Line Parameters
----------------------------

::

   Options:
     -e, -l, --execute, --load
     -b, --back PATH            path to a file containing OWL class
                                definitions as background knowledge.
     -d, --domain TEXT          prefix for domain:. (default
                                https://github.com/Edkamb/SemanticObjects/ontologies/default#)
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
   * - ``outdir`` *path*
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
     - Plots data from the given output port of an :term:`FMO` in the given interval.
       In order to use this command, gnuplot must be installed.
     - - *role*: The :term:`FMO` to plot data from, as named by its ``role`` field.
       - *port*: The output port of the FMO to be plotted
       - *from*: (optional) the starting time of the plot
       - *to*: (optional) the end time of the plot
   * - ``dump`` *file*
     - Create file in ``outdir`` containing the current heap state in TRTL
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
