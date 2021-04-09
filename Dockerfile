# To build the container:
#     docker build -t smol .
# To run smol in the current directory:
#     docker run -it --rm -v "$PWD":/root/smol smol
FROM openjdk:11
COPY . /usr/local/src/smol
WORKDIR /usr/local/src/smol
RUN ./gradlew --no-daemon build
WORKDIR /root/smol
CMD ["java", "-jar", "/usr/local/src/smol/build/libs/MicroObjects-0.2-all.jar"]
