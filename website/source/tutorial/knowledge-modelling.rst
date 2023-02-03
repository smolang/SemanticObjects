Modelling Knowledge using Semantic Technologies
===============================================

Semantic Technologies
----------------------

Knowledge can be described ad hoc or in a structural manner. Semantic Technologies facilitate the description of structured knowledge, consistency checking and reasoning

RDF (Resource Description Framework)
-------------------------------------

Data in expressed using triple pattern, which consists of a *subject*, a *predicate* and an *object*.

Example:

.. figure:: images/rdf-example.svg
   :align: center
   :alt: RDF example

Here 'Alice' is subject, 'a' is predicate, 'Person' is object, 'Alice' is subject,
'id' is predicate, '111' is object, ...

..
    TODO: what does this mean?

OWL (Web Ontology Language)
---------------------------

It's a knowledge representation language used to build ontologies.
- Ontologies are logics for knowledge representation

Example:

.. figure:: images/owl-example.svg
   :align: center
   :alt: OWL example

.. math::
    \forall x \exists y \exists z \dot hasChild(x, y) \and hasChild(y, z) \and Person(z) \longrightarrow GrandParent(x)
    
..
    TODO: check if formatted correctly

hasChild **some** (hasChild **some** Person) **subClassOf** GrandParent

- Ontologies represent knoledge that is incremented over time

SPARQL (SPARQL Protocol and RDF Query Language)
-----------------------------------------------

It's a query language for databases stored in RDF format.

.. figure:: image/owl-example.svg
   :align: center
   :alt: OWL example

.. code-block:: sparql
    
    SELECT ?x WHERE { ?x a :Person }
    SELECT ?x ?y WHERE { ?x a :Person. ?x :hasChild ?y }
    SELECT ?x WHERE { ?x a :GrandParent }

..
    TODO: add example in Protégé image

Asset modelling
---------------

.. admonition:: **Asset model in the engineering domain**
    :class: note

    An asset model is an **organized, digital description** of the composition and properties of an **asset**.

* In the engineering domain it is common practice to build
asset models to support, e.g., maintenance, operations, design etc
* There are currently several industry initiatives that endorse the use of ontologies for asset modelling, e.g., in the Industry 4.0

.. admonition:: Asset model in the engineering domain
    :class: note

    Assets models are any **object of interest** in a digital twin. They provide the twin with **knowledge** about the **static structure** that can be **used for the twin’s simulation model**

The next example is the modelling of an house by exploiting semantic technologies illustrated before.
We can create a representation of the house (in all his parts) by the means of RDF triples.
In this way there is a **formal** representation of it that can be interpreted/understood by a computer in order to obtain meaningful information from it.

House Asset Use Case
^^^^^^^^^^^^^^^^^^^^

.. figure:: images/house-asset-use-case.svg
   :align: center
   :alt: House Asset Use Case

Download the house asset model from: https://github.com/smolang/SemanticObjects/blob/master/examples/tutorialfiles.zip
File: house.ttl

example of SPARQL in SMOL
-------------------------

The following is an example of the usage of SPARQL in SMOL code.

.. code::
    main
        List<Int> results = access(
            "SELECT ?obj {
                ?a a asset:Room.
                ?a asset:id ?obj
            }";
        )
        while results != null do
            Int current = results.content;
            results = results.next;
            println(current);
        end
    end

