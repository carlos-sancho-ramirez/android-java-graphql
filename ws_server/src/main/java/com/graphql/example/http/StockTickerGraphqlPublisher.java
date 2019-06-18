package com.graphql.example.http;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class StockTickerGraphqlPublisher {
    private final GraphQLSchema graphQLSchema;
    private GraphQLDataFetchers graphQLDataFetchers = new GraphQLDataFetchers();

    public StockTickerGraphqlPublisher() {
        graphQLSchema = buildSchema();
    }

    private GraphQLSchema buildSchema() {
        Reader streamReader = loadSchemaFile("schema.graphqls");
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(streamReader);
        return new SchemaGenerator().makeExecutableSchema(typeRegistry, buildWiring());
    }

    private RuntimeWiring buildWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("allPeople", graphQLDataFetchers.getAllPeople())
                        .dataFetcher("allNotes", graphQLDataFetchers.getAllNotes())
                        .dataFetcher("noteById", graphQLDataFetchers.getNoteById())
                )
                .type(newTypeWiring("Mutation")
                        .dataFetcher("createPerson", graphQLDataFetchers.createPerson())
                        .dataFetcher("createNote", graphQLDataFetchers.createNote())
                        .dataFetcher("updateNote", graphQLDataFetchers.updateNote())
                        .dataFetcher("deleteNote", graphQLDataFetchers.deleteNote())
                )
                .type(newTypeWiring("Subscription")
                        .dataFetcher("newNote", graphQLDataFetchers.newNote())
                )
                .type(newTypeWiring("Person")
                        .dataFetcher("email", graphQLDataFetchers.getPersonEmail())
                        .dataFetcher("name", graphQLDataFetchers.getPersonName())
                )
                .type(newTypeWiring("Note")
                        .dataFetcher("author", graphQLDataFetchers.getNoteAuthor())
                )
                .build();
    }

    public GraphQLSchema getGraphQLSchema() {
        return graphQLSchema;
    }

    @SuppressWarnings("SameParameterValue")
    private Reader loadSchemaFile(String name) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        return new InputStreamReader(stream);
    }

}
