package sword.android.graphqlnotes;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.subscription.WebSocketSubscriptionTransport;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import okhttp3.OkHttpClient;

public final class MainActivity extends AppCompatActivity implements View.OnClickListener, DialogInterface.OnClickListener, DialogInterface.OnCancelListener, TextWatcher, ListView.OnItemClickListener, ListView.OnItemLongClickListener {

    private static ApolloClient mApolloClient;
    private boolean mActivityResumed;
    private ApolloSubscriptionCall<NewNoteSubscription.Data> mApolloNewNoteSubscription;

    public static ApolloClient getApolloClient() {
        if (mApolloClient == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
            mApolloClient = ApolloClient.builder()
                    .serverUrl(ProjectConfig.SERVER_URL)
                    .okHttpClient(okHttpClient)
                    .subscriptionTransportFactory(new WebSocketSubscriptionTransport.Factory(ProjectConfig.SERVER_URL, okHttpClient))
                    .build();
        }

        return mApolloClient;
    }

    interface States {
        int READY = 0;
        int CREATING_NOTE = 1;
        int DELETING_NOTE = 2;
    }

    interface SavedKeys {
        String DELETING_NOTE_ID = "dni";
        String PROPOSED_TITLE = "pt";
        String STATE = "st";
    }

    private int mState;
    private String mProposedTitle;
    private String mDeletingNoteId;

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
        mListView.setOnItemLongClickListener(this);

        if (savedInstanceState != null) {
            mState = savedInstanceState.getInt(SavedKeys.STATE);
            mProposedTitle = savedInstanceState.getString(SavedKeys.PROPOSED_TITLE);
            mDeletingNoteId = savedInstanceState.getString(SavedKeys.DELETING_NOTE_ID);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mActivityResumed = true;

        if (mState == States.CREATING_NOTE) {
            displayCreateNoteDialog();
        }
        else if (mState == States.DELETING_NOTE) {
            displayDeleteNoteDialog();
        }
        else {
            queryAllNotes();
        }
    }

    private final class NewNoteSubscriptionCallback implements ApolloSubscriptionCall.Callback<NewNoteSubscription.Data> {

        private final ApolloSubscriptionCall<NewNoteSubscription.Data> mAttachedSubscription;

        NewNoteSubscriptionCallback(ApolloSubscriptionCall<NewNoteSubscription.Data> subscription) {
            mAttachedSubscription = subscription;
        }

        @Override
        public void onResponse(@NotNull Response<NewNoteSubscription.Data> response) {
            Log.i("MainActivity", "Subscription.onResponse triggered");
            final NewNoteSubscription.NewNote newNote = response.data().newNote();
            final NoteEntry entry = new NoteEntry(newNote.id, newNote.title);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final NoteListAdapter adapter = (NoteListAdapter) mListView.getAdapter();
                    if (mApolloNewNoteSubscription == mAttachedSubscription && adapter != null) {
                        adapter.appendNote(entry);
                    }
                }
            });
        }

        @Override
        public void onFailure(@NotNull ApolloException e) {
            Log.i("MainActivity", "Subscription.onFailure triggered");
        }

        @Override
        public void onCompleted() {
            Log.i("MainActivity", "Subscription.onCompleted triggered");
        }

        @Override
        public void onTerminated() {
            Log.i("MainActivity", "Subscription.onTerminated triggered");
        }

        @Override
        public void onConnected() {
            Log.i("MainActivity", "Subscription.onConnected triggered");
        }
    }

    private void triggerNewNoteSubscription() {
        if (mActivityResumed && mListView.getAdapter() != null && mApolloNewNoteSubscription == null) {
            mApolloNewNoteSubscription = getApolloClient().subscribe(NewNoteSubscription.builder().build());
            mApolloNewNoteSubscription.execute(new NewNoteSubscriptionCallback(mApolloNewNoteSubscription));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final NoteEntry entry = (NoteEntry) mListView.getAdapter().getItem(position);
        NoteActivity.open(this, entry.id);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        mDeletingNoteId = ((NoteEntry) mListView.getAdapter().getItem(position)).id;
        mState = States.DELETING_NOTE;
        displayDeleteNoteDialog();
        return true;
    }

    private void displayCreateNoteDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.createNoteDialogTitle)
                .setPositiveButton(R.string.createOption, this)
                .setNegativeButton(R.string.cancelOption, this)
                .setCancelable(false)
                .create();

        final View view = LayoutInflater.from(dialog.getContext()).inflate(R.layout.create_note_dialog, null);
        final EditText editText = view.findViewById(R.id.editText);
        editText.setText(mProposedTitle);
        editText.addTextChangedListener(this);
        dialog.setView(view);
        dialog.show();
    }

    private void displayDeleteNoteDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.deleteNoteDialogMessage)
                .setPositiveButton(R.string.deleteOption, this)
                .setOnCancelListener(this)
                .create().show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                final int state = mState;
                mState = States.READY;

                if (state == States.CREATING_NOTE) {
                    final String title = mProposedTitle;
                    mProposedTitle = null;
                    createNote(title);
                }
                else if (state == States.DELETING_NOTE) {
                    final String noteId = mDeletingNoteId;
                    mDeletingNoteId = null;
                    deleteNote(noteId);
                }
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

    @Override
    public void onCancel(DialogInterface dialog) {
        mState = States.READY;
        mDeletingNoteId = null;

        if (mListView.getAdapter() == null) {
            queryAllNotes();
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
                        triggerNewNoteSubscription();
                    }
                });
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(MainActivity.this, R.string.noteCreatedFeedback, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                showFeedback(R.string.error);
            }
        });
    }

    private void deleteNote(String noteId) {
        getApolloClient().mutate(DeleteNoteMutation.builder().noteId(noteId).build()).enqueue(new ApolloCall.Callback<DeleteNoteMutation.Data>() {
            @Override
            public void onResponse(@NotNull Response<DeleteNoteMutation.Data> response) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.noteDeletedFeedback, Toast.LENGTH_SHORT).show();
                        queryAllNotes();
                    }
                });
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
                Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
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
    public void onPause() {
        if (mApolloNewNoteSubscription != null) {
            mApolloNewNoteSubscription.cancel();
            mApolloNewNoteSubscription = null;
        }
        mActivityResumed = false;

        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SavedKeys.STATE, mState);
        outState.putString(SavedKeys.PROPOSED_TITLE, mProposedTitle);
        outState.putString(SavedKeys.DELETING_NOTE_ID, mDeletingNoteId);
    }
}
