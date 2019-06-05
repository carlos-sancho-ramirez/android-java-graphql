package com.graphqljava.tutorial.bookdetails;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class GraphQLProvider {

    private GraphQL graphQL;

    @Autowired
    GraphQLDataFetchers graphQLDataFetchers;

    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }

    @PostConstruct
    public void init() throws IOException {
        URL url = Resources.getResource("schema.graphqls");
        String sdl = Resources.toString(url, Charsets.UTF_8);
        GraphQLSchema graphQLSchema = buildSchema(sdl);
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    private GraphQLSchema buildSchema(String sdl) {
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        RuntimeWiring runtimeWiring = buildWiring();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }

    private RuntimeWiring buildWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("allPeople", graphQLDataFetchers.getAllPeople())
                        .dataFetcher("allNotes", graphQLDataFetchers.getAllNotes())
                        .dataFetcher("bookById", graphQLDataFetchers.getBookById())
                        .dataFetcher("noteById", graphQLDataFetchers.getNoteById())
                )
                .type(newTypeWiring("Mutation")
                        .dataFetcher("createPerson", graphQLDataFetchers.createPerson())
                        .dataFetcher("createNote", graphQLDataFetchers.createNote())
                        .dataFetcher("updateNote", graphQLDataFetchers.updateNote())
                        .dataFetcher("deleteNote", graphQLDataFetchers.deleteNote())
                )
                .type(newTypeWiring("Book")
                        .dataFetcher("author", graphQLDataFetchers.getAuthor())
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
}
