# Dynamic reclassification examples

These examples exercise the declarative dynamic-reclassification feature:
the class-level declarations `classifies "..."` / `retrieves "..."` and the
statements `classify(...)` and `adapt(...)`.

Vocabulary note: the current lifting links a run object to its model with
`smol:links` and states class membership with `smol:implements prog:<Class>`
(NOT `rdf:type prog:<Class>`). The `models`/`links` keyword of the modelling
bridge is accepted in both spellings, but the emitted triple is always
`smol:links`.

## Prerequisites

Build the jar once from the repository root:

```
./gradlew build
```

All commands below are run from the repository root.

## A/B/C example (classification)

Files: `classification_example.ttl` (ontology), `classification.smol`.

```
java -jar build/libs/smol.jar --execute --back examples/Reclassification/classification_example.ttl --domain http://www.smolang.org/ex# --prefixes ast=http://www.smolang.org/ex# --input examples/Reclassification/classification.smol
```

The program creates `A a = new B();` and calls `classify(a);`.

Expected state transition:

* `a` starts as a **B** object; its modelling bridge gives it
  `run:a smol:links [ a ast:A ]` (class `B` declares `models "a ast:A"`).
* `classify(a)` (single argument, reclassifies in place) evaluates the
  `classifies` declarations of the states of the root class `A`:
  * `B`'s ASK query `ASK WHERE { %this smol:links [a ast:B] . }` is **false**
    (the model node says `ast:A`, not `ast:B`).
  * `C`'s Manchester query `<smol:links> some <ast:A>` **matches**, so `a` is
    reclassified to **C** in place (and now links to a node saying `ast:B`).
* At the `breakpoint` the heap contains a `C` object; `a.printVal()` prints
  `C`.

## A/B/C example (classify + adapt cycle)

File: `reclassification.smol` (same ontology).

```
java -jar build/libs/smol.jar --execute --back examples/Reclassification/classification_example.ttl --domain http://www.smolang.org/ex# --prefixes ast=http://www.smolang.org/ex# --input examples/Reclassification/reclassification.smol
```

Expected state transitions:

* `A a = new B();` — `a` is a **B** (model node `ast:A`).
* `classify(a);` — as above, `a` becomes **C** (model node `ast:B`).
* `adapt(a);` — now `B`'s ASK query `%this smol:links [a ast:B]` is **true**,
  so `a` is adapted back to **B** in place (model node `ast:A` again).
* At the `breakpoint`, `a.printVal()` prints `B`.

The cycle therefore is **B → C → B**.

## Tree example (Manchester `classifies` + `retrieves`)

Files: `tree.ttl` (ontology with the `ast:Seedling`/`ast:Sapling`/… state
classes), `Tree.smol`, `Tree_union.smol`.

```
java -jar build/libs/smol.jar --execute --back examples/Reclassification/tree.ttl --domain http://www.smolang.org/tree# --prefixes ast=http://www.smolang.org/tree# --input examples/Reclassification/Tree.smol
```

```
java -jar build/libs/smol.jar --execute --back examples/Reclassification/tree.ttl --domain http://www.smolang.org/tree# --prefixes ast=http://www.smolang.org/tree# --input examples/Reclassification/Tree_union.smol
```

`Tree.smol`:

* `AssetModel.adaptModel()` loads the ontology individual `:oak1`
  (age 50, family "Quercus", height 80.0) with `construct(...)`.
* It then sets `tree.age = 10` and calls `adapt(tree)`.
* The `classifies "<ast:Seedling>"` Manchester declaration of `SeedlingTree`
  matches after OWL reasoning over the model node (age &lt; 40, height &lt; 90,
  family "Quercus" ⇒ `ast:Seedling`), so the tree is reclassified **in place**
  to a **SeedlingTree**.
* The `retrieves` SELECT query supplies the constructor parameter of the state
  class: it joins the model node (`%context smol:implements prog:Tree ;
  smol:links ?x`) with the ontology (`ast:oxAmount`) and yields
  `oxygen = 0.8` (from `:SeedlingOak`).
* At the `breakpoint`, `printStmt()` prints
  `SeedlingTree has oxygen: 0.8`.

`Tree_union.smol` additionally demonstrates the declared-target/in-place
classify path over all constructed trees (`configureModel()` → classify into
the age-appropriate state, `adaptBehaviour()` → set age 10 and re-adapt, which
drives every tree to the **SeedlingTree** state).

## Reserved keywords

`classify`, `adapt`, `classifies`, `retrieves` and `context` are reserved
keywords in the current grammar. Existing programs that used them as
identifiers must rename them — e.g. in the bundled GreenHouse example the
method `adapt` was renamed to `adaptModel`.
