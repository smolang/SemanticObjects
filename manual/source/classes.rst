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

::

   ClassDeclaration ::= 'abstract'? ('private' | 'protected')?
                        'class' Identifier ( '<' Identifier (',' Identifier)* '>')?
                        ('extends' Type )?
                        '(' (FieldDeclaration (',' FieldDeclaration)* )? ')'
                        ModelsDeclaration?
                        MethodDefinition*
                        'end'

   FieldDeclaration ::= 'nonsemantic'? ('private' | 'protected')? 'domain'?
                        Type Identifier

   ModelsDeclaration ::= ('models' '(' Expression ')' StringLiteral ';')*
                         'models' StringLiteral ';'

   MethodDefinition ::= 'abstract'? ('private' | 'protected')? 'rule'? 'domain'? 'override'?
                        Type Identifier '(' (Type Identifier (',' Type Identifier)* )? ')'
                        Statement*
                        'end'
