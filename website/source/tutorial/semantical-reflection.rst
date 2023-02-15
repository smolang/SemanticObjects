Semantically Reflected Digital Twins
====================================

Self-Adaptation
--------------------------

Taking into consideration the architectures and ideas presented above we highlight that through them it is possible to:

* **Access the sensors** of the physical system (FMI)
* Access the **structure of the physical system** (SWT), and
* **Simulate** the digital design (FMI)

In other words:

* We can **compare simulations** (DT) **to data from sensors** (PT)
* On **changes** of the PT perceived by sensors, the **system adapts** to reflect such changes in the simulation

Self-adaptation means to **automatically reestablish some property** of a system, by **reacting to outside stimuli**. For Digital Twins, the “outside” is the physical system.

There are two kinds of self-adaptation to reestablish the twinning property.

Behavioral self-adaptation
^^^^^^^^^^^^^^^^^^^^^^^^^^

It's the case of a behavioral self-adaptation when the **simulated** (expected) **behavior** of certain components does **not match** the **real** (measured) **behavior** of the system, measured trough sensors.

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

Structural self-adaptation
^^^^^^^^^^^^^^^^^^^^^^^^^^

It's the case of a structural self-adaptation when the **simulated** (represented by a knowledge graph) **structure** of the system does not match its **real** (expressed in the asset model) **structure**.

In order to do this we need to **express the program structure** in a **formal way**, so we can **uniformly access it** together with the asset model. This could be done with the use of semantic web technologies. But how to apply them on programs?

We can use **semantical lifting**, a mechanism to automatically generate the knowledge graph of a program state.

To self adapt we must, first, **detect** the broken twinning and then **repair** it.

#. We **access** PT structure through asset model
    * Changes of PT are visible in asset model
    * Asset model accessible directly to DT
#. **Detect** changes through **combined knowledge graph** (PT + DT)
    * From the combination of PT and DT we can extract all the information needed to repair and perform the adaptation

..
    TODO add image from demo_day2 slide 39


MAPE-K
------

.. admonition:: **MAPE-K**
    :class: note

    MAPE-K is an established conceptual framework to structure self-adaptive systems.

This framework expects a structure formed by a **K**\ nowledge component which keeps track of information and goals for the self-adaptation loop.

In particular its tasks are to:

* **M**\ onitor the situation of the system
* **A**\ nalyze whether the situation requires adaptation (structure/behavior of digital and physical twins is different)
* **P**\ lan the adaptation
* **E**\ xecute the plan

..
    TODO: add image from demo_day2 slide 27


Semantically Lifted States
--------------------------

A **semantically lifted program** can interpret its own program state as a **knowledge graph** and reflect on itself through it.

A specific program state ``conf`` can be **lifted** to create a **knowledge graph** representing it.

..
    TODO: add image from demo_day2 slide 42

A change in the **program state** determines an **equivalent change** in the **knowledge graph**. In other words determines an update of the representation of the program state itself.

..
    TODO: add image from demo_day2 slide 46

Example
^^^^^^^

In this example we can see a SMOL program in which is created a class ``C`` composed by:

* An integer field ``i``
* A method ``inc()`` that increments ``i`` by 1

A ``main`` in which:

* A new object named ``c`` of type ``C`` is created
* ``c.inc`` is executed the variable named ``i`` is incremented

.. code-block::

    class C (Int i)
        Unit inc() this.i = this.i + 1; end
    end
    main
        C c = new C(5);
        c.inc();
    end

..
    TODO: correct code into slides

The program state can be **formalised in RDF** and expressed in the form of triples. Some examples could be:

* ``C`` is a class and has a field named ``i``
    * ``prog:C a prog:class . prog:C prog:hasField prog:C_i .``
* ``C`` has a mehod named ``inc``
    * ``prog:C prog:hasMethod prog:C_inc .``
* ``inc`` has a Body ``s`` (and it could be described in more detail) 
    * ``prog:inc prog:hasBody prog:s ;``
* ``obj1`` is an instance of ``C`` with ``i`` initialised to 5
    * ``run:obj1 a prog:C . run:obj1 prog:C_i 5 .``
* in a certain moment the stack top has a frame ``frame1`` which is the execution of the ``inc`` method
    * ``run:stack run:top run:frame1 . run:frame1 run:executes prog:inc.``


SMOL
----

Semantical lifting and reflection is implemented in the Semantic Micro
Object Language, `smolang.org`_

.. _smolang.org: https://www.smolang.org.

Given the **lifted state**, we can use it for multiple operations:

* **Access it** to retrieve objects without traversing pointers
* **Enrich it** with an ontology, perform logical reasoning and retrieve objects using a query using the vocabulary of the domain.
* **Combine it** with another knowledge graph and access external data based on information from the current program state.


SMOL also permits to **query knowledge base** by using SPARQL query language.
In the following example the program retrieve a list of Overloaded servers from a knowledge base and use it to perform some operation. In other words it extracts some information from a set of RDF triples. 

.. code-block::
    
    class Server(List<Task> taskList) ... end
        class Scheduler(List<Platform> serverList)
            Unit reschedule()
                List<Server> l := access("SELECT ?x WHERE {?x a :Overloaded}");
                this.adapt(l);
            end
        end

However we need a **formal definition** of what an Overloaded server is.
This can be done again using Semantic Technologies to express that an Overloaded server is a Server which has at least 3 tasks in the ``taskList``.

.. code-block::

    :Overloaded
        owl:equivalentClass [
            owl:onProperty (:taskList, :length);
        owl:minValue 3;
    ].


DEMO - Semantic reflection
--------------------------
	
In this example we see how to:

* Monitor consistency
* Monitor twinning
* Adapt to addition of new rooms

We will use the House assets use-case based on SMOL language.

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
In SMOL the **Functional Mock-Up Objects (FMOs)** provide a tight integration of simulation units (FMU) using FMI into programs.

.. code-block::

	Cont[out Double val] shadow =
		simulate("Sim.fmu", input=sys.val, p=1.0);
	Cont[out Double val] sys = simulate("Realsys.fmu");
	Monitor m = new Monitor(sys,shadow); m.run(1.0);

In this example ``shadow`` and ``sys`` are FMOs (Cont). These allow to **map the FMU to a SMOL Object** allowing also to interact with the FMU itself. See `smolang documentation about FMOs`_  for more details.

.. _smolang documentation about FMOs: https://smolang.org/language/fmos.html.

**Integration of FMOs in SMOL**

* The type of FMO is directly checked against model description
* Variables of FMU become fields, functions become methods
* Causality reflected in type

**Functional Mock-Up Interface (FMI)** is a standard for (co-)simulation units, called function mock-up units (FMUs). Can also serve as interface to sensors and actuators.

.. code-block::

	//simplified shadow
	class Monitor(Cont[out Double val] sys,
		Cont[out Double val] shadow)
		Unit run(Double threshold)
		while shadow != null do
			sys.doStep(1.0); shadow.doStep(1.0);
			if(sys.val - shadow.val >= threshold) then ... end
		end ...

This SMOL example shows a system ``sys``, which is twinned by another FMO ``shadow``. 
When the difference between the two objects (sys.val - shadow.val) exceeds a threshold some events are triggered (e.g. Self adaptation).

**FMOs are objects**, so they are **part of the knoledge graph** once the program state is **lifted**.

.. code-block::
    
    class Monitor(Cont[out Double val] sys,
                    Cont[out Double val] shadow)

This class Monitor takes two FMOs as parameters. The first one is the system to be monitored ``sys`` and the second one is its shadow ``shadow``.
The result of the **semantical lifting** of this program is the following.

..
    TODO add image from demo_day2 slide 60

Using the Semantical Lifting
----------------------------

We can use **SPARQL** to **query** the program state and the knowledge base, thus **checking if domain constraints are met**.

Taking the house assets example into consideration we could:

* **Query** the program state to check if the **house setup is consistent** (e.g. there should be no rooms that are both left and right of a controller)

.. code-block::

    SELECT ?x WHERE { 
        ?ctrl a prog:Controller.
        ?ctrl prog:Controller_left ?room.
        ?ctrl prog:Controller_right ?room 
    }

* **Query** to check **structural consistency** for heaters:

.. code-block::

    SELECT ?x WHERE { 
        ?o1 prog:Room_id ?id1. ?h1 asset:id ?id1.
        ?o2 prog:Room_id ?id2. ?h2 asset:id ?id2.
        ?h1 htLeftOf ?h2.
        ?c a prog:Controller.
        ?c prog:Controller_left ?o1.
        ?c prog:Controller_right ?o2
    }


Demo - Inconsistent twinning
----------------------------

**Detecting Structural Drift**

The two previous **SPARQL** queries can detect that some **mismatch** between asset model and program state exists. How to detect **where** the mismatch is and how to **repair** it?

Solution: MAPE-K loop

* Retrieve all assets, and their connections by id (**Monitor**)
* Remove all ids present in the digital twin
* If any id is left, assets needs to be twinned (**Analyze**)
* Find kind of defect to plan repair (**Plan**)
* Execute repair according to connections (**Execute**)
* Monitor connections using previous query
* (And v.v. to detect twins that must be removed)

Example: Adding a New Room
--------------------------

* Get all (asset) rooms and their neighboring walls
* Remove all (twinned) rooms with the same id
* Use the information obtained from the query to check if there are rooms in the asset model not represented in the twin model (so we can add them)
* Assumption: at least one new room is next to an existing one

..
    missing part in previous point 3 in demo_day2 slide 65, check if correct

.. code-block::

    class RoomAsrt(String room, String wallLt, String wallRt) end
    ...
    List<RoomAsrt> newRooms =
        construct(" SELECT ?room ?wallLt ?wallRt WHERE
        { ?x a asset:Room;
            asset:right [asset:Wall_id ?wallRt];
            asset:left [asset:Wall_id ?wallLt];
            asset:Room_id ?room.
            FILTER NOT EXISTS {?y a prog:Room; prog:Room_id ?room.} }");


Demo - Repair
-------------

**Assumptions**

* We know all the **possible modifications up-front** E.g., how to deal with a heater getting new features?
* We know how to always **correct structural drift**
* Changes **do not happen faster** than we can repair

Monitoring is still needed to (a) **ensure** that repairs work correctly, and (b) **detect loss** of twinning due to, e.g., unexpected structural drift.


Summary
-------

**Digital Twins and the FMI**

Digital twins are computer simulations of a physical system. They are used to monitor and control the physical system. Its functioning is based on an interconnection between physical assets and digital models.

**Semantic Lifting and Asset Models**

Semantic lifting allows to represent the program state as a **knowledge graph.** The asset model can then be **combined** with the program state to query **verify** certain properties.

**Structural Self-Adaptation**

It's based on th use of semantic technologies to **query** and **monitor** combined knowledge graph from asset model and program state. This allows to **detect** structural drift and **repair** it.

What have we used to construct a self-adaptive, semantically reflected Digital Twin?

**Technologies**

* Semantic Web technologies
    * OWL/Protege
    * RDF, SPARQL
* Physical modeling, interfacing
    * Modelica, FMI
* SMOL

**Main concepts we explored**

* Digital Twins
* Self-Adaptation through MAPE-K loop
* Semantically lifted programs
* Asset models


Current Research Questions
--------------------------

**Digital Twins and Formal Methods**

* How to use the fully formal setting for static analysis?
* How to generate digital twins automatically?
* How to deal with concurrency?

**Digital Twins@UiO**

If you are interested in semantic technologies for programs or digital twins, contact us under

* einarj@ifi.uio.no
* sltarifa@ifi.uio.no
* rudi@ifi.uio.no
* eduard@ifi.uio.no