package sword.android.graphqlnotes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
                showFeedback(R.string.error);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.saveOption:
                saveNote();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveNote() {
        final String noteId = getIntent().getStringExtra(ArgKeys.ID);
        final String text = mEditText.getText().toString();
        MainActivity.getApolloClient().mutate(UpdateNoteMutation.builder().noteId(noteId).body(text).build()).enqueue(new ApolloCall.Callback<UpdateNoteMutation.Data>() {
            @Override
            public void onResponse(@NotNull Response<UpdateNoteMutation.Data> response) {
                showFeedback(R.string.noteSavedFeedback);
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                showFeedback(R.string.error);
            }
        });
    }

    private void showFeedback(@StringRes int text) {
        final int toastText = text;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(NoteActivity.this, toastText, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
