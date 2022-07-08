.. _semantic-access:

Semantic Access
===============

*General Introduction*

Semantic Lifting
----------------

*General introduction*

Ontology
^^^^^^^^

*How does it look like, link to it, .ttl file*

Virtualization and Domain Knowledge
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

*What is generated, what is input, some example*

Semantic Reflection
-------------------

*general introduction*

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
