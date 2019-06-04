package sword.android.graphqlnotes;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import okhttp3.OkHttpClient;

public final class MainActivity extends AppCompatActivity implements View.OnClickListener, DialogInterface.OnClickListener, TextWatcher, ListView.OnItemClickListener {

    private static ApolloClient mApolloClient;

    public static ApolloClient getApolloClient() {
        if (mApolloClient == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
            mApolloClient = ApolloClient.builder()
                    .serverUrl(ProjectConfig.SERVER_URL)
                    .okHttpClient(okHttpClient)
                    .build();
        }

        return mApolloClient;
    }

    interface States {
        int READY = 0;
        int CREATING_NOTE = 1;
    }

    interface SavedKeys {
        String STATE = "st";
        String PROPOSED_TITLE = "pt";
    }

    private int mState;
    private String mProposedTitle;

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(this);

        mListView = findViewById(R.id.listView);
        mListView.setOnItemClickListener(this);

        if (savedInstanceState != null) {
            mState = savedInstanceState.getInt(SavedKeys.STATE);
            mProposedTitle = savedInstanceState.getString(SavedKeys.PROPOSED_TITLE);
        }

        if (mState == States.CREATING_NOTE) {
            displayCreateNoteDialog();
        }
        else {
            queryAllNotes();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final NoteEntry entry = (NoteEntry) mListView.getAdapter().getItem(position);
        NoteActivity.open(this, entry.id);
    }

    private void displayCreateNoteDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Type the new note title")
                .setPositiveButton("Create", this)
                .setNegativeButton("Cancel", this)
                .setCancelable(false)
                .create();

        final View view = LayoutInflater.from(dialog.getContext()).inflate(R.layout.create_note_dialog, null);
        final EditText editText = view.findViewById(R.id.editText);
        editText.setText(mProposedTitle);
        editText.addTextChangedListener(this);
        dialog.setView(view);
        dialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                mState = States.READY;
                final String title = mProposedTitle;
                mProposedTitle = null;
                createNote(title);
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                mState = States.READY;
                mProposedTitle = null;

                if (mListView.getAdapter() == null) {
                    queryAllNotes();
                }
                break;
        }
    }

    private void queryAllNotes() {
        getApolloClient().query(AllNotesQuery.builder().build()).enqueue(new ApolloCall.Callback<AllNotesQuery.Data>() {
            @Override
            public void onResponse(@NotNull Response<AllNotesQuery.Data> response) {
                final ArrayList<NoteEntry> noteTitles = new ArrayList<>();
                for (AllNotesQuery.AllNote note : response.data().allNotes()) {
                    noteTitles.add(new NoteEntry(note.id(), note.title()));
                }

                final NoteListAdapter adapter = new NoteListAdapter(noteTitles);
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        mListView.setAdapter(adapter);
                    }
                });
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void createNote(String title) {
        getApolloClient().mutate(CreateNoteMutation.builder().title(title).build()).enqueue(new ApolloCall.Callback<CreateNoteMutation.Data>() {
            @Override
            public void onResponse(@NotNull Response<CreateNoteMutation.Data> response) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        queryAllNotes();
                    }
                });
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View v) {
        mState = States.CREATING_NOTE;
        displayCreateNoteDialog();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Nothing to be done
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Nothing to be done
    }

    @Override
    public void afterTextChanged(Editable s) {
        mProposedTitle = s.toString();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SavedKeys.STATE, mState);
        outState.putString(SavedKeys.PROPOSED_TITLE, mProposedTitle);
    }
}
