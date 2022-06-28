Lexical Structure
=================

.. highlight:: BNF

This section describes the lexical structure of SMOL.  We define the grammar
using a simple :term:`EBNF` notation as defined by `the W3C
<https://www.w3.org/TR/2006/REC-xml11-20060816/#sec-notation>`_.

Line Terminators and Whitespace
-------------------------------

::

   Whitespace ::= #x20 | #x09 | #x0D | #x0A | #x0C
   EOL ::= #x0A | #x0C

The following characters count as whitespace:

.. list-table:: Whitespace
   :header-rows: 1
   :align: left

   * - Code
     - Name
   * - 0x20
     - Space
   * - 0x09
     - Tab
   * - 0x0D
     - Carriage Return
   * - 0x0A
     - Line Feed
   * - 0x0C
     - Form Feed

Whitespace has no semantic meaning, with the exception of the Line Feed and
Carriage Return characters which terminate a line comment.

Comments
--------

Comments are syntax elements that are ignored during parsing.

::

   Comment ::= BlockComment | LineComment
   BlockComment ::= '/*' .* '*/'
   LineComment ::= '//' .* EOL

A block comment starts with ``/*`` and extends until the first occurrence of
``*/``.  A line comment starts with ``//`` and extends until the end of the line.

Identifiers
-----------

Identifiers start with a letter or underscore followed by a sequence of
letters, numbers and underscores.  A language keyword cannot be used as an
identifier.

::

   Identifier ::= [a-zA-Z_] [a-zA-Z0-9_]*


Keywords
--------

Any word occurring in the grammar (e.g., `class`) is a keyword.  Keywords
cannot be used as identifiers.

TODO: list all language keywords here?

.. _literals_ref:

Literals
--------

A literal is a textual representation of a value.  SMOL supports integer,
Boolean, string and float literals.

::

   Literal ::= IntLiteral | FloatLiteral | StringLiteral | BoolLiteral
   IntLiteral ::= '0' | '-'? [1-9] [0-9]*
   FloatLiteral ::= IntLiteral? '.' [0-9]* ([eE]IntLiteral)?
   StringLiteral ::= '"' ('\"' | .)* '"'
