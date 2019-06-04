package sword.android.graphqlnotes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

final class NoteListAdapter extends BaseAdapter {

    private final List<String> mTexts;
    private LayoutInflater mInflater;

    NoteListAdapter(List<String> texts) {
        mTexts = texts;
    }

    @Override
    public int getCount() {
        return mTexts.size();
    }

    @Override
    public String getItem(int position) {
        return mTexts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            if (mInflater == null) {
                mInflater = LayoutInflater.from(parent.getContext());
            }

            view = mInflater.inflate(R.layout.note_list_entry, parent, false);
        }

        final TextView textView = view.findViewById(R.id.text);
        textView.setText(mTexts.get(position));

        return view;
    }
}
