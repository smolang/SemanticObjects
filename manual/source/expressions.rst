Expressions
===========

This chapter describes all expressions of SMOL.  Informally, expressions are
the language elements that can occur on the right hand side of an assignment.

::

   Expression ::= LiteralExpression
                | OperatorExpression

Literal Expressions
-------------------

All literals, as defined in :ref:`literals_ref`, can be used as expressions.

::

   LiteralExpression ::= Literal

Unary and Binary Operator Expressions
-------------------------------------

SMOL has a range of unary and binary operators working on pre-defined datatypes. 

::

   OperatorExpression ::= UnaryOperatorExpression | BinaryOperatorExpression

   UnaryOperatorExpression ::= UnaryOperator Expression

   UnaryOperator ::= '!'

   BinaryOperatorExpression ::= Expression BinaryOperator Expression

   BinaryOperator ::= '/' | '%' | '*' | '+' | '-' | '==' | '!=' | '>=' | '<=' | '>' | '<' | '&&' | '||'

The following table describes the meaning as well as the associativity and the
precedence of the different operators. The list is sorted from low precedence
to high precedence.

.. list-table:: Operators
   :header-rows: 1
   :align: left

   * - Expression
     - Meaning
     - Argument types
     - Result type
   * - ``! e``
     - logical negation
     - Boolean
     - Boolean
   * - ``e1 || e2``
     - logical or
     - Boolean
     - Boolean
   * - ``e1 && e2``
     - logical and
     - Boolean
     - Boolean
   * - ``e1 < e2``
     - less than
     - numeric
     - Boolean
   * - ``e1 > e2``
     - greater than
     - numeric
     - Boolean
   * - ``e1 <= e2``
     - less or equal than
     - numeric
     - Boolean
   * - ``e1 >= e2``
     - greater or equal than
     - numeric
     - Boolean
   * - ``e1 != e2``
     - not equal to
     - compatible
     - Boolean
   * - ``e1 == e2``
     - equal to
     - compatible
     - Boolean
   * - ``e1 - e2``
     - subtraction
     - numeric
     - Boolean
   * - ``e1 + e2``
     - addition
     - numeric
     - Boolean
   * - ``e1 * e2``
     - multiplication
     - numeric
     - Boolean
   * - ``e1 % e2``
     - modulus
     - numeric
     - Boolean
   * - ``e1 / e2``
     - division
     - numeric
     - Boolean

Semantics of Comparison Operators
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Equality and inequality comparison is standard: by value for datatypes and by
reference for objects. I.e., two strings "Hello" compare as identical via
``==``, as do two numbers. Two references to objects compare as identical via
``==`` if they point to the same object or future. The inequality operator
``!=`` evaluates to ``True`` for any two values that compare to ``False``
under ``==`` and vice versa.

The less-than operator ``<`` and the other comparison operators compare
numbers of different types (integers vs floats) in the expected way.
