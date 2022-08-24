Title: Emacs Editor Support for SMOL

This directory contains files to add SMOL support to the Emacs editor.

# Installation

To activate it, add the following command to your Emacs init file (typically
`~/.emacs.d/init.el`):

```elisp
(load "/path/to/SemanticObjects/editor-support/emacs/smol-mode")
```

Afterwards, files with the extension `.smol` will be opened in `smol-mode`.

# Running a SMOL REPL inside Emacs

To run a SMOL REPL inside Emacs, set the variable `smol-jar-file` to the path
of the SMOL jar file, e.g. via

```elisp
(setq smol-jar-file "/path/to/SemanticObjects/build/libs/smol-0.2-all.jar")
```
