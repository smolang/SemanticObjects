# Deploying to `smolang.org`

1. Build the website: run `make html` in this directory.
2. Check out https://github.com/smolang/smolang.github.io.git
3. Copy the contents of `build/html/` to the root of the directory created in
   Step 2
4. Commit the changes in the checkout of `smolang.github.io` created in Step
   2; take care to also `git add` all newly-created files and `git rm` all
   vanished files..

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

    find source -name '*.rst' | entr -d make html

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
