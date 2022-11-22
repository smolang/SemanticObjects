.. _introduction:

Introduction
============

Our Vision
----------

The SMOL language can be used as a framework for developing digital twins. For
digital twins, the knowledge graphs can be used to capture asset models. SMOL
programs can then *seamlessly interact with asset models and domain knowledge*
to configure and adapt, e.g., simulators. SMOL uses Functional Mock-Up Objects
(FMOs) as a programming layer to encapsulate simulators compliant with the FMI
standard into OO structures and integrates FMOs into the class and type
systems. By means of the semantic lifting, the FMOs can be integrated into
knowledge graphs and used to ensure structural correctness properties for
cyber-physical applications.

SMOL features
-------------

* Imperative, object-oriented language
* Seamless integration of programs and knowledge bases
* Built-in reflection of program state in the knowledge base
* Knowledge bases can be queried from SMOL programs
* Encapsulation of simulators in objects based on the FMI standard

**Open source**. SMOL is released under the terms of the 3-clause BSD licence,
 the source is available at https://github.com/smolang/SemanticObjects.

Digital Twins and SMOL
----------------------

Digital Twins and similar applications typically connect simulators with
data-rich components and domain knowledge, both commonly formalised as
knowledge graphs. Engineering such applications poses challenges to
developers, which we address using a language-based approach to enable their
efficient development, as well as explore analysis and design: **SMOL** is an
imperative, object-oriented research language which integrates semantic web
technologies and numerical simulation blocks, and can serve as a test bed for
creating digital twins.


Semantic Web Technologies and SMOL
----------------------------------

SMOL supports *semantic lifting* of program state: objects and their fields
are represented as :term:`RDF` triples and can be processed via standard semantic web
technologies like :term:`SPARQL`.  An external ontology can be used to give additional
semantics to the lifted program state.  Section :ref:`semantic-access` describes this in detail.

SMOL proposes a *language-based integration of knowledge graphs and
object-oriented programs*. SMOL programs can contain queries to external
knowledge graphs that contain, e.g., domain knowledge about an application
domain. SMOL further proposes *semantic reflection* of programs into knowledge
graphs by lifting the runtime state of the program into an associated
knowledge graph, which enables programs to directly query this semantic
representation of itself at runtime. This way, programs can make use of domain
knowledge in the knowledge graph. Semantic reflection in SMOL can be used in
interesting ways by giving the program access to formalised domain knowledge
about its own runtime state, for example for debugging but also for system
reconfiguration.

Co-Simulation and SMOL
----------------------

Dynamic simulation model components (:term:`FMU`\ s) that follow the `FMI
standard <https://fmi-standard.org>`_ can be directly integrated into SMOL
code.  A SMOL program can drive the dynamic model inputs and controls the
advancement of time for all FMUs, and can access the dynamic model outputs,
making them available for semantic lifting.  This is discussed further in
Section :ref:`fmos`.
