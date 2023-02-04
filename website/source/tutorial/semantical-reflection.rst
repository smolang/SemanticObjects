Semantically Reflected Digital Twins
====================================

Self-Adaptation
--------------------------

* We can access the sensors of the physical system (FMI)
* access the structure of the physical system (SWT), and
* simulate the digital design (FMI)

In other words:

* We can compare simulations (DT) to sensors (FT)
* On changes of the FT perceived by sensors, the system adapts to reflect such changes in the simulation

Self-adaptation means to automatically reestablish some property of a
system, by reacting to outside stimuli. For Digital Twins, the “outside”
is the physical system.

Two kinds of self-adaptation to reestablish the twinning property.

Behavioral self-adaptation
^^^^^^^^^^^^^^^^^^^^^^^^^^

Simulated (=expected) behavior of certain components does not match
the real (=measured) behavior of the sensors.

This can be caused by multiple reasons, such as:
    * Sensor drift
    * Modeling errors
    * Faults
    * Unexpected events

In order to solve this unintended behavior you can:
    * Monitor sensors
    * Analyze the relation to simulation
    * Plan repair by, e.g., finding new simulation parameters
    * Exchange simulators or send signal to physical system

Structural Self-adaptation
^^^^^^^^^^^^^^^^^^^^^^^^^^

Simulated (= lifted) structure of digital system does not match real (= expressed in asset model) structure.

We need to express the program structure, so we can uniformly access it
together with the asset model. How to apply semantic web technologies
on programs?

**Semantical lifting** a mechanism to automatically generate the
knowledge graph of a program state.

MAPE-K
------

.. admonition:: **MAPE-K**
    :class: note

    MAPE-K is an established conceptual framework to structure selfadaptive systems.

This framework expects a structure formed by a **K**\ nowledge component which keeps track of information and goals for the self-adaptation loop.

In particular its tasks are to:
* **M**\ onitor the situation of the system (Digital Twin (?))
* **A**\ nalyze whether the situation requires adaptation
* **P**\ lan the adaptation
* **E**\ xecute the plan

..
    TODO: add image from demo_day2 slide 27


Semantically Lifted States
--------------------------

A semantically lifted program can interpret its own program state as a knowledge graph and reflect on itself through it.

A specific program state ``conf`` can be *lifted* to create a *knowledge graph*.

..
    TODO: add image from demo_day2 slide 42

A change in the program state determines an equivalent change in the knowledge graph and so an update on the representation of the program state itself.

..
    TODO: add image from demo_day2 slide 46

..
    TODO: ask if correct


Example
^^^^^^^

In this example we can see a small SMOL (xd lol) program in which is created a class ``C`` composed by:
    * An integer field ``i``
    * A method (Unit) ``inc()`` that increments ``i`` by 1

A ``main`` in which:
    * A new object named ``c`` of type ``C`` is created
    * ``c.inc`` is executed and the return saved in a variable named ``i``

.. code-block::

    class C (Int i)
        Unit inc() this.i = this.i + 1; end
    end
    main
        C c = new C(5);
        Int i = c.inc();
    end

The program state can be formalised in RDF and expressed in the form of triples. Some examples could be:

    * ``C`` is a class and has a field named ``i``
        * ``prog:C a prog:class. prog:C prog:hasField prog:C_i.``
    * ``C`` has a mehod named ``inc``
        * ``prog:C prog:hasMethod prog:C_inc.``
    * ``inc`` has a Body ``s`` (and it could be described in more detail) 
        * ``prog:inc prog:hasBody prog:s;``
    * ``obj1`` is an instance of ``C`` with ``i`` initialised to 5
        * ``run:obj1 a prog:C. run:obj1 prog:C_i 5.``
    * in a certain moment the stack top has a frame ``frame1`` which is the execution of the ``inc`` method
        * ``run:stack run:top run:frame1. run:frame1 run:executes prog:inc.``


SMOL
----

Semantical lifting and reflection is implemented in the Semantic Micro
Object Language, smolang.org.

Given the lifted state, we can use it for multiple operations:
* **Access it** to retrieve objects without traversing pointers
* **Enrich it** with an ontology, perform logical reasoning and retrieve
objects using a query using the vocabulary of the domain.
* **Combine it** with another knowledge graph and access external data
based on information from the current program state.

..
    TODO: check correctness of next part until end of section

SMOL also permits to query knowledge base by using SPARQL query language.
In the following example the program retrieve a list of Overloaded servers from a knowledge base and use it to perform some operation. In other words it extracts some information from a set of RDF triples. 

.. code-block::

    class Server(List<Task> taskList) ... end
    class Scheduler(List<Platform> serverList)
		Unit reschedule()
			List<Server> l := access("SELECT ?x WHERE {?x a :Overloaded}");
			this.adapt(l);
    	end
	end

However we need a formal definition of what an Overloaded server is.
This can be done again using Semantic Technologies to express that an Overloaded server 
is a Server which has at least 3 tasks in the ``taskList``

.. code-block::

    :Overloaded
        owl:equivalentClass [
            owl:onProperty (:taskList, :length);
        owl:minValue 3;
    ].


DEMO - Semantic reflection
--------------------------
	
In this example we will learn how to:
* Monitor consisten
* Monitor twinning
* Adapt to addition of new rooms
Using the SMOL language.

We will use the House assets use-case

.. figure:: /images/house-asset-use-case_2.svg
    :align: center
    :alt: House Assets Use Case

Model Description
^^^^^^^^^^^^^^^^^

.. code-block:: xml

    <fmiModelDescription fmiVersion="2.0" modelName="Example" ...>
        <CoSimulation needsExecutionTool="true" .../>
        <ModelVariables>
            <ScalarVariable name="p" variability="continuous" cusality="parameter">
                <Real start="0.0" />
            </ScalarVariable>
            <ScalarVariable name="input" variability="continuous" causality="input">
                <Real start="0.0" />
            </ScalarVariable>
            <ScalarVariable name="val" variability="continuous" causality="output" initial="calculated">
                <Real/>
        </ModelVariables>
        <ModelStructure> ... </ModelStructure>
    </fmiModelDescription>

SMOL and FMI
------------
**Functional Mock-Up Objects (FMOs)**
Tight integration of simulation units using FMI into programs.

.. code-block::

	Cont[out Double val] shadow =
		simulate("Sim.fmu", input=sys.val, p=1.0);
	Cont[out Double val] sys = simulate("Realsys.fmu");
	Monitor m = new Monitor(sys,shadow); m.run(1.0);
	
**Integration of FMOs in SMOL**

* Type of FMO directly checked against model description
* Variables become fields, functions become methods
* Causality reflected in type

**Functional Mock-Up Interface (FMI)**

Standard for (co-)simulation units, called function mock-up units
(FMUs). Can also serve as interface to sensors and actuators.

.. code-block::

	//simplified shadow
	class Monitor(Cont[out Double val] sys,
		Cont[out Double val] shadow)
		Unit run(Double threshold)
		while shadow != null do
			sys.doStep(1.0); shadow.doStep(1.0);
			if(sys.val - shadow.val >= threshold) then ... end
		end ...

This SMOL example shows a system (sys), which is twinned by a shadow object (shadow). 
When the difference between certain valutes of two objects exceeds a threshold, SMOL reacts triggering certain events.