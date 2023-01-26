Internals
=========

This page contains a sketch of the SMOL implementation internals.

Parsing and Typechecking Program Flow
-------------------------------------

- SMOL input files are parsed via antlr; the antlr grammar can be found in `src/main/antlr/While.g4 <https://github.com/smolang/SemanticObjects/blob/master/src/main/antlr/While.g4>`_.

- Parsing, typechecking and preparation to run a SMOL program are done by the method `REPL.initInterpreter <https://github.com/smolang/SemanticObjects/blob/master/src/main/kotlin/no/uio/microobject/runtime/REPL.kt>`_, which calls methods according to Figure :ref:`Parsing Flow <parsing-flow-ref>`.


.. _parsing-flow-ref:

.. mermaid::
   :caption: The program flow of smol program text inside ``REPL.initInterpreter``

   flowchart TB
       input([ `-i` argument or REPL `read` command])
       antlr[[WhileParser.program]]
       translate[[Translate.generateStatic]]
       typecheck[TypeChecker]
       interpret[Interpreter]

       click antlr "https://github.com/smolang/SemanticObjects/blob/master/src/main/antlr/While.g4" _blank
       click translate "https://github.com/smolang/SemanticObjects/blob/master/src/main/kotlin/no/uio/microobject/ast/Translate.kt" _blank
       click interpret "https://github.com/smolang/SemanticObjects/blob/master/src/main/kotlin/no/uio/microobject/runtime/Interpreter.kt" _blank
       click typecheck "https://github.com/smolang/SemanticObjects/blob/master/src/main/kotlin/no/uio/microobject/type/TypeChecker.kt" _blank

       input-- smol filename -->antlr

       subgraph REPL.initInterpreter
       direction TB
       antlr -- antlr tree -->translate
       antlr -- antlr tree -->typecheck
       translate -- StaticTable -->interpret
       translate -- StaticTable -->typecheck
       end


Statement Execution
-------------------

Executing a smol program is controlled by the `REPL <https://github.com/smolang/SemanticObjects/blob/master/src/main/kotlin/no/uio/microobject/runtime/REPL.kt>`_ class.  An  ``Interpreter`` object keeps track of the execution state, which consists of a stack of `StackEntry <https://github.com/smolang/SemanticObjects/blob/master/src/main/kotlin/no/uio/microobject/runtime/State.kt#:~:text=data%20class%20StackEntry>`_ objects and a `GlobalMemory <https://github.com/smolang/SemanticObjects/blob/master/src/main/kotlin/no/uio/microobject/runtime/State.kt#:~:text=typealias%20GlobalMemory>`_ instance, which in turn maps object names to their `Memory <https://github.com/smolang/SemanticObjects/blob/master/src/main/kotlin/no/uio/microobject/runtime/State.kt#:~:text=typealias%20Memory>`_.

A single statement is executed by the method `Interpreter.makeStep <https://github.com/smolang/SemanticObjects/blob/master/src/main/kotlin/no/uio/microobject/runtime/Interpreter.kt#:~:text=fun%20makeStep>`_.
