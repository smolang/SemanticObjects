The SMOL interactive REPL
=========================

SMOL programs are run and queried via a :term:`REPL`.  Currently, the
REPL offers the following commands:

::

   exit        - exits the shell
   read        - reads a file
                 - parameter: Path to a .smol file
   reada       - reads a file and runs auto
                 - parameter: Path to a .smol file
   info        - prints static information in internal format
   examine     - prints state in internal format
   dump        - dumps into ${tmp_path}/output.ttl
   auto        - continues execution until the next breakpoint
   step        - executes one step
   guards      - Enables/disables guard clauses when searching for triples in the heap or the static table
                 - parameter: [heap|staticTable] [true|false]
   virtual     - Enables/disables virtualization searching for triples in the heap or the static table. Warning: the alternative to virtualization is naive and slow.
                 - parameter: [heap|staticTable] [true|false]
   source      - Set which sources to include (true) or exclude (false) when querying
                 - parameter: [heap|staticTable|vocabularyFile|externalOntology] [true|false]reasoner    - Specify which Jena reasoner to use, or turn it off
                 - parameter: [off|rdfs|owl]
   query       - executes a SPARQL query
                 - parameter: SPARQL query
   plot        - plot ROLE PORT FROM TO runs gnuplot on port PORT of role ROLE from FROM to TO. FROM and TO are optional
                 - parameter: 
   consistency - prints all classes and checks that the internal ontology is consistent
   class       - returns all members of a class
                 - parameter: class expression in Manchester Syntax, e.r., "<smol:Class>"
   eval        - evaluates a .smol expression in the current frame
                 - parameter: a .smol expression
   verbose     - Sets verbose output to on or off
                 - parameter: `true` or `on` to switch on verbose output, `false` or `off` to switch it off



