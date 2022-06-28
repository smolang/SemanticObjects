Classes
=======

.. highlight:: BNF

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
