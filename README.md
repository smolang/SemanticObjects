# Lazy Semantic Micro Object Language

This repository contains an interactive interpreter for **LMOL**, a
minimal object-oriented language with integrated semantic state
access.  The interpreter can be used to examine the state with SPARQL,
SHACL and OWL queries.

This branch contains the lazy loading mechanism on top of the semantical lifting of **SMOL**.
Tested only on Linux. 

To compile and run the LMOL REPL, run
```
./gradlew build
java -jar build/libs/MicroObjects-0.2-all.jar --help
```

To compile and run the LMOL REPL inside docker, run
```
docker build -t smol .
docker run -it --rm -v "$PWD":/root/smol smol
```

Inside the REPL, enter `help` for an overview over the available commands.

## Examples
A number of examples are available under examples/.

### Performance
To run the performance evaluation execute the Python 3 script `eval/evaluation.py`. This requires the Python package `pexpect`. Run
```
python3 eval/evaluate_lmol.py scenario1
python3 eval/evaluate_lmol.py scenario2
```
After running either scenario the results will be written to `eval/result-lazyChain.tsv`.

### Modelling
The classes for the Slegge queries are in `slegge.smol`. For comparison, the queries themselves are in `slegge.txt`. 
Taken from `http://purl.org/slegge`.


