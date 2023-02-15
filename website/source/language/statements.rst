Statements
==========

.. highlight:: BNF

This chapter specifies all SMOL statements.

::

   Statement ::= SkipStatement
               | ExpressionStatement
               | VariableDeclarationStatement
               | AssignmentStatement
               | ConditionalStatement
               | WhileStatement
               | ReturnStatement
               | TickStatement
               | BreakpointStatement
               | PrintStatement
               | DestroyStatement

The Skip Statement
------------------

The skip statement is a statement that does nothing.

::

   SkipStatement ::= 'skip' ';'

*Example:*

.. code-block:: java

   skip;

.. _expression_statement_ref:

The Expression Statement
------------------------

All expressions can be written as statements on their own, i.e., without assigning their value to a variable.

::

   ExpressionStatement ::= Expression ';'

*Example:*

.. code-block:: java

   worker.processRequest();
   2 + 3;

The Variable Declaration Statement
----------------------------------

This statement declares a variable in the current scope.  All variables must
have an initial value.

.. note::

   Currently, a bug in SMOL means that variables are in scope until the end of
   the method, even if declared inside a more limited scope like the body of a
   ``while`` loop.

::

   VariableDeclarationStatement ::= Type Identifier ':=' Expression ';'

*Example:*

.. code-block:: java

   Boolean result = worker.processRequest();
   Int a_number = 2 + 3;

The Assignment Statement
------------------------

This statement assigns a new value to a given variable or field.

::
   
   AssignmentStatement ::= ( VariableExpression | FieldExpression ) ':=' Expression ';'

*Example:*

.. code-block:: java

   result = worker.processRequest();
   a_number = 5 + 3;


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

*Example:*

.. code-block:: java

   if a_number > 17 then
       a_number = - a_number;
   else
       a_number = a_number + 1;
   end


The While Loop
--------------

The while loop executes the statements in its body repeatedly, as long as the
condition evaluates to true beforehand.

::

   WhileStatement ::= 'while' SimpleExpression 'do' Statement+ 'end'

*Example:*

.. code-block:: java

   while !result do
       result = worker.processRequest();
   end


The Return Statement
--------------------

The return statement finishes execution of the current method and returns the
value of its argument to the caller of the method.

::

   ReturnStatement ::= 'return' SimpleExpression ';'

*Example:*

.. code-block:: java

   return false;

The Tick Statement
------------------

The Tick statements advances time for the given FMU by the given amount.

::

   TickStatement ::= SimpleExpression '.' 'tick' '(' SimpleExpression ')' ';'

*Example:*

.. code-block:: java

   my_fmu.tick(1.0);

The Breakpoint Statement
------------------------

The breakpoint statement interrupts execution and transfers control to the
REPL.  Execution can be resumed at the REPL via the ``auto`` command.

::

   BreakpointStatement ::= 'breakpoint' ';'

*Example:*

.. code-block:: java

   breakpoint;

The Print Statement
-------------------

The print statement evaluates its argument and prints the resulting value to
the terminal.

::

   PrintStatement ::= 'print' '(' SimpleExpression ')' ';'

*Example:*

.. code-block:: java

   print("Checkpoint reached");

The Destroy Statement
---------------------

The destroy statement destroys the given object and frees its memory.

::

   DestroyStatement ::= 'destroy' '(' SimpleExpression ')' ';'

*Example:*

.. code-block:: java

   destroy(my_object);
