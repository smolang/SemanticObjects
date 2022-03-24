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
java -jar build/libs/MicroObjects-0.2-all.jar --help
```

To compile and run the SMOL REPL inside docker, run
```
docker build -t smol .
docker run -it --rm -v "$PWD":/root/smol smol
```

Inside the REPL, enter `help` for an overview over the available commands.

To run the digital shadows scenarios, use the examples under examples/Shadow.