# android-java-graphql_trial

## Objectives of this repo

This repo is aimed to implement a really basic client-server application using GraphQL as communication protocol.

Client is an Android app written in Java using apollo library.

There is currently 2 server implementation.
The server in *server* folder is a really basic GraphQL implementation over HTTP using Spring boot. This implementation does not allow subscriptions.
Server in *ws_server* folder is a GraphQL implementation using jetty. This is the recommended one as it allow subscriptions through WebSockets.

This trial has been created after checking the documentation in the following websites:
  * https://www.howtographql.com/
  * https://www.graphql-java.com/tutorials/getting-started-with-spring-boot/

## Quick explanation on the test application

The server will be able to memorize notes and their authors, and allow query any client connecting to it about any note content.

The client of this server should be able to create, update and delete notes, using GraphQL over HTTP.

If the client subscribes, it will be able to receive an event every time a note is created.

## How to run the jetty sever (recommended)

1. From the terminal, go to ./ws_server subfolder within this repo.
2. Execute Gradle task run by typing './gradlew run'. When boot up, a query can be performed at http://localhost:8080/graphql

## How to run the Spring boot server (not recommended, use the jetty server instead)

1. From the terminal, go to ./server subfolder within this repo.
2. Execute Gradle task bootRun by typing './gradlew bootRun'. When boot up, a query can be performed at http://localhost:8080/graphql

## How to test the server for queries and mutations without running the client

### Using curl

The following is a sample:
curl 'http://127.0.0.1:8080/graphql' -H 'Accept-Encoding: gzip, deflate, br' -H 'Content-Type: application/json' -H 'Accept: application/json' -H 'Connection: keep-alive' -H 'DNT: 1' -H 'Origin: https://www.graphqlbin.com' --data-binary '{"query":"{\n  bookById(id: \"book-2\") {\n    name\n  }\n}"}' --compressed

### Using the Altair Google Chrome plugin

In order to use this plugin, Google Chrome web browser must be installed.
Visit https://chrome.google.com/webstore/detail/altair-graphql-client/flnheeellpciglgpaodhkhmapeljopja from Google chrome browser in order to install the plugin.

### Using online browser based clients

Visit https://www.graphqlbin.com/

## How to test the server for subscriptions without running the client

A simple way to test if the WebSocket is up and running is by using the echo test in www.websocket.org. Just change the URL to ws://localhost:8080/graphql. Note that the jetty server is ready to process graphql queries from websockets. Unexpected behaviour may happen if the JSON messages sent do not match the expected schema.

## How the Android client works

Android project is adding the Apollo GraphQL's Gradle plugin. Visit https://github.com/apollographql/apollo-android for more instructions.
The gradle plugin loads a JSON file extracted from the schema (schema.json), and different queries (all under src/min/graphql folder) and generates java models according to the schema in compile time.
Important note: schema.json is a really specific file required by apollo, it can be extracted by executing the following command when the server is running:
    apollo schema:download --endpoint=http://localhost:8080/graphql schema.json

Apollo and its Gradle plugin requires node.js 8.0.0 or higher. It must be installed in the system in order to compile this project.

## How to run the client

Before you can compile the Android client, you need to introduce the URL where the server is running.
Open android_client/app/src/main/java/sword/android/graphqlnotes/ProjectConfig.java and change the comment by the url.
So, for example, if the machine running the server has IP 192.168.0.2, type 'http://192.168.0.2:8080/graphql'

As any other Android project, gradle can be used to generate its APK, just run ./gradlew assembleDebug and a debug APK will be generated in android_client/app/build/.
Just install it on any Android device or emulator.

