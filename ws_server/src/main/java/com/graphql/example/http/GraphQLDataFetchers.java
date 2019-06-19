package com.graphql.example.http;

import com.graphql.example.http.models.Note;
import com.graphql.example.http.models.Person;
import graphql.schema.DataFetcher;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public final class GraphQLDataFetchers {

    private final Map<String, String> people = new HashMap<>();
    private final Map<String, Note> notes = new HashMap<>();
    private int noteCount = 0;

    private final ArrayList<Note> newNotes = new ArrayList<>();
    private class NewNoteSubscription implements Subscription {

        private final Subscriber<? super Note> subscriber;
        private int lastEventIndex;
        private int lastRequestedEventIndex;
        private boolean canceled;

        private NewNoteSubscription(Subscriber<? super Note> subscriber) {
            this.subscriber = subscriber;
            lastEventIndex = newNotes.size() - 1;
            lastRequestedEventIndex = lastEventIndex;
        }

        @Override
        public void request(long n) {
            if (canceled) {
                throw new UnsupportedOperationException("Subscription already canceled");
            }

            lastRequestedEventIndex += n;
            deliverRequested();
        }

        private void deliverRequested() {
            final int mostRecentNoteEvent = newNotes.size() - 1;
            while (mostRecentNoteEvent > lastEventIndex && lastRequestedEventIndex > lastEventIndex) {
                subscriber.onNext(newNotes.get(++lastEventIndex));
            }
        }

        @Override
        public void cancel() {
            canceled = true;
            newNotePublisher.subscriptions.remove(this);
            subscriber.onComplete();
        }
    }

    private class NewNotePublisher implements Publisher<Note> {

        private final HashSet<NewNoteSubscription> subscriptions = new HashSet<>();

        @Override
        public void subscribe(Subscriber<? super Note> s) {
            final NewNoteSubscription subscription = new NewNoteSubscription(s);
            subscriptions.add(subscription);
            s.onSubscribe(subscription);
        }

        void notifyNewNotes() {
            for (NewNoteSubscription subscription : new ArrayList<>(subscriptions)) {
                subscription.deliverRequested();
            }
        }
    }

    private final NewNotePublisher newNotePublisher = new NewNotePublisher();

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

    public DataFetcher getNoteById() {
        return env -> {
            String noteId = env.getArgument("id");
            return notes.get(noteId);
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
            final Map<String, String> author = env.getArgument("author");
            final String title = env.getArgument("title");
            final String body = env.getArgument("body");

            if (title == null || author == null) {
                return null;
            }

            final String newId = Integer.toString(++noteCount);
            final String authorEmail = author.get("email");
            if (!people.containsKey(authorEmail)) {
                people.put(authorEmail, author.get("name"));
            }

            final Note note = new Note(newId, title, authorEmail);
            note.body = body;
            notes.put(newId, note);

            newNotes.add(note);
            newNotePublisher.notifyNewNotes();

            return note;
        };
    }

    public DataFetcher updateNote() {
        return env -> {
            final String noteId = env.getArgument("noteId");
            final String body = env.getArgument("body");

            if (noteId == null) {
                return null;
            }

            Note note = notes.get(noteId);
            if (note == null) {
                return null;
            }

            note.body = body;
            return note;
        };
    }

    public DataFetcher deleteNote() {
        return env -> {
            final String noteId = env.getArgument("noteId");
            return (noteId != null)? notes.remove(noteId) : null;
        };
    }

    public DataFetcher newNote() {
        return env -> newNotePublisher;
    }
}
