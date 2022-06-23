Expressions
===========

This chapter describes all expressions of SMOL.  Informally, expressions are
the language elements that can occur on the right hand side of an assignment (even though they can be written on their own, see :ref:`expression_statement_ref`).

Expressions are divided syntactially into two categories: *Simple
Expressions*, which can be nested, and *Top Level Expressions*, which cannot
be sub-expressions of other expressions.  This slight notational inconvenience
makes it easier to develop static analysis techniques and tools for SMOL.

::

   Expression ::= SimpleExpression
                | TopLevelExpression

   SimpleExpression ::= LiteralExpression
                      | OperatorExpression
                      | VariableExpression
                      | FieldExpression

   TopLevelExpression ::= NewExpression

Literal Expressions
-------------------

All literals, as defined in :ref:`literals_ref`, can be used as simple expressions.

::

   LiteralExpression ::= Literal

Unary and Binary Operator Expressions
-------------------------------------

SMOL has a range of unary and binary operators working on pre-defined
datatypes.

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

The Variable Expression
-----------------------

Variable expressions evaluate to the current content of the named variable.

::

   VariableExpression ::= Identifier



The Field Expression
--------------------

Field expressions evaluate to the current content of the named field in the
current object.  It is an error to use a field expression in the main block.

::

   FieldExpression ::= 'this' '.' Identifier

The New Expression
------------------

The New expression creates a new object of the given class.  Values for the
class's constructor parameters are given as simple expressions inside
parentheses.

The optional ``models`` clause overrides any ``domain`` modifier or ``models``
clause of the new object's class declarations (see
:ref:`class_declaration_ref`).

::

   NewExpression ::= 'new' Identifier '(' ( SimpleExpression ( ',' SimpleExpression)* )?  ')' ( 'models' SimpleExpression )

The New FMU Expression
-----------------------

SIMULATE

The Method Call Expression
--------------------------

The ``super`` Expression
------------------------

The Query Expression
--------------------

The Construct Expression
------------------------

The Concept Expression
---------------------

MEMBER

The Shape Expression
--------------------

VALIDATE

