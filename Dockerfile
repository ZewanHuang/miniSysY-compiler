FROM openjdk:15

WORKDIR /app/
COPY src ./
RUN javac -encoding UTF-8 -d ./output Main.java
WORKDIR /app/output/