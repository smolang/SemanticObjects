
Introduction
============

SMOL is an imperative, object-oriented research language.  SMOL integrates
semantic web technologies and numerical simulation blocks, and can serve as a
test bed for creating digital twins.

Semantic Web Technologies and SMOL
----------------------------------

SMOL supports *semantic lifting* of program state: objects and their fields
are represented as :term:`RDF` triples and can be processed via standard semantic web
technologies like :term:`SPARQL`.  An external ontology can be used to give additional
semantics to the lifted program state.  Section :ref:`semantic-access` describes this in detail.

Co-Simulation and SMOL
----------------------

Dynamic simulation model components following the `FMI standard
<https://fmi-standard.org>`_ (:term:`FMU`\ s) can be directly integrated into SMOL code.  SMOL
drives the dynamic model inputs and controls the advancement of time for all FMUs.  This is discussed further in Section :ref:`fmos`.
