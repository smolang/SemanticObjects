Statements
==========

This chapter specifies all SMOL statements.

::

   Statement ::= SkipStatement
               | ExpressionStatement
               | VariableDeclarationStatement
               | AssignmentStatement
               | ConditionalStatement
               | WhileStatement
               | TickStatement
               | BreakpointStatement
               | PrintStatement
               | DestroyStatement

The Skip Statement
------------------

The skip statement is a statement that does nothing.

::

   SkipStatement ::= 'skip' ';'


.. _expression_statement_ref:

The Expression Statement
------------------------

All expressions can be written as statements on their own, i.e., without assigning their value to a variable.

::

   ExpressionStatement ::= Expression ';'


The Variable Declaration Statement
----------------------------------

This statement declares a variable in the current scope.  All variables must
have an initial value.

.. note::

   Currently, a bug in SMOL means that variables are in scope until the end of
   the method, even if declared inside a more limited scope.

::

   VariableDeclarationStatement ::= Type Identifier ':=' Expression ';'


The Assignment Statement
------------------------

This statement assigns a new value to a given variable or field.

::
   
   AssignmentStatement ::= ( VariableExpression | FieldExpression ) ':=' Expression ';'

The Conditional Statement
-------------------------

The conditional statement executes either the statements in its consequent
branch or, if given, the statements in its alternate branch, depending on the
value of its ocnditional expression.

::

   ConditionalStatement ::= 'if' SimpleExpression
                            'then' Statement+
                            ( 'else' Statement+ )?
                            'end'

The While Loop
--------------

The while loop checks its condition and, if true, executes its statements
until the given condition evalutes to False.

::

   WhileStatement ::= 'while' SimpleExpression 'do' Statement+ 'end'

The Tick Statement
------------------

The Tick statements advances time for the given FMU by the given amount.

::

   TickStatement ::= SimpleExpression '.' 'tick' '(' SimpleExpression ')' ';'

The Breakpoint Statement
------------------------

::

   BreakpointStatement ::= 'breakpoint' ';'

The Print Statement
-------------------

The print statement evaluates its argument and prints the resulting value to
the terminal.

::

   PrintStatement ::= 'print' '(' SimpleExpression ')' ';'

The Destroy Statement
---------------------

::

   DestroyStatement ::= 'destroy' '(' SimpleExpression ')' ';'
