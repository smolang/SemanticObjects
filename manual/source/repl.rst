The SMOL interactive REPL
=========================

SMOL programs are run and queried via a :term:`REPL`.  Currently, the
REPL offers the following commands:

General Commands
----------------

``exit``
   exits the REPL

``verbose`` enabled
   Sets verbose output to on or off

   Parameter:

   - enabled
      ``true`` or ``false``

Commands for Running SMOL
-------------------------

``read`` file
   reads the given file

   Parameter:

   - ``file``
      Path to a ``.smol`` file

``reada`` file
   reads and runs the given file

   Parameter:

   - ``file``
      Path to a ``.smol`` file

``auto``
   starts or continues execution of the currently-loaded smol file until the
   next breakpoint

``step``
   executes the next statement

Commands for Querying SMOL
--------------------------

``eval`` expression
   evaluates a smol expression in the current program state

   Parameter:
   
   - expression
      a smol expression

``query`` query
   executes a SPARQL query in the current program state

   Parameter:

   - query
      The SPARQL query to execute

``source`` source enabled
   Set which sources to include (true) or exclude (false) when querying

   Parameter:
   
   - source
      ``heap`` or ``staticTable`` or ``vocabularyFile`` or ``externalOntology``

   - enabled
      ``true`` or ``false``

``reasoner`` reasoner
   Specify which Jena reasoner to use, or turn it off

   Parameter:

   - reasoner
     ``off`` or ``rdfs`` or ``owl``

``class`` class
   returns all members of a class

   Parameter:

   - class
      class expression in Manchester Syntax, e.g., ``<smol:Class>``

``plot`` role port from to
   Plots data from the given output port of an FMO in the given interval.  In
   order to use this command, gnuplot must be installed.

   Parameter:

   - role
      The FMO to plot data from, as named by its ``role`` field.

   - port
      The output port of the FMO to be plotted

   - from
      (optional) the starting time of the plot

   - to
      (optional) the end time of the plot
   


Diagnostic Commands
-------------------

``consistency``
   prints all classes and checks that the internal ontology is consistent

``info``
   prints static information in internal format

``examine``
   prints state in internal format

``dump``
   dumps into ``${tmp_path}/output.ttl``

``guards`` guard enabled
   Enables/disables guard clauses when searching for triples in the heap or
   the static table.  This command is mainly used for debugging and
   performance measuring.

   Parameter:
   
   - guard
      ``heap`` or ``staticTable``
      
   - enabled
      ``true`` or ``false``

``virtual`` guard enabled
   Enables/disables virtualization searching for triples in the heap or the
   static table.  This command is mainly used for debugging and performance
   measuring.

   Parameter:

   - guard
      ``heap`` or ``staticTable``
      
   - enabled
      ``true`` or ``false``

