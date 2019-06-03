# android-java-graphql_trial

## Objectives of this repo

This repo is aimed to implement a really basic client-server application using GraphQL as communication protocol.

Client is an Android app written in Java using apollo library. Server is a GraphQL implementation using Spring boot.

This trial has been created after checking the documentation in the following websites:
  * https://www.howtographql.com/
  * https://www.graphql-java.com/tutorials/getting-started-with-spring-boot/

## How to run the server

1. From the terminal, go to ./server subfolder within this repo.
2. Execute Gradle task bootRun by typing './gradlew bootRun'. When boot up, a query can be performed at http://localhost:8080/graphql

## How to test the server without running the client

### Using curl

The following is a sample:
curl 'http://127.0.0.1:8080/graphql' -H 'Accept-Encoding: gzip, deflate, br' -H 'Content-Type: application/json' -H 'Accept: application/json' -H 'Connection: keep-alive' -H 'DNT: 1' -H 'Origin: https://www.graphqlbin.com' --data-binary '{"query":"{\n  bookById(id: \"book-2\") {\n    name\n  }\n}"}' --compressed

### Using the Altair Google Chrome plugin

In order to use this plugin, Google Chrome web browser must be installed.
Visit https://chrome.google.com/webstore/detail/altair-graphql-client/flnheeellpciglgpaodhkhmapeljopja from Google chrome browser in order to install the plugin.

### Using online browser based clients

Visit https://www.graphqlbin.com/

## How the Android client works

Android projects in adding the Apollo GraphQL's Gradle plugin. Visit https://github.com/apollographql/apollo-android for more instructions.
The gradle plugin loads a JSON file extracted from the schema (schema.json), and deferent queries (all under src/min/graphql folder) and generates java models according to the schema in compile time.
Important note: schema.json is a really specific file required by apollo, it can be extracted by executing the following command when the server is running:
    apollo schema:download --endpoint=http://localhost:8080/graphql schema.json

Apollo and its Gradle plugin requires node.js 8.0.0 or higher. It must be installed in the system in order to compile this project.
