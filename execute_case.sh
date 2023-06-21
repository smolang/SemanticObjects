#!/bin/bash
./gradlew shadowJar
java -jar build/libs/smol.jar -i examples/Geological/simulate_onto.smol -v -e -b examples/Geological/total_mini.ttl -p UFRGS1=https://www.inf.ufrgs.br/bdi/ontologies/geocoreontology#UFRGS -p obo=http://purl.obolibrary.org/obo/ -d http://www.semanticweb.org/quy/ontologies/2023/2/untitled-ontology-38#
