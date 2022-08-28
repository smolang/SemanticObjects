# Headline Structure

Use the following punctuation characters in the section titles:

- `#` for Parts
- `*` for Chapters
- `=` for sections (“Heading 1”)
- `-` for subsections (“Heading 2”)
- `^` for subsubsections (“Heading 3”)
- `"` for paragraphs (“Heading 4”)

# Basic rst info

https://chiplicity.readthedocs.io/en/latest/Using_Sphinx/OnReStructuredText.html

# Automatically rebuild with entr

[eradman/entr: Run arbitrary commands when files change](https://github.com/eradman/entr)

    find source -name '*.rst' | entr -d make singlehtml

# Citations

https://chiplicity.readthedocs.io/en/latest/Using_Sphinx/UsingBibTeXCitationsInSphinx.html

# Railroad diagrams from EBNF

(EBNF spec is linked inside the documentation)

Use GrammKit: https://github.com/dundalek/GrammKit

    npm install -g grammkit
    grammkit -t md smol.ebnf # generates separate SVG files

Online playground at https://github.com/dundalek/GrammKit

## Other railroad diagram generators

https://bottlecaps.de/rr/ui

https://github.com/tabatkins/railroad-diagrams

https://github.com/katef/kgt

# Embed svg pictures

https://stackoverflow.com/questions/34777943/insert-clickable-svg-image-into-sphinx-documentation

`.. image:: myfile.svg`
