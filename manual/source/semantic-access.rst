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
*access with sparql*
*construct with sparql*

Shape Access
^^^^^^^^^^^^
*validate with shacl*

Concept Access
^^^^^^^^^^^^^^

*member with dl*

Time Series Access
------------------

While not semantic, a syntactically similar mechanism is available to query data from `InfluxDB <https://www.influxdata.com/>`_ databases.
Syntactically, one passes different parameters to the ``access`` statement.
The first parameter is a path to a ``String``-literal containing a InfluxQL query, the second parameter is a mode of the form ``INFLUXDB(StringLiteral)``,
where the parameter of the mode is a ``String``-literal containing a path to a `YAML <https://yaml.org/>`_ configuration to connect to the InfluxDB endpoint.
In this case, the result is always a ``List`` of ``Double``s.
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
