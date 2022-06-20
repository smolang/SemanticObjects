Lexical Structure
=================

This section describes the lexical structure of SMOL.  We define the grammar
using a simple :term:`EBNF` notation as defined by `the W3C
<https://www.w3.org/TR/2006/REC-xml11-20060816/#sec-notation>`_.

Line Terminators and Whitespace
-------------------------------

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
   BlockComment ::= '/*' .\* '*/'
   LineComment ::= '//' .* EOL

A block comment starts with ``/*`` and extends until the first occurrence of
``*/``.  A line comment starts with ``//`` and extends until the end of the line.

Identifiers
-----------



Keywords
--------

Literals
--------


