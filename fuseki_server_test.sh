#!/bin/bash

# Build the fuseki image
docker build -f DockerfileFuseki -t fuseki .

# Run the fuseki image
docker run  -d --name fuseki_container -it --rm -v "$PWD":/root/smol -p 3030:3030 fuseki

# Define the FUSEKI_SERVER environment variable
export FUSEKI_DOCKER=true

# Execute the test
./gradlew --stop
./gradlew test --tests no.uio.microobject.test.data.TripleManagerTest

# Stop the fuseki server
kill $(ps aux | grep '[f]useki' | awk '{print $2}')

# Stop the Docker container when done testing (optional)
docker stop fuseki_container
docker kill fuseki_container # to kill the container if docker stop doesn't work
docker rm fuseki_container

# Delete the fuseki image
docker rmi fuseki
