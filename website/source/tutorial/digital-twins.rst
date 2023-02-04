Digital Twins Introduction: Concepts and Engineering Perspectives
=================================================================

Digital twins are an emerging, enabling technology for industry to transition to
the next level of digitisation.

They are meant to understand and control assets in nature, in industry and in 
society at large.

They were originally conceived at NASA for the space program, they have emerged 
as an engineering discpline, based on best practices.


What is a digital twin
----------------------

.. admonition:: NASA’s definition of a DT
    :class: note
    
    “an integrated multi-physics, multi-scale, probabilistic simulation
    of a vehicle or system that uses the best available physical models,
    sensor updates, fleet history, etc., to mirror the life of its flying
    twin. It is ultra-realistic and may consider one or more important
    and interdependent vehicle systems”

A Digital Twin (DT) is a live replica of a Physical System (Physical Twin, PT).
DT is connected to PT in near real-time via data-streams (e.g. by sensors). 
DT takes information from the PT, uses them to do observations and take 
decisions/actions to be performed on the PT.

A Digital Twin integrates aspects of models and control systems.

.. 
    TODO: add image from demo_day1 slide 15 

Lifecycle Management
----------------------

Digital Thread: The digital twin is meant to adapt, as the underlying assets evolve with time.

This allows to:
#. Connect the designs, requirements and software that go into the system represented by the DT
#. Connect the different phases of the system to the DT: design, development, operation, decommissioning, . . .

Thus proposing a new Software Engineering paradigm:
* **Models are an integrated part of the system**, as they are also used in SE phases following the system design phase.
* The purpose of the system is **building software to maintain models**, instead of building models to maintain software.
* Changes in the assets trigger **model evolutions**.
* **CPS in-the-large**: distributed, heterogeneous


Conceptual Layers of a Digital Twin
-----------------------------------

.. figure:: /images/conceptual-layers.svg
    :align: center
    :alt: Conceptual Layers

- **Descriptive:** Insight into the past ("what happened?" scenarios)
- **Predictive:** Understanding the future ("what may happen?" scenarios)
- **Prescriptive:** Advise on possible outcomes ("what if?" scenarios)
- **Reactive:** Automated decision making

.. 
    TODO: add section on connection between information and insight

Digital Twins and Formal Methods
--------------------------------

.. 
    TODO: ask about which tool is actually used (Coq, Agda...)

Role of Formal Methods in DT
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- Conceptual clearness, semantics, compositionality
- correctness
- Better tool support
- Beyond simulation: worst-case, what-if scenarios, etc

Role of Knowledge Representation in DT
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- Structural twin: uniformly represent knowledge about PT and DT
- Reasoning support that can exploit this knowledge
- Allows correctness properties to be expressed as relations between PT and DT


The Semantically Reflected Digital Twin
---------------------------------------

Now we see an example of a digital twin architecture
description using formal methods based on three technologies
techniques.

**SWT** ::
    Semantic Web Technologies for uniform knowledge
    representation and integration of domain knowledge
    (discussed later).

**FMI** ::
    The Functional Mock-Up Interface standard for interfaces
    between PT and DT, as well as simulations (discussed
    later).

**SMOL** ::
    Semantic Reflection to reason about PT and DT through
    the integration of SWT and FMI into a programming
    language (discussed later). The system is implemented in SMOL, a
    unique language designed specifically for integration of
    SWT and programming.

.. 
    TODO: add reference/redirect to swt/fmi/smol parts (knowledge modelling) (?) 

Example: House heating
^^^^^^^^^^^^^^^^^^^^^^

An example for a digital twin architecture can be made for an house with a heating system.

A DT can be divided into

* **Structural Twin**: formed by
    * Asset model:
        * Domain knowledge: connection between all the "parts" of the house (rooms, heaters, walls) to represent the concept of "House" as long as the simulators for each part.
        * Instance: a spectific instance of the domain knowledge for a particular house"
    * Twin model:
        * Domain & Instance: instance of the domain knowledge  for the behavioral twin

* **Behavioral Twin**: formed by
    * Digital twin infrastructure: which coordinates the simulation of all the units (parts of the house).
    * Twin configuration: which control the coupled simulation of all the units (parts of the house).

.. 
    TODO: add house img from demo_day1 slide 35 

Tool Installation
-----------------

Here is a list of the softwares and assets that will be used in this tutorial.

* Download https://github.com/smolang/SemanticObjects/blob/master/examples/tutorialfiles.zip
* Download and install Protegé from https://protege.stanford.edu/products.php
* Download and install docker from https://www.docker.com/get-started/ (or from your favorite Linux distribution)

Once installed Docker on your device:

* Run docker pull ghcr.io/smolang/smol:latest
* Run docker pull openmodelica/openmodelica:v1.19.2-minimal


