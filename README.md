# Online Monitoring

This repository contains the branch of **SMOL** that implements the online verification of reconfigurable digital twin scenarios,
including the experiments for the performance. General description follows below. To run one experiment, run the following commands (tested with bash and Ubuntu 20.04).

```
./gradlew build
java -jar build/libs/smol.jar -e -v -i examples/Electro/electrotwice.smol | tee examples/Electro/eval/output
```

If the command does not terminate, Ctrl+Z after `"finished execution"` has been printed.
To get the numbers per iteration, execute

```
cd examples/Electro/eval/
./execute.sh
```




# Semantic Micro Object Language

This repository contains an interactive interpreter for **SMOL**, a
minimal object-oriented language with integrated semantic state
access.  The interpreter can be used to examine the state with SPARQL,
SHACL and OWL queries.

The language is in development. The version described by our [technical report](https://ebjohnsen.org/publication/rr499.pdf) is available under the commit  [351ee57](https://github.com/Edkamb/SemanticObjects/commit/351ee5723b916dd9b52a89e4608615e02443da96).
 
The project is in an early stage, and the interpreter performs little checks on its input. 
In particular, there is no type system, and it is not checked that a `return` ends every path in the control flow.
Input has to be in turtle syntax (except the `class` command of the REPL, which is Manchester syntax).
Tested only on Linux. 

To compile and run the SMOL REPL, run
```
./gradlew build
java -jar build/libs/smol.jar --help
```

To run the SMOL REPL pre-compiled using docker, run the following command:
```
docker run -it --rm -v "$PWD":/root/smol ghcr.io/smolang/smol:latest
```

To compile and run the SMOL REPL inside docker, run the following commands:
```
docker build -t smol .
docker run -it --rm -v "$PWD":/root/smol smol
```

Inside the REPL, enter `help` for an overview over the available commands.

## Examples
A number of examples are available under examples/.
Additionally, eval/ contains the files used for performance evaluation and domain.pdf gives the used domain model.


### Cloud Platforms

* src/test/kotlin/resources/overload.mo contains a simple demo for ontology-mediated programming
* examples/overload.back defines a server as overloaded if it has 3 tasks running
* examples/overload.imo has a sample session that queries all overlaoded servers, reschedules and shows that no servers are overloaded afterwards

To execute it, run the following.
```
./gradlew assemble
java -jar build/libs/smol.jar -i src/test/resources/overload.smol -r examples/overload.imo -b examples/overload.back 
```

To change the interpretation of `:Overloaded` modify examples/overload.back, e.g., change the last line to `owl:equivalentClass :ThreeServer.` to consider a server as being overloaded if 2 tasks run.
The implementation is not guaranteeing that no server is overloaded afterwards, but is used to demonstrate the principle of using a domain-specific query to control the execution of the program.

### FMUs 
To execute an example for Hybrid SMOL, download the required FMUs as described in `examples/SimulationDemo/lv-simple.smol` and run the following
```
./gradlew assemble
java -jar build/libs/smol.jar -i examples/SimulationDemo/lv-simple.smol -e
```
### Geometric Scene

* examples/scene.mo contains an example to illustrate `rule`, the computational semantic state access (CSSA).
* examples/scene.imo contain a sample session.

To execute it, run the following.
```
./gradlew assemble
java -jar build/libs/smol.jar -i src/test/resources/scene.smol -r examples/scene.imo
```

### 2-3 Trees

* src/test/kotlin/resources/TwoThreeTree.smol contains an implementation of 2-3 trees
* examples/TwoThreeTree.back contains the domain knowledge as a set of OWL classes
* examples/tree_shapes.ttl contains a SHACL query that ensures that all objects implement a class.
* examples/TTT.imo contains an example session (The one from the companion paper).

To execute it, run the following. 
```
./gradlew build
java -jar build/libs/smol.jar -i src/test/resources/TwoThreeTree.smol -b examples/TwoThreeTree.back -r examples/double.imo 
```




### Doubly Linked Lists

 * src/test/kotlin/resources/double.smol contains a simple doubly linked list example.
 * examples/double.rq contains a SPARQL query to select all `List` elements.
 * examples/double.ttl contains a SHACL query that ensures that all objects implement a class.
 * examples/double.imo contains a simple test session.

To execute it, run the following. 
```
./gradlew build
java -jar build/libs/smol.jar -i src/test/resources/double.smol -r examples/double.imo 
```




## Misc.
  * SMOL: Results of type checking are ignored, but output to the user.
  * SMOL: `super` refers to the overloaded method, not the instance as its supertype.
  * General: To run the simulation example, python3 and the zmq package must be installed. 
