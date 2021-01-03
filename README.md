# Semantic Micro Object Language
This repository contains an interactive interpreter for **SMOL**, a minimal object-oriented language with integrated semantic state access.
The interpreter can be used to examine the state with SPARQL, SHACL and OWL queries.
 
The project is in an early stage, and the interpreter performs little checks on its input. 
In particular, there is no type system and it is not checked that a `return` ends every path in the control flow.
Input has to be in turtle syntax (except the `class` command of the REPL, which is Manchester syntax).
Tested only on Linux. 

To compile a runnable jar, run
```
./gradlew build
java -jar build/libs/MicroObjects-0.1-SNAPSHOT-all.jar --help
```

Inside the REPL, enter `help` for an overview over the available commands.

## Examples
A number of examples are available under examples/.
Additionally, eval/ contains the files used for performance evaluation and domain.pdf gives the used domain model.


### Doubly Linked Lists

 * src/test/kotlin/resources/double.smol contains a simple doubly linked list example.
 * examples/double.rq contains a SPARQL query to select all `List` elements.
 * examples/double.ttl contains a SHACL query that ensures that all objects implement a class.
 * examples/double.imo contains a simple test session.

To execute it, run the following. If no jena path is provided (remove `-j $(dirname $(which jena))`), only the validate command fails.
```
./gradlew build
java -jar build/libs/MicroObjects-0.1-SNAPSHOT-all.jar -j $(dirname $(which jena)) -l src/test/kotlin/resources/double.smol -r examples/double.imo 
```

### 2-3 Trees

 * src/test/kotlin/resources/TwoThreeTree.smol contains an implementation of 2-3 trees
 * examples/TwoThreeTree.back contains the domain knowledge as a set of OWL classes
 * examples/tree_shapes.ttl contains a SHACL query that ensures that all objects implement a class.
 * examples/TTT.imo contains an example session (The one from the companion paper).

To execute it, run the following. If no jena path is provided (remove `-j $(dirname $(which jena))`), only the validate command fails.
```
./gradlew build
java -jar build/libs/MicroObjects-0.1-SNAPSHOT-all.jar -j $(dirname $(which jena)) -l src/test/kotlin/resources/TwoThreeTree.smol -b examples/TwoThreeTree.back -r examples/double.imo 
```

### Geometric Scene

 * examples/scene.mo contains an example to illustrate `rule`, the computational semantic state access (CSSA).
 * examples/scene.imo contain a sample session.

To execute it, run the following. 
```
./gradlew build
java -jar build/libs/MicroObjects-0.1-SNAPSHOT-all.jar -l examples/scene.mo -r examples/scene.imo 
```

### Cloud Platforms

 * src/test/kotlin/resources/overload.mo contains a simple demo for ontology-mediated programming 
 * examples/overload.back defines a server as overloaded if it has 3 tasks running
 * examples/overload.imo has a sample session that queries all overlaoded servers, reschedules and shows that no servers are overloaded afterwards

To execute it, run the following.
```
./gradlew build
java -jar build/libs/MicroObjects-0.1-SNAPSHOT-all.jar -l src/test/kotlin/resources/overload.smol -r examples/overload.imo -b examples/overload.back 
```

To change the interpretation of `:Overloaded` modify examples/overload.back, e.g., change the last line to `owl:equivalentClass :ThreeServer.` to consider a server as being overloaded if 2 tasks run.
Note that the implementation is *not* guaranteeing that no server is overloaded afterwards, but is used to demonstrate the principle of using a domain-specific query to control the execution of the program.
### Geological System

To run the geological system, run
 ```
./gradlew build
java -jar build/libs/MicroObjects-0.1-SNAPSHOT-all.jar -l examples/geo.smol -r examples/geo.imo -b examples/geo.back
```

Set the first parameter of the earthquake to `0` for a non-sealing fault. The model is not faithful to geological process and is only an illustration of the debugger for a bigger model.
If the example takes too much time, remove the `-b file` parameter to disable OWL inference for all queries.

## Evaluation

To reproduce the numbers from the companion paper, run one of the following commands (with the according value for *n*):
 ```
cd eval
./generate.sh n
cd.. 
java -jar build/libs/MicroObjects-0.1-SNAPSHOT-all.jar -l eval/TTT_rule_n.mo -r eval/eval_rule.imo
java -jar build/libs/MicroObjects-0.1-SNAPSHOT-all.jar -l eval/TTT_class_n.mo -r eval/eval_class.imo
```

## Misc.
 
  * REPL: `validate` and `query-file` use the shell to call the Apache Jena installation, all other commands have no extra dependencies.
  * REPL: If you use `-b` to load background knowledge, OWL reasoning is used for all queries.
  * SMOL: If you use the `query` *statement*, a `List` class with fields `content` and `next` is assumed to exist. The result of the command is a list with all results for the variable `?obj`. (You must use `?obj`, every other variable is dropped.)
  * SMOL: A method modified by `rule` is not allowed to have parameters. 
  * SMOL: Results of type checking are ignored, but output to the user.
  * SMOL: SSA is not type checked.
  * SMOL: `super` refers to the overloaded method, not the instance as its supertype
