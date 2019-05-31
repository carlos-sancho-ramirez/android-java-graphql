# android-java-graphql_trial

## Objectives of this repo

This repo is aimed to implement a really basic client-server application using GraphQL as communication protocol.

Client is an Android app written in Java. Server is a GraphQL implementation using Spring boot.

This trial has been created after checking the documentation in the following websites:
  * https://www.howtographql.com/
  * https://www.graphql-java.com/tutorials/getting-started-with-spring-boot/

## How to run

1. From the terminal, go to ./server subfolder within this repo.
2. Execute Gradle task bootRun by typing './gradlew bootRun'. When boot up, a query can be performed at http://localhost:8080/graphql

## How to test without running the client

Use curl for that purpose. The following is a sample:
curl 'http://127.0.0.1:8080/graphql' -H 'Accept-Encoding: gzip, deflate, br' -H 'Content-Type: application/json' -H 'Accept: application/json' -H 'Connection: keep-alive' -H 'DNT: 1' -H 'Origin: https://www.graphqlbin.com' --data-binary '{"query":"{\n  bookById(id: \"book-2\") {\n    name\n  }\n}"}' --compressed

In order to compose curl queries easily, visit https://www.graphqlbin.com/
