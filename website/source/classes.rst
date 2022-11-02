Classes
=======

.. highlight:: BNF

All code to be executed in SMOL, with the exception of the main block, is
contained in classes.  Classes define fields and methods.  Classes are
instantiated in objects via the ``new`` expression, all objects of a class
share the same methods and fields.  Each object's fields hold different state
that can change over time.

.. _class_declaration_ref:

Class Declarations
------------------
A class can have generic type parameters, field declarations, methods and a modelling bridge.
Additionally a class may be abstract and contain abstract methods. An abstract class may containt non-abstract methods.
::

   ClassDeclaration ::= 'abstract'? 
                        'class' Identifier ( '<' Identifier (',' Identifier)* '>')?
                        ('extends' Type )?
                        '(' (FieldDeclaration (',' FieldDeclaration)* )? ')'
                        ModelsDeclaration?
                        MethodDefinition*
                        'end'

A field is declared using a type and a name. 
The ``hidden`` modifier disables semantic lifting for this field.
The ``domain`` modifier moves the generated triples to the modelled node.
A field cannot be ``domain`` and ``hidden``.

::

  FieldDeclaration ::= 'hidden'? 'domain'?
                        Type Identifier



*models*

The ``models`` declarations are used to connect object structure to semantic
reflection; see Section :ref:`modeling-bridge`.

::

   ModelsDeclaration ::= ('models' '(' Expression ')' StringLiteral ';')*
                         'models' StringLiteral ';'

*methods*

A ``rule`` method cannot have side-effects (no object creation, no writing field access), no parameters and can only call other ``rule`` methods.

::

   MethodDefinition ::= ConcreteMethod | AbstractMethod

   ConcreteMethod ::= 'rule'? 'domain'? 'override'?
                        Type Identifier '(' (Type Identifier (',' Type Identifier)* )? ')'
                        Statement*
                        'end'

   AbstractMethod ::= 'abstract' 'rule'? 'domain'? 'override'?
                        Type Identifier '(' (Type Identifier (',' Type Identifier)* )? ')'
