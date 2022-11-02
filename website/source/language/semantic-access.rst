.. _semantic-access:

Semantic Access
===============

The SMOL interpreter implements *semantic lifting*, the process of generating
a knowledge graph from the current program state.  The generated knowledge
graph can be investigated by external tools after creating a ``.ttl`` file via
the REPL ``dump`` command, queried during breakpoints using the REPL commands
listed in :ref:`querying-smol`, or accessed within the running program itself
via special statements for *semantic reflection* through OWL concepts, SHACL
shapes or SPARQL queries.

Semantic Lifting
----------------

Ontology and Virtualization
^^^^^^^^^^^^^^^^^^^^^^^^^^^

The generated knowledge graph is generated from the following sources:

* The *SMOL ontology* defining the general vocabulary and basic axioms for states.
* The *class definitions* of the SMOL program.
* The knowledge generated from the current *runtime state* (object instances,
  heap and stack).
* A user-defined *domain ontology*, if supplied.

The SMOL ontology and domain ontology are given as files, while the heap and
class table are virtualized, i.e., the knowledge graph is built on demand.  If
parts of the knowledge graph are not needed by the current query, they are not
generated.

The generated knowledge graph can be accessed using SPARQL (using the `Apache
Jena reasoner <https://jena.apache.org/documentation/inference/>`_), SHACL
(also using the Apache Jena model) or an OWL/DL concept (using `HermiT
<http://www.hermit-reasoner.com/>`_).  The general structure is pictured
below:

.. figure:: /images/alone.png 

*What is generated, what is input, some example*

Semantic Reflection
-------------------

*general introduction*

.. _modeling-bridge:

Modeling Bridge
^^^^^^^^^^^^^^^

To connect with the domain model, SMOL implements the modeling bridge, a mechanism to manually add axioms over the lifted SMOL objects.
A simple modeling bridge takes as a parameter a string containing a `predicate object list <https://www.w3.org/TR/turtle/#grammar-production-predicateObjectList>`_ in turtle syntax.
The IRI of the lifted objects will be attached as the subject for the predicate object list.

A complex modeling bridge is a sequence of guarded modeling bridges, where the guard is an expression over the fields of an object.
The guarded predicate object list is used if the corresponding guard evaluated to true. Guarded modeling bridges are evaluated from the top until a guard evaluates to true, the remaining part is skipped. A complex modeling bridge must end with a simple modeling bridge as the default.

A simple modeling bridge can be annotated either to an object creation or to a class. 
A complex modeling bridge can annotated only to classes.
If a modeling bridge is given for a class and an object creation of this class, the bridge of the object
overrides the one of the class.
::
  
  MODELS ::= SIMPLE_MODELS | COMPLEX_MODELS ;
  SIMPLE_MODELS  ::= `models` STRING `;`;
  COMPLEX_MODELS ::= `models` `(` Expression `)` STRING `;` MODELS;


Example

.. code-block:: Java

  class C (Int i) 
    models (this.i > 0) "a :containsPositive";
    models "a :containsNonPositive";
  end

  main
    C c = new C(5);
    C d = new C(0);
    C e = new C(4) models "a :special";
  end

The lifting will contain the following axioms:

.. code-block:: none

  run:obj1 prog:C_i 5;
           a prog:C.
  run:obj2 prog:C_i 0;
           a prog:C.
  run:obj3 prog:C_i 4;
           a prog:C.
  run:obj1 a :containsPositive.
  run:obj2 a :containsNonPositive.
  run:obj3 a :special.

Crossing the Modeling Bridge
""""""""""""""""""""""""""""

Additionally to adding axioms, the ``domain`` modifier can be used to create a *modelled* object.
If a class contains a field or method modified by ``domain``, then each SMOL object is lifted to *two* objects in the knowledge graph,
the standard one using the ``run:`` prefix (the modelling object), and a second one using the ``domain:`` prefix (the modelled object). The second object is envisaged to be the corresponding 
object in the domain, modelled by the created object. The two objects are connected using the ``smol:models`` property.

If a field is annotated with ``domain``, then it is lifted for the modelled object. Its prefix is also changed to ``domain``.
The same holds for methods (see CSSA.). The modelled object is only created during lifting if at least one field or method is annotated with ``domain``.

.. code-block:: Java

   class C(Int i, domain Int j) end
   main 
     C c = new C(5, 4);
   end

The lifting of the created object is as follows. 

.. code-block:: none

   run:obj1 a prog:C.
   run:obj1 prog:C_i 5.
   run:obj1 domain:models domain:obj1.
   domain:obj1 domain:C_i 4.

Ignoring Fields
"""""""""""""""

To exclude certain fields in a class from being lifted, they can be annotated with the ``hidden`` modifier.
The field will be completely ignored during lifting: neither general axioms nor instances are generated.
The ``hidden`` modifier does not interact with the visibility modifiers
If the field is of object-type, the object it points to will still be lifted.

.. code-block:: Java

  class C (Int i, hidden C j) end
  main
    C c = new C(5,null);
    C d = new C(6, c);
  end

The lifting will contain the following axioms. Note that ``prog:C_j`` is not mentioned.

.. code-block:: none

   prog:C a smol:Class.
   prog:C_i a smol:Field.
   prog:C smol:hasField prog:C_i.

   run:obj1 a prog:C;
            C_i 5.
   run:obj2 a prog:C;
            C_i 6.


Computational Semantic State Access
"""""""""""""""""""""""""""""""""""

Methods annotated with ``rule`` generate additional triples during lifting. 
To this end, for each created object with such a method, the method is executed *in the current state* the return value of the execution is then added to the knowledge graph.
The used property has the name ``prog:<class>_<method>_builtin_res``.

.. code-block:: Java

  class C(Int i) 
        rule Int double() return this.i*this.i;
  end
  main
    C c = new C(5);
  end


The lifting will generate the following axioms.

.. code-block:: none

   prog:C a smol:Class.
   prog:double a smol:Method.
   prog:C smol:hasMethod prog:double.
   prog:C_double_builtin_res a owl:ObjectProperty;
                             rdfs:domain prog:C;
                             rdfs:range xsd:integer.

   run:obj1 a prog:C.
   run:obj1 prog:C_i 5.
   run:obj1 prog:C_double_builtin_res 25.

A ``rule`` method is not allowed to have side-effects (except exceptions), the following restrictions are statically checked:

* It cannot have parameters.
* It cannot create objects.
* It cannot call non-``rule`` methods.
* It cannot write into any fields.


Query Access
^^^^^^^^^^^^

Query access retrieves data from the lifted knowledge graph using queries.

Retrieving a list of literals or lifted objects is done via the ``access`` top-level expression.
It takes as its first parameter a ``String``-literal containing an extended `SPARQL <https://www.w3.org/TR/sparql11-overview/>`_ query, which additionally may contain non-answer variables of the form ``%i`` for some strictly positive number ``i``. The set of numbers for the non-answer variables must form an interval [1,n] for some n.
Additionally, the top-level expression takes a list of expressions of the length n.

At runtime, these expressions are evaluated and the result is syntactically substituted for the corresponding non-answer variable.
The SPARQL query is then executed and the results of the ``?obj`` variable are then translated into a list.
For example, the following retrieves all objects ``o`` of type ``C`` with ``o.aCB.aB.sealing = x``.
::

   List<C> l = access("SELECT ?obj WHERE {?obj prog:C_aCB ?b. ?b prog:B_aB ?a. ?a prog:A_sealing %1 }", this.x);

The execution fails if any answer variable than ``?obj`` is used for retrieval, the elements are not literals or IRIs of lifted objects,
or mixes literals of lifted objects. The compiler outputs a warning if the SPARQL query cannot be shown to always return a list of elements of the type of the target variable.

.. NOTE::
   The query must be tree shaped for type-checking.

Constructing a list of *new* objects from a SPARQL query is done via the ``construct`` top-level expression.
Its parameters are as the one of the ``access`` top-level expression, but the variables are handled differently:
Each variable must have the name of a field of the type of the target location. For each field there must be one variable. All fields must be of primitive data type.
::

   class C(Int j1, Int j2) end
   ...
   List<C> v = construct("SELECT ?j1 ?j2 WHERE { ?y a prog:B. ?y prog:B_i2 ?j2.?y prog:B_a ?x.?x a prog:A. ?x prog:A_i1 ?j1 }");

.. NOTE::
   For a mechanism to load data into classes with structure, i.e., field of class types, see the *advanced semantic access* section below.

Shape Access
^^^^^^^^^^^^

Shape access validates the correctness of the lifted knowledge graph with respect to a graph shape using the top-level expression ``validate(Literal)``.
The parameter must be a ``String``-literal containing a path to `SHACL <https://www.w3.org/TR/shacl/>`_ shapes in `turtle <https://www.w3.org/TR/turtle/>`_ syntax.
::

   Boolean b  = validate("examples/double.ttl");

The execution fails if the file does not accessable or the SHACL shapes are mal-formed.

Concept Access
^^^^^^^^^^^^^^

Concept access retrieves the list of objects described by an OWL concept using the top-level expression ``member(Literal)``.
The parameter must be a ``String``-literal containing a concept in `Manchester syntax <https://www.w3.org/TR/owl2-manchester-syntax/>`_.
For example, the following retrieves all members of class ``C`` that model some domain concept ``domain:D``.
::

  List<C> list := member("<domain:models> some <domain:D>");

The execution fails if the concept is either mal-formed or contains elements that are not IRIs of lifted objects.

.. NOTE::
   Currently, type checking of concept access is not supported.

Time Series Access
------------------

While not semantic, a syntactically similar mechanism is available to query data from `InfluxDB <https://www.influxdata.com/>`_ databases.
Syntactically, one passes different parameters to the ``access`` statement.
The first parameter is a path to a ``String``-literal containing a InfluxQL query, the second parameter is a mode of the form ``INFLUXDB(StringLiteral)``,
where the parameter of the mode is a ``String``-literal containing a path to a `YAML <https://yaml.org/>`_ configuration to connect to the InfluxDB endpoint.
In this case, the result is always a ``List`` of ``Double`` values.
::

  main
    List<Double> list := access(
    "from(bucket: \"petwin\")
      |> range(start: -1h, stop: -1m)
      |> filter(fn: (r) => r[\"_measurement\"] == \"chili\")
      |> filter(fn: (r) => r[\"_field\"] == \"temperature\")
      |> filter(fn: (r) => r[\"name\"] == \"faarikaal1\")
      |> aggregateWindow(every: 5m, fn: mean, createEmpty: false)
      |> yield(name: \"mean\")",
    INFLUXDB("petwin.yml"));
    print(list.content);
  end

.. NOTE::
   Currently, only InfluxQL queries with a single return variable are supported. Influx-mode ``access`` statements are not type-checked.



Advanced Semantic Access
------------------------

.. WARNING::
   The following section describes a feature that is on active development on a feature branch (``lazy``) and is not available on the master branch.

`Advanced query access in SMOL <https://doi.org/10.1007/978-3-031-06981-9_12>`_ is a tight coupling between classes and the query that retrieves its contents from an external database.
To this end, a class can be annotated with a *retrieval query*, and a special statement loads all elements of this class through this query, possibly refined with a restriction. 
Furthermore, we enable lazy loading for retrieval queries: 
if a class ``C`` refers to another class ``D`` through a field ``f``, then the query of the second class ``D`` is only executed if the field ``f`` is accessed.


Retrieval Queries
^^^^^^^^^^^^^^^^^

*retrieve, anchor*

Lazy Loading
^^^^^^^^^^^^

*QFut etc.*
