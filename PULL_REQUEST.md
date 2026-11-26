# Pull Request — SMOL 1.0: dynamic reclassification and the new lifting vocabulary

> Use the **Suggested title** below for the PR title and paste the **Description** section into
> the PR body. Nothing in this file is pushed or committed automatically.

**Suggested title**

```text
SMOL 1.0: declarative reclassification, smol:links lifting, and boolean support
```

**Base / head**

| | |
|---|---|
| Base branch | `master` |
| Head branch | `prepare-1.0-reclassification` |
| Commits | 26 |
| Feature diff | 75 files, +4,180 / −1,614 |
| Follow-up working-tree diff | 14 files, +365 / −42 (README, website manual, EBNF, examples, test helper) |

---

## Description

### Summary

This PR prepares the 1.0 release of SMOL. It adds **declarative dynamic
reclassification** to the language, replaces the previous ad-hoc lifting with a
dedicated lifter and a revised vocabulary, adds **Boolean query and conversion
support**, and brings the type checker, REPL, documentation, examples, and test
suite in line with the new semantics.

At a high level:

- New class-level declarations `classifies "..."` and `retrieves "..."`, plus
  the `context` field modifier, describe the states of an adaptation hierarchy.
- New statements `classify(obj)` / `T x = classify(ctx)` and `adapt(obj)`
  reclassify objects in place or create a new state object.
- The modelling bridge now emits `smol:links` (instead of `domain:models` /
  `smol:models`) and class membership is asserted with
  `smol:implements prog:<Class>` (instead of `rdf:type prog:<Class>`).
- The default lifting represents fields as memory entries
  (`smol:hasEntry` / `smol:hasValue` / `smol:entryOf`); the pre-1.0 punning
  lifting remains available with `--punning`.
- Booleans are now first-class in query results, `construct`/`access`, and
  conversions (`booleanToString`).
- Version bumped to `1.0`; the language manual, grammar, examples, and tests
  were updated accordingly.

### Motivation

SMOL programs model systems that change their behaviour at runtime. Until now,
changing the class of a live object required manual heap manipulation and had no
declarative link to the knowledge graph. This PR makes reclassification a
first-class language feature: the current knowledge graph decides *whether* an
object changes state, and a query supplies the data needed by the new state.

The same release also fixes an ambiguity in the lifting vocabulary. Using
`domain:models` for the modelling bridge and `rdf:type prog:<Class>` for class
membership conflated the modelling relation with RDF typing and only worked
reliably under OWL punning. The new vocabulary separates these concerns and
makes the non-punning representation the default.

### What changed

#### 1. Language and grammar

- **Adaptation declarations** in class bodies (`src/main/antlr/While.g4`):
  - `classifies "<query>"` — decides whether an object belongs to a state.
    Accepts an `ASK` query, a `SELECT` query, or a Manchester class expression.
  - `retrieves "<select>"` — optional; supplies constructor parameters for the
    new state object. Only valid together with `classifies`.
- **Context field modifier**: `context Type name` marks the field that supplies
  the context object for classification. It must be the first field and is only
  allowed on constructor parameters.
- **New statements**:
  - `classify(expr);` — in-place reclassification.
  - `T x = classify(ctx);` — declared-target form; creates a new state object
    and leaves the source object untouched.
  - `adapt(expr);` — in-place reclassification of an existing object.
- **`models` / `links` alias**: the bridge keyword accepts both spellings; the
  emitted property is always `smol:links`.
- **Boolean support**:
  - `booleanToString(...)` conversion.
  - Boolean literals (`True` / `False`) are recognised in `access` /
    `construct` query results, including `xsd:boolean` lexical forms.
- **Reserved keywords**: `classify`, `adapt`, `classifies`, `retrieves`, and
  `context` can no longer be used as identifiers (see *Breaking changes*).

#### 2. Runtime semantics (`ClassifyStmt`, `AdaptStmt`)

- `classify` and `adapt` force the OWL reasoner for the duration of the
  evaluation and restore the previous setting in a `finally` block, so an
  exception cannot leak reasoner state.
- `%this`, `%context`, and `%parent` placeholders are substituted before a
  query runs. `%parent` denotes the root class of the adaptation hierarchy, not
  the candidate state class.
- `ASK`, `SELECT`, and Manchester `classifies` clauses are all supported;
  short Manchester expressions are wrapped as `<smol:links> some <expr>`.
- `retrieves` results become the constructor parameters of the new state.
  A `retrieves` query with no rows is a normal outcome: the candidate state is
  skipped and the next state is tried.
- In-place adaptation rewrites the heap entry, keeps only fields of the parent
  class, fills the fields of the new state, and redirects **every alias** of the
  old object (other heap entries, all stack frames, the current frame, and the
  current heap object) to the new object.
- `adapt(this)` is a no-op; if no state matches, the object is left unchanged.
- The declared-target form creates a new object with `CreateStmt`; if no state
  matches, the target is bound to the context object instead of failing.
- When the context field is declared on the root class, the context object is
  prepended as the first constructor parameter of the new state.
- `ASK` support was added to the interpreter (`Interpreter.ask`) and exposed in
  the REPL as `ask` / `a`.

#### 3. Lifting and knowledge graph vocabulary

- New `src/main/kotlin/no/uio/microobject/data/Lifters.kt` centralises class,
  field, method, rule-method, and linkage lifting.
- `TripleManager` was reworked around the new lifters, with guard clauses,
  virtualized vs. naive graphs, and per-source settings.
- **Default (non-punning) lifting** — fields become memory entries:

  ```turtle
  run:obj1 smol:implements prog:C ;
           smol:hasEntry run:entry1 .
  run:entry1 rdf:type smol:MemoryEntry ;
             smol:hasValue 5 ;
             smol:entryOf prog:C_i .
  ```

  Class declarations use `smol:hasField` / `smol:hasMethod`; the class hierarchy
  uses `smol:subClass`.

- **Punning lifting** (`--punning`, pre-1.0 compatible) — fields are lifted
  directly with generated properties; class membership is still asserted with
  `smol:implements`, and `rdf:type` membership can additionally be inferred from
  the punning field domains.
- The modelling bridge always emits `smol:links`; `smol:implements` is the
  canonical class-membership triple under both lifting modes.
- `src/main/resources/vocab.owl` was rewritten to describe the new classes and
  properties (`smol:Object`, `smol:MemoryEntry`, `smol:implements`,
  `smol:hasEntry`, `smol:hasPointer`, `smol:hasValue`, `smol:links`,
  `smol:entryOf`, `smol:subClass`).
- Bundled rules and examples (University, Geological, new geo rules) were
  migrated from `domain:models` to `smol:links` and from `a prog:<Class>` to
  `smol:implements prog:<Class>`.

#### 4. Type checking and query checking

- `TypeChecker` now validates adaptation declarations:
  - all constructor parameters of a state class must be `hidden`;
  - state classes must be leaves of the hierarchy;
  - the superclass should be the root of the hierarchy (warning otherwise);
  - all states of one hierarchy must declare the same methods (name, return
    type, and parameter types);
  - the `context` field must be first;
  - a field or constructor parameter typed as a state class produces a warning
    (use the root class instead);
  - instantiating a state with `new` produces a warning ("state marked for
    adaptation").
- `classify` / `adapt` statements are checked against the adaptation tables
  (`checkClassifiesTable`): a target outside any adaptation hierarchy is an
  error.
- `REPL` runs `checkAdaptationConsistency` when classification declarations are
  present: Manchester states are compared against the union of the declared
  state expressions; ASK/SELECT clauses are skipped because they are runtime
  queries, not DL expressions.
- `QueryChecker` was ported to a graph/DL-tree representation that handles
  multi-triple query patterns, uses the settings' prefix map (including
  `smol:`, `prog:`, `domain:`, `run:`), and checks against the static ontology
  without the heap. Type checks distinguish punning and non-punning membership
  (`smol:implements`).

#### 5. Tooling and documentation

- `build.gradle` version `0.4` → `1.0`.
- New CLI flag `--punning` switches to the pre-1.0 lifting; the default is the
  memory-entry lifting.
- REPL gets `ask` / `a`; query execution gains a dedicated `ask` path.
- `README.md` documents reclassification and the new vocabulary.
- Website language manual updated:
  - `classes.rst` — `classifies`, `retrieves`, `context`, single-modifier rule;
  - `statements.rst` — `classify` and `adapt` statements;
  - `semantic-access.rst` — new lifting vocabulary and a full *Classification
    and reclassification* section;
  - `datatypes.rst` / `expressions.rst` — Boolean access and
    `booleanToString`;
  - `smol.ebnf` — grammar summary updated.
- New runnable examples under `examples/Reclassification/`:
  - `classification.smol` — in-place classification (B → C);
  - `reclassification.smol` — classify/adapt cycle (B → C → B);
  - `Tree.smol` / `Tree_union.smol` — Manchester `classifies` plus
    `retrieves` over an ontology;
  - `README.md` — expected output and exact commands.

#### 6. Tests and examples

New/updated test coverage:

| Test | Covers |
|---|---|
| `ClassificationTest` | in-place classify, declared-target form, empty `retrieves` |
| `AdaptationTest` | ontology consistency, ontology growth, A/B/C adapt, tree classify/retrieve, adaptation consistency success/failure |
| `ReclassificationTypeTest` | accepted root-typed classify/adapt, unrelated-class error, state-instantiation warnings, consistency checks |
| `BooleanSupportTest` | `booleanToString`, boolean `access` results |
| `SMOLTypeNoPunTest` | query type checking under the non-punning lifting |
| `type_query_new_lift.smol` | fixture for the new lifting queries |
| `tree.ttl` | adds `:oak2` as a second `Oak` individual for object-uniqueness coverage |

Supporting test infrastructure changes:

- `MicroObjectTest` gains `punning` parameters, prefix injection for
  `initInterpreter`, and an `ADAPTATION_TESTS` gate for the heavier adaptation
  suite.
- `GreenHouse.smol` renames the `adapt` method to `adaptModel` (keyword
  migration).
- `models.smol` / `models.owl` migrated to `smol:links` and a subclass axiom.

#### 7. Repository housekeeping

- Removed obsolete `eval/` scripts, `examples/Deprecated/`, `examples/bug.smol`,
  and `domain.pdf`.
- `LICENSE.txt` copyright range updated to 2025.
- `.gitignore` adds `.opencode`.

### Breaking changes and migration

| Before | After | Action |
|---|---|---|
| `domain:models` / `smol:models` bridge property | `smol:links` | Rewrite SPARQL/rules; `models` / `links` keyword both accepted |
| `run:obj a prog:C` | `run:obj smol:implements prog:C` | Rewrite queries/rules; `rdf:type` remains inferable under `--punning` |
| `--punning` unavailable | default is non-punning; flag opts into old lifting | Use `--punning` for legacy programs/queries |
| Methods/variables named `classify`, `adapt`, `classifies`, `retrieves`, `context` | Reserved keywords | Rename identifiers (e.g. `adapt` → `adaptModel`) |
| Version `0.4` | `1.0` | Update version pins |
| Punning-only field representation | memory entries | Use `--punning` or migrate queries to `smol:hasEntry` / `smol:hasValue` / `smol:entryOf` |

### How to test

```bash
# Compile and run the test suite
./gradlew build
./gradlew test

# Targeted new tests (these passed locally on macOS with Corretto 11)
./gradlew test \
  --tests "no.uio.microobject.test.ast.stmt.ClassificationTest" \
  --tests "no.uio.microobject.test.type.ReclassificationTypeTest" \
  --tests "no.uio.microobject.test.execution.BooleanSupportTest" \
  --tests "no.uio.microobject.test.type.SMOLTypeNoPunTest"

# Heavier adaptation suite (ontology + HermiT); disabled by default
ADAPTATION_TESTS=true ./gradlew test \
  --tests "no.uio.microobject.test.ast.stmt.AdaptationTest"
```

Manual examples (from the repository root, after `./gradlew build`):

```bash
# A/B/C in-place classification
java -jar build/libs/smol.jar --execute \
  --back examples/Reclassification/classification_example.ttl \
  --domain http://www.smolang.org/ex# \
  --prefixes ast=http://www.smolang.org/ex# \
  --input examples/Reclassification/classification.smol

# Tree: Manchester classifies + retrieves
java -jar build/libs/smol.jar --execute \
  --back examples/Reclassification/tree.ttl \
  --domain http://www.smolang.org/tree# \
  --prefixes ast=http://www.smolang.org/tree# \
  --input examples/Reclassification/Tree.smol
```

Expected results are documented in `examples/Reclassification/README.md`.

### Reviewer's guide

Suggested reading order:

1. `src/main/antlr/While.g4`, `ast/Translate.kt` — syntax and static tables.
2. `ast/stmt/ClassifyStmt.kt`, `ast/stmt/AdaptStmt.kt` — runtime semantics.
3. `data/Lifters.kt`, `data/TripleManager.kt`, `resources/vocab.owl` — lifting
   and vocabulary.
4. `type/TypeChecker.kt`, `type/QueryChecker.kt` — static checks.
5. `src/test/...` and `examples/Reclassification/` — behaviour and examples.

### Known limitations / follow-ups

- `classify` / `adapt` force the OWL reasoner during evaluation. This is
  correct but can be expensive on large ontologies.
- `AdaptationTest` is gated behind `ADAPTATION_TESTS=true`; it is not part of
  the default test run.
- The `--punning` path is retained for compatibility and is covered by the
  existing suite; new code should target the default memory-entry lifting.
- `.idea/gradle.xml` and `.idea/kotlinc.xml` contain local IDE configuration
  changes and can be dropped before merge if the project does not want to track
  them.
- The working tree also contains local, untracked artifacts
  (`config_local.yml`, `ontology-hermit.ttl`, `examples/tosemServices/`,
  `examples/tosemTests/`, `examples/tosemTests copy/`) that are **not** part of
  this change and must not be committed — `config_local.yml` contains an Influx
  token.

### Checklist

- [x] Grammar and translator updated
- [x] Runtime statements implemented
- [x] Lifting and vocabulary updated
- [x] Type checker and query checker updated
- [x] Tests added for classification, adaptation, Boolean support, and
      non-punning type checking
- [x] Documentation and examples updated
- [x] Targeted tests pass locally
- [ ] Full `ADAPTATION_TESTS=true` suite run on CI
- [ ] Drop incidental `.idea/` changes before merge
- [ ] Confirm no local artifacts are committed
