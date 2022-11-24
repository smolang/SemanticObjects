Modelling Physical Systems
==========================

We will do the following:

* Model a water tank in `Modelica`_;
* Compile the Modelica model into an :term:`FMU`;
* Write a simple SMOL program that loads the FMU;
* Simulate and control the system’s behavior over time

.. _Modelica: https://www.openmodelica.org

Behavior of a Watertank
-----------------------

We will model a watertank with a pipe, such that turning on the pipe adds a
constant amount of water per second to the current level of the tank.  The
tank also has a drain hole at the bottom; the rate of outflow is dependent on
the current water level of the tank.

Mathematically, the behavior of the tank can be described by the following formula:

.. math::

   l' = -d * l + v * f

where

* :math:`l`: current level in tank

* :math:`d`: drain rate, "size of the hole"

* :math:`f`: rate of inflow

* :math:`v`: valve control (0: valve is closed, 1: valve is open)

The Watertank in Modelica
-------------------------

The following Modelica model implements the watertank.  Note that we use a
Boolean variable to model the valve status, and introduce a second equation
explicitly defining the current inflow from the valve.

.. code-block:: modelica

   model Tank
     parameter Real d = 0.5        "drain rate / 'size of hole'";
     parameter Real f = 1.0        "fill rate in l/s (constant)";
     input Boolean v(start = true) "Valve closed / open";
     output Real l(start = 5)      "water level";
     Real inFlow                   "Current fill rate";
   equation
     der(l) = inFlow - d * l;
     if v then inFlow = f; else inFlow = 0.0; end if;
   end Tank;

Creating an FMU for the Watertank
---------------------------------

OpenModelica can export Modelica models as FMUs.  This can be done by opening
the model in the OMEdit application, or from the command line via the ``omc``
program.  To generate an FMU from the command line, save the above model in a
file ``simple_tank.mo`` and create a file ``generate_fmu.mos`` with the
following content:

.. code-block:: modelica

   installPackage(Modelica);
   loadModel(Modelica);
   loadFile("simple_tank.mo");
   buildModelFMU(Tank, version="2.0", fmuType="me_cs");
   getErrorString()

Then, run the command ``omc generate_fmu.mos``, which should result in a file
``Tank.fmu``.
