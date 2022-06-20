
Random helpful links to documentation, 

# Basic rst info

https://chiplicity.readthedocs.io/en/latest/Using_Sphinx/OnReStructuredText.html

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
