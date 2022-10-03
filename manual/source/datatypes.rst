Datatypes
=========

.. highlight:: Java

This chapter describes the datatypes of SMOL.

Booleans
--------

The name for the Boolean (true or false) datatype is ``Boolean``.

::

   Boolean falsity = True;

Integers
--------

The name for the integer datatype is ``Int``.

::

   Int counter = 0;

Floating Point Numbers
----------------------

The name for the floating-point datatype is ``Double``.

::

   Double approximately_pi = 3.1415927;

Strings
-------

The name for the string datatype is ``String``.

::

   String language_name = "SMOL";

Objects
-------

Classes are datatypes, objects of that class have the type defined by the class.

::

   class Person (String name, Int age)
   end

   main
     Person p = new Person("SMOL", 1);
   end

FMUs
----

FMUs, instantiated via the ``simulate`` expression, have a type ``Cont[...]``,
with the square brackets listing the ``modelDescription.xml`` file as
contained in the FMU.

::

   Cont[in Int j, out Int i] cont = simulate("path/to/fmu", j=1, k=1);
   cont.j = 5;


Lists
-----

The ``List<C>`` datatype holds a list of values of type ``C``, where ``C`` can
be any SMOL datatype.  Currently lists are implemented as objects and are
built incrementally.

::

   List<Int> l1 = new List(5, null);
   List<Int> l2 = new List(3, l1);

