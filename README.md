 # Semantic Micro Object Language
 This repository contains an interactive interpreter for **SMOL**, a minimal object-oriented language with integrated semantic state access.
 The interpreter can be used to examine the state with SPARQL, SHACL and OWL queries.
 
 The project is in a very early stage, and the language performs almost no checks on its input.
 Tested only on Linux.
 
 ## Examples
Enter `help` for an overview over the available commands.
 
 * examples/double.mo contains a simple doubly linked list example.
 * examples/double.rq contains a SPARQL query to select all `List` elements.
 * examples/double.ttl contains a SHACL query that ensures that all objects implement a class.
 * examples/double.imo contains a simple test session.
 
To run the geo session, run
 ```
./gradlew shadowJar
java -jar build/libs/MicroObjects-0.1-SNAPSHOT-all.jar  -l examples/geo.mo -r examples/geo.imo -b examples/geo.back
```

Set the first parameter of the earthquake to `0` for a non-sealing fault. The model is not faithful to geological process and barely illustrates the debugger.
If the example takes too much time, remove the `-b file` parameter to disable OWL inference for all queries.

To run the test session that demonstrates how inference drives simulation run 
 ```
./gradlew shadowJar
java -jar build/libs/MicroObjects-0.1-SNAPSHOT-all.jar  -l examples/simulate.mo -r examples/simulate.imo
```
Note how the result is an execution of the method `D.n` triggered from the inference engine, not the code. 
Currently, the `rule` modifier is likely to cause errors in other programs, use with care.   

To run the general test session and continue interactively, run
 ```
./gradlew shadowJar
java -jar build/libs/MicroObjects-0.1-SNAPSHOT-all.jar -j </path/to/jena/> -l examples/double.mo -r examples/double.imo
```

 ## Misc.
 
  * REPL: `validate` and `query-file` use the shell to call the Apache Jena installation, all other commands have no extra dependencies.
  * REPL: If you use `-b` to load background knowledge, OWL reasoning is used for all queries.
  * SMOL: If you use the `query` *statement*, a `List` class with fields `content` and `next` is assumed to exist. The result of the command is  list with all results for the variable `?obj`. (You must use `?obj`, every other variable is dropped.)
  * SMOL: If you use `rule` modifiers, the rules are invoked for every process.
  * SMOL: A method modified by `rule` is not allowed to have parameters. 