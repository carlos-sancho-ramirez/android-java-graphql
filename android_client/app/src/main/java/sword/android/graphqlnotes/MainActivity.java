package sword.android.graphqlnotes;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import org.jetbrains.annotations.NotNull;

import java.net.Proxy;
import java.util.ArrayList;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

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
        getApolloClient().query(AllNotesQuery.builder().build()).enqueue(new ApolloCall.Callback<AllNotesQuery.Data>() {
            @Override
            public void onResponse(@NotNull Response<AllNotesQuery.Data> response) {
                final ArrayList<String> noteTitles = new ArrayList<>();
                for (AllNotesQuery.AllNote note : response.data().allNotes()) {
                    noteTitles.add(note.title());
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

    @Override
    public void onClick(View v) {
        Toast.makeText(this, "To be implemented", Toast.LENGTH_SHORT).show();
    }
}
