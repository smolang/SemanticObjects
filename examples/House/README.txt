These examples need to run on Linux or in docker.

To run these examples, start the SMOL REPL at the root directory of the
project so that the paths to the FMUs and ttl file resolve:

    $ docker run -it --rm -v "$PWD":/root/smol smol
    Interactive shell started.
    MO> read examples/House/osphouseV2.smol

To prepare the docker image, run the following command in the root directory of the project:

    $ docker build -t smol .
