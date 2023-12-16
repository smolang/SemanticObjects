# Semantic Micro Object Language

This repository contains an interactive interpreter for **SMOL**, a
minimal object-oriented language with integrated semantic state
access.  The interpreter can be used to examine the state with SPARQL,
SHACL and OWL queries.

The language is in development, for a general description, examples and tutorial to SMOL, we refer to [its webpage](https://www.smolang.org).


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