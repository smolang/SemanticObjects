Title: Atom Editor Support for SMOL

This directory contains files to add SMOL support to the Atom editor.

# Installation

To activate it, issue the following shell commands:

```shell
cd ~/.atom/packages
ln -s /path/to/SemanticObjects/editor-support/atom/language-smol language-smol
```

Afterwards, files with the extension `.smol` will be opened in `smol-mode`.

# Random Development Notes

`Ctrl-Shift-Cmd-R`: live reload themes, packages (package `dev-live-reload`)


Good sample grammar: [GitHub - JeremyHeleine/language-scilab: Scilab
language support in
Atom](https://github.com/JeremyHeleine/language-scilab)

Tutorial-in-progress: [Creating a
Grammar](https://flight-manual.atom.io/hacking-atom/sections/creating-a-grammar/)

Regex documentation: [oniguruma/RE at master · kkos/oniguruma ·
GitHub](https://github.com/kkos/oniguruma/blob/master/doc/RE)
