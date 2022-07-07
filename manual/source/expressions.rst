Expressions
===========

.. highlight:: BNF

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
                      | NullExpression
                      | ThisExpression
                      | VariableExpression
                      | FieldExpression
                      | FmuFieldExpression

   TopLevelExpression ::= NewExpression
                        | NewFmuExpression
                        | MethodCallExpression
                        | SuperExpression
                        | QueryExpression
                        | ConstructExpression
                        | ConceptExpression
                        | ShapeExpression

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

The Null Expression
-------------------

The null expression evaluates to a value denoting an invalid object.  It can
be used, e.g., to initialize a variable that will be assigned another value
later in the program.

::

   NullExpression ::= 'null'

The This Expression
-------------------

This expression evaluates to the current object.  This expression cannot be
used in the main block, since the main block does not execute within the scope
of an object.

::

   ThisExpression ::= 'this'


The Variable Expression
-----------------------

Variable expressions evaluate to the current content of the named variable.

::

   VariableExpression ::= Identifier



The Field Expression
--------------------

Field expressions evaluate to the current content of the named field in the
given object.  The object can be ``this`` or another object.

.. TODO: discuss ``private``, ``public``, ``nonsemantic``

::

   FieldExpression ::= SimpleExpression '.' Identifier

The FMU Field Expression
------------------------

This expression reads the current value of the named out port of the given
FMU.

::

   FmuFieldExpression ::= SimpleExpression '.' 'port' '(' StringLiteral ')'

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

This expression creates a new FMU.  The expression takes first a literal
string containing the path to the FMU, followed by zero or more initializer
terms for the FMU's parameters.  All parameters specified by the FMU must be
initialized in this expression.

::

   NewFmuExpression ::= 'simulate' '(' StringLiteral (',' Identifier ':=' SimpleExpression)* ')'

The Method Call Expression
--------------------------

This expression invokes the named method on the given object instance.

.. TODO: discuss public, private methods

::

   MethodCallExpression ::= Expression '.' Identifier '(' ( SimpleExpression ( ',' SimpleExpression)* )? ')'

The ``super`` Expression
------------------------

This expression invokes the method as defined in a superclass of the current
object's class from within the overriding method.  The ``super`` expression is
only valid inside a method that overrides a superclass's method.

::

   SuperExpression ::= 'super' '(' ( SimpleExpression ( ',' SimpleExpression)* )? ')'

The Query Expression
--------------------

..
   The first argument is the query, second is language spec, then parameters

::

   QueryMode ::= 'SPARQL' | ('INFLUXDB' '(' StringLiteral ')')

   QueryExpression ::= 'access' '(' SimpleExpression (',' QueryMode)? ( ',' SimpleExpression)* ')'

The Construct Expression
------------------------

..
   The first argument is the query, rest are parameters

::

   ConstructExpression ::= 'construct' '(' Expression ( ',' SimpleExpression)* ')'

The Concept Expression
----------------------

..
   query is single argument

::

   ConceptExpression ::= 'member' '(' Expression ')'

The Shape Expression
--------------------

..
   query is single argument

::

   ShapeExpression ::= 'validate' '(' Expression ')'

