package com.graphql.example.http.models;

public class Note {
    public final String id;
    public final String title;
    public final String authorEmail;
    public String body;

    public Note(String id, String title, String authorEmail) {
        this.id = id;
        this.title = title;
        this.authorEmail = authorEmail;
    }
}
