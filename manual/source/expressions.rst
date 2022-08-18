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

*Example:*

.. code-block:: java

   "This is a string"
   5.13
   false

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

*Example:*

.. code-block:: java

   1 / 2
   bid > price || customer == "VIP"


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
     - numeric
   * - ``e1 + e2``
     - addition
     - numeric
     - numeric
   * - ``e1 * e2``
     - multiplication
     - numeric
     - numeric
   * - ``e1 % e2``
     - modulus
     - numeric
     - numeric
   * - ``e1 / e2``
     - division
     - numeric
     - numeric

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

*Example:*

.. code-block:: java

   null

The This Expression
-------------------

This expression evaluates to the current object.  This expression cannot be
used in the main block, since the main block does not execute within the scope
of an object.

::

   ThisExpression ::= 'this'

*Example:*

.. code-block:: java

   this

The Variable Expression
-----------------------

Variable expressions evaluate to the current content of the named variable.

::

   VariableExpression ::= Identifier

*Example:*

.. code-block:: java

   x
   a_long_variable_name

The Field Expression
--------------------

Field expressions evaluate to the current content of the named field in the
given object.  The object can be ``this`` or another object.

Note that fields of the current object cannot be accessed without the ``this.`` prefix.

.. TODO: discuss ``private``, ``public``, ``nonsemantic``

::

   FieldExpression ::= SimpleExpression '.' Identifier

*Example:*

.. code-block:: java

   this.x
   an_object.a_long_field_name

The FMU Field Expression
------------------------

This expression reads the current value of the named out port of the given
FMU.

::

   FmuFieldExpression ::= SimpleExpression '.' 'port' '(' StringLiteral ')'

*Example:*

.. code-block:: java

   my_fmu.port("outport")

The New Expression
------------------

The New expression creates a new object of the given class.  Values for the
class's constructor parameters are given as simple expressions inside
parentheses. All generic type parameters of a class must be instantiated.

The optional ``models`` clause overrides any ``domain`` modifier or ``models``
clause of the new object's class declarations (see
:ref:`class_declaration_ref`).

::

   NewExpression ::= 'new' Type '(' ( SimpleExpression ( ',' SimpleExpression)* )?  ')' ( 'models' SimpleExpression )

*Example:*

.. code-block:: java

   new Person("Name", 35) models "a :person"

The New FMU Expression
-----------------------

This expression creates a new FMU.  The expression takes first a literal
string containing the path to the FMU, followed by zero or more initializer
terms for the FMU's parameters.  All parameters specified by the FMU in its
``modelDescription.xml`` file must be initialized.

::

   NewFmuExpression ::= 'simulate' '(' StringLiteral (',' Identifier ':=' SimpleExpression)* ')'

*Example:*

.. code-block:: java

   simulate("../Sim.fmu", iValue = 0.0, slope = 1.5)

The Method Call Expression
--------------------------

This expression invokes the named method on the given object instance.

.. TODO: discuss public, private methods

::

   MethodCallExpression ::= Expression '.' Identifier '(' ( SimpleExpression ( ',' SimpleExpression)* )? ')'

*Example:*

.. code-block:: java

   this.doWork();
   worker.processRequest();

The ``super`` Expression
------------------------

This expression invokes the method as defined in a superclass of the current
object's class from within the overriding method.  The ``super`` expression is
only valid inside a method that overrides a superclass's method.

::

   SuperExpression ::= 'super' '(' ( SimpleExpression ( ',' SimpleExpression)* )? ')'

*Example:*

.. code-block:: java

   super.doWork()

The Query Expression
--------------------

The ``access`` top-level expression retrieves a list of literals or lifted objects using a query mode ``SPARQL`` to access the semantically lifted state or ``INFLUXDB`` to access an external InfluxDB database. If no query mode is given, ``SPARQL`` is the default.

::

   QueryMode ::= 'SPARQL' | ('INFLUXDB' '(' StringLiteral ')')

   QueryExpression ::= 'access' '(' SimpleExpression (',' QueryMode)? ( ',' SimpleExpression)* ')'

In ``SPARQL`` mode, it takes as its first parameter a ``String``-literal containing an extended `SPARQL <https://www.w3.org/TR/sparql11-overview/>`_ query, which additionally may contain non-answer variables of the form ``%i`` for some strictly positive number ``i``. The set of numbers for the non-answer variables must form an interval [1,n] for some n.
Additionally, the top-level expression takes a list of expressions of the length n.

At runtime, these expressions are evaluated and the result is syntactically substituted for the corresponding non-answer variable.
The SPARQL query must contain a ``?obj`` variable.


*Example:* The following retrieves all objects ``o`` of type ``C`` with ``o.b.c == this.x``.
::

   List<C> l = access("SELECT ?obj WHERE {?obj prog:C_b ?b. ?b prog:D_c %1 }", this.x);

In ``INFLUXDB`` mode, the first parameter is an InfluxDB query with a single answer variable. Non-answer variables are not supported and the query mode has a parameter itself, which is
a ``String``-literal containing a path to a `YAML <https://yaml.org/>`_ configuration to connect to the InfluxDB endpoint.
The result is always a ``List`` of ``Double`` values.

*Example:*
::

  main
    List<Double> list := access(
    "from(bucket: \"petwin\")
      |> range(start: -1h, stop: -1m)
      |> filter(fn: (r) => r[\"_measurement\"] == \"chili\")
      |> filter(fn: (r) => r[\"_field\"] == \"temperature\")
      |> filter(fn: (r) => r[\"name\"] == \"faarikaal1\")
      |> aggregateWindow(every: 5m, fn: mean, createEmpty: false)
      |> yield(name: \"mean\")",
    INFLUXDB("petwin.yml"));
    print(list.content);
  end

The Construct Expression
------------------------

The ``construct`` top-level expression constructs a list of *new* objects from a SPARQL query.
Its parameters are as the one of the ``access`` top-level expression, but the variables are handled differently:
::

   ConstructExpression ::= 'construct' '(' Expression ( ',' SimpleExpression)* ')'

Each variable must have the name of a field of the type of the target location. For each field there must be one variable. All fields must be of primitive data type.

*Example:*
::

   class C(Int j1, Int j2) end
   ...
   List<C> v = construct("SELECT ?j1 ?j2 WHERE { ?y a prog:B. ?y prog:B_i2 ?j2.?y prog:B_a ?x.?x a prog:A. ?x prog:A_i1 ?j1 }");

The Concept Expression
----------------------

The ``member`` top-level expression retrieves the list of objects described by an OWL concept.
The parameter must be a ``String``-literal containing a concept in `Manchester syntax <https://www.w3.org/TR/owl2-manchester-syntax/>`_.
::

   ConceptExpression ::= 'member' '(' Expression ')'

*Example:* The following retrieves all members of class ``C`` that model some domain concept ``domain:D``.
::

  List<C> list := member("<domain:models> some <domain:D>");

The execution fails if the concept is either mal-formed or contains elements that are not IRIs of lifted objects.

The Shape Expression
--------------------

Shape access validates the correctness of the lifted knowledge graph with respect to a graph shape using the top-level expression ``validate(Literal)``.
The parameter must be a ``String``-literal containing a path to `SHACL <https://www.w3.org/TR/shacl/>`_ shapes in `turtle <https://www.w3.org/TR/turtle/>`_ syntax.
::

   ShapeExpression ::= 'validate' '(' Expression ')'

*Example:*
::

   Boolean b  = validate("examples/double.ttl");

The execution fails if the file does not accessable or the SHACL shapes are mal-formed.
