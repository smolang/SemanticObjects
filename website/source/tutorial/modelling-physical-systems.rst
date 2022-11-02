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

.. math::

   l' = -d * l + v * f

where

* :math:`l`: current level in tank

* :math:`d`: drain rate, "size of the hole"

* :math:`f`: rate of inflow

* :math:`v`: valve control (0: valve is closed, 1: valve is open)
