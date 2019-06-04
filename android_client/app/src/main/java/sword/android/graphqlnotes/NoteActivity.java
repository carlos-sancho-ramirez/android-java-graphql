package sword.android.graphqlnotes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.Toast;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import org.jetbrains.annotations.NotNull;

public final class NoteActivity extends AppCompatActivity {

    interface ArgKeys {
        String ID = "id";
    }

    static void open(Context context, String noteId) {
        final Intent intent = new Intent(context, NoteActivity.class);
        intent.putExtra(ArgKeys.ID, noteId);
        context.startActivity(intent);
    }

    private EditText mEditText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        mEditText = findViewById(R.id.editText);

        queryNoteContent();
    }

    private void queryNoteContent() {
        final String noteId = getIntent().getStringExtra(ArgKeys.ID);
        MainActivity.getApolloClient().query(NoteContentQuery.builder().noteId(noteId).build()).enqueue(new ApolloCall.Callback<NoteContentQuery.Data>() {
            @Override
            public void onResponse(@NotNull Response<NoteContentQuery.Data> response) {
                final String title = response.data().noteById().title;
                final String body = response.data().noteById().body;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setTitle(title);
                        mEditText.setText(body);
                    }
                });
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(NoteActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
