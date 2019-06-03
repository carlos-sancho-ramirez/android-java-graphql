package com.graphqljava.tutorial.bookdetails;

import com.google.common.collect.ImmutableMap;
import com.graphqljava.tutorial.bookdetails.models.Note;
import com.graphqljava.tutorial.bookdetails.models.Person;
import graphql.schema.DataFetcher;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GraphQLDataFetchers {

    private static List<Map<String, String>> books = Arrays.asList(
            ImmutableMap.of("id", "book-1",
                    "name", "Harry Potter and the Philosopher's Stone",
                    "pageCount", "223",
                    "authorId", "author-1"),
            ImmutableMap.of("id", "book-2",
                    "name", "Moby Dick",
                    "pageCount", "635",
                    "authorId", "author-2"),
            ImmutableMap.of("id", "book-3",
                    "name", "Interview with the vampire",
                    "pageCount", "371",
                    "authorId", "author-3")
    );

    private static List<Map<String, String>> authors = Arrays.asList(
            ImmutableMap.of("id", "author-1",
                    "firstName", "Joanne",
                    "lastName", "Rowling"),
            ImmutableMap.of("id", "author-2",
                    "firstName", "Herman",
                    "lastName", "Melville"),
            ImmutableMap.of("id", "author-3",
                    "firstName", "Anne",
                    "lastName", "Rice")
    );

    private final HashMap<String, String> people = new HashMap<>();
    private final HashMap<String, Note> notes = new HashMap<>();
    private int noteCount = 0;

    public DataFetcher getBookByIdDataFetcher() {
        return dataFetchingEnvironment -> {
            String bookId = dataFetchingEnvironment.getArgument("id");
            return books
                    .stream()
                    .filter(book -> book.get("id").equals(bookId))
                    .findFirst()
                    .orElse(null);
        };
    }

    public DataFetcher getAuthorDataFetcher() {
        return dataFetchingEnvironment -> {
            Map<String,String> book = dataFetchingEnvironment.getSource();
            String authorId = book.get("authorId");
            return authors
                    .stream()
                    .filter(author -> author.get("id").equals(authorId))
                    .findFirst()
                    .orElse(null);
        };
    }

    public DataFetcher getAllPeople() {
        return env -> {
            final ArrayList<Person> list = new ArrayList<>();
            for (String key : people.keySet()) {
                list.add(new Person(key, people.get(key)));
            }

            return list;
        };
    }

    public DataFetcher getAllNotes() {
        return env -> {
            final ArrayList<Note> list = new ArrayList<>();
            for (Note note : notes.values()) {
                list.add(note);
            }

            return list;
        };
    }

    public DataFetcher getPersonEmail() {
        return env -> {
            Person person = env.getSource();
            return person.email;
        };
    }

    public DataFetcher getPersonName() {
        return env -> {
            Person person = env.getSource();
            return person.name;
        };
    }

    public DataFetcher getNoteAuthor() {
        return env -> {
            Note note = env.getSource();
            final String email = note.authorEmail;
            return new Person(email, people.get(email));
        };
    }

    public DataFetcher createPerson() {
        return env -> {
            final String email = env.getArgument("email");
            final String name = env.getArgument("name");
            if (email == null || name == null || people.containsKey(email)) {
                return null;
            }

            people.put(email, name);
            return new Person(email, name);
        };
    }

    public DataFetcher createNote() {
        return env -> {
            final String title = env.getArgument("title");
            final String authorEmail = env.getArgument("authorEmail");
            if (title == null || authorEmail == null) {
                return null;
            }

            final String newId = Integer.toString(++noteCount);
            final Note note = new Note(newId, title, authorEmail);
            notes.put(newId, note);

            return note;
        };
    }
}
