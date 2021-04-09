# To build the container:
#     docker build -t smol .
# To run smol in the current directory:
#     docker run -it --rm -v "$PWD":/root/smol smol
FROM openjdk:11
RUN apt-get -y update \
    && apt-get -y install python3 python3-pip \
    && rm -rf /var/lib/apt/lists/*
RUN pip3 install pyzmq
COPY . /usr/local/src/smol
WORKDIR /usr/local/src/smol
RUN ./gradlew --no-daemon build
WORKDIR /root/smol
CMD ["java", "-jar", "/usr/local/src/smol/build/libs/MicroObjects-0.2-all.jar"]
