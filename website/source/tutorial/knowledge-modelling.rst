Modelling Knowledge using Semantic Technologies
===============================================

Semantic Technologies
----------------------

Knowledge can be described ad hoc or in a structural manner. Semantic Technologies facilitate the description of structured knowledge, consistency checking and reasoning.

RDF (Resource Description Framework)
-------------------------------------

In the semantic technology context, information is expressed by triples (facts), which consists of a *subject*, a *predicate* and an *object*.

Example:

.. figure:: /images/rdf-example.svg
   :align: center
   :alt: RDF example

Alice (id 111) is a Person. She has a child named Bob (id 222) who is also a Person. Bob has a child named Charlie (id 333) who is also a Person.
Here we see multiple facts, each of which is a triple. The subject is the entity that is being described (Alice id 111), the predicate is the property of the subject (having a child), and the object is the value of the property (Bob id 111).


OWL (Web Ontology Language)
---------------------------

It's a knowledge representation language used to build ontologies.
- Ontologies are logics for knowledge representation

Example:

.. figure:: /images/owl-example.svg
   :align: center
   :alt: OWL example

.. math::
    \forall x \exists y \exists z . hasChild(x, y) \land hasChild(y, z) \land Person(z) \implies GrandParent(x)
    
..
    TODO: check if formatted correctly

hasChild **some** (hasChild **some** Person) **subClassOf** GrandParent

- Ontologies represent knoledge that is incremented over time

SPARQL (SPARQL Protocol and RDF Query Language)
-----------------------------------------------

It's a query language for databases stored in RDF format. 

.. figure:: /images/owl-example.svg
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

* In the engineering domain it is common practice to build asset models to support, e.g., maintenance, operations, design etc
* There are currently several industry initiatives that endorse the use of ontologies for asset modelling, e.g., in the Industry 4.0

.. admonition:: Asset models & digital twins
    :class: note

    Assets models are any **object of interest** in a digital twin. They provide the twin with **knowledge** about the **static structure** that can be **used for the twin’s simulation model**

The next example is the modelling of an house by exploiting semantic technologies illustrated before.
We can create a representation of the house (in all his parts) by the means of RDF triples.
In this way there is a **formal** representation of it that can be interpreted/understood by a computer in order to obtain meaningful information from it.

House Asset Use Case
^^^^^^^^^^^^^^^^^^^^

.. figure:: /images/house-asset-use-case.svg
   :align: center
   :alt: House Asset Use Case

Download the house asset model from: https://github.com/smolang/SemanticObjects/blob/master/examples/tutorialfiles.zip
File: house.ttl

example of SPARQL in SMOL
-------------------------

The following is an example of the usage of SPARQL in SMOL code.

.. code-block::

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
