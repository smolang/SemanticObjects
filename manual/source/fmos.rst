.. _fmos:

Functional Mock-Up Objects
==========================
.. highlight:: BNF

SMOL implements *functional mock-up objects* (FMOs), encapsulations of
*functional mock-up units* (:term:`FMU`\ s) as defined by the `FMI standard
<https://fmi-standard.org/>`_, currently `version 2.0.2
<https://github.com/modelica/fmi-standard/releases/download/v2.0.2/FMI-Specification-2.0.2.pdf>`_.
An FMO is a wrapper that integrates FMUs into the language; it is a special
object with the inputs and outputs of an FMU as properties/fields and special
statements that initialize and advance the wrapped FMU. An FMO is not an
instance of a SMOL class, but its inputs and outputs define an FMO-type on
which a subtyping relation is given.

Syntax
------

Syntactically, FMOs are treated as special objects with distinct *FMO-types*
and a distinct expression for their instantiation.

Types
^^^^^

The type of an FMO contains the list of all its inputs and all its outputs.
Syntactically, an *FMO-type* has the following form: The keyword ``Cont``,
followed by a comma-separated list of inputs and outputs enclosed in square
brackets.

::

  TYPE ::= ... | 'Cont' '[' (FmuParam (',' FmuParam)* )? ']'
  FmuParam ::= ('in' | 'out') TYPE NAME

Each input of the underlying FMO that is to be written must be declared via
``in TYPE NAME``, and every output that is to be read must be declared via
``out TYPE NAME``.  ``NAME`` must be the name of an input or output variable,
respectively, as declared by the underlying FMU in its
``modelDescription.xml`` file.

Types of input and output variables must be given as SMOL types.  The
following table shows the type names used by the FMI standard and how they
translate to SMOL type names:

========= =======
FMI 2.0.2 SMOL
========= =======
Real      Double
Boolean   Boolean
String    String
Integer   Int
========= =======

The type of an FMO wrapping an FMU without any inputs and outputs is
``Cont[]``.

FMO-types are covariant:
An FMO-type ``T = Cont[in Ti1 i1,... , out To1 o1...]`` is a subtype of another FMO-type ``S = Cont[in Sj1 j1,... , out Sp1 p1...]`` 
if

1. each input ``i`` of ``T`` is also an input ``i`` of ``S``, such that the type of ``i`` in ``T`` is a subtype of the type of ``i`` in ``S``, and 
2. each output ``i`` of ``T`` is also an output ``i`` of ``S``, such that the type of ``i`` in ``T`` is a subtype of the type of ``i`` in ``S``.

Statements
^^^^^^^^^^

The ``simulate`` statement takes a path to an FMU and a list of variable initializers, which initialize the FMO.
The ``doStep`` statement targets an expression with an FMO-type and takes a ``Double`` typed expression as a parameter.
::

  Statement ::= ... | (Type? NAME '=')? 'simulate' '(' STRING (',' VarInits)?')' 
                    | Expression '.' 'doStep' '(' Expression ')'
  VarInits  ::= NAME '=' LITERAL (',' VarInits)? 

The variable initializers must target inputs or parameters of the FMU and respect typing.
The string parameter must be a path to an FMU with a model description for co-simulation.
If the path is relative, then it must be relative to the directory where the SMOL runtime is run.

.. highlight:: Java

The following shows how an FMO is loaded and passed as a parameter.
::

  class C(Cont[ out Int i] fmo) end

  main 
    Cont[in Int j, out Int i] cont = simulate("path/to/fmu", j=1, k=1);
    C c = new C(cont);
  end


Semantics
---------

The fields and methods of an FMO are auto-generated: there is one field for every input and one field for every output.
Assigning to a field that is an output
triggers at a compile-time error. 
Reading a field that is an input 
triggers a compile-time warning. [#footnoteinout]_
Each such operation results in a call to ``fmi2SetXXX``, resp. ``fmi2GetXXX``, an FMO does not buffer read or written values itself.

Addtionally, each FMO has a field ``role`` of ``String`` type and a field ``offset`` of integer type, which play a role in their semantic lifting and is explained below.
These fields can be read and written and are not connected to the encapsulated FMU.

The ``doStep`` statement advances the encapsulated FMU by ``t`` time units. It results
in a call to ``doStep`` of the encapsulated FMU.


.. highlight:: Java

The following shows how an FMO is loaded and manipulated. 
::

  main 
    Cont[in Int j, out Int i] cont = simulate("path/to/fmu", j=1, k=1);
    cont.role = "Example FMO";
    cont.doStep(0.1);
    Int v = cont.i;
    cont.j = v+1;
  end

  

Semantic Lifting
----------------

.. NOTE::
  Semantical lifting of FMOs is under development. Currently, they are *completely* omitted from the generated knowledge graph.

.. rubric:: Footnotes

.. [#footnoteinout] FMUs may or may not throw a runtime error on this operations.
