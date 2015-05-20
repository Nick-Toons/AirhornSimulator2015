package com.herrschreiber.airhornsimulator2015;

import android.app.SearchManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class SongsActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, SearchView.OnQueryTextListener {
    public static final String KEY_SOUND_NAME = "SOUND_NAME";
    private static final String TAG = "SongsActivity";
    @InjectView(R.id.songs)
    protected GridView songList;
    private SoundPlayer soundPlayer;
    private SongsListAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songs);
        ButterKnife.inject(this);

        String soundName = getIntent().getStringExtra(KEY_SOUND_NAME);

        soundPlayer = ((AirhornSimulatorApplication) getApplication()).getSoundPlayer();
        SongParser songParser = ((AirhornSimulatorApplication) getApplication()).getSongParser();
        try {
            songParser.parseSongs();
        } catch (IOException e) {
            Log.e(TAG, "Error parsing songs", e);
            return;
        }
        List<Song> songs = songParser.getSongs();

        AssetSound sound = soundPlayer.getSounds().get(soundName);

        List<SampledSongSound> sampledSongs = new ArrayList<>();
        for (Song song : songs) {
            SampledSongSound sampledSong = new SampledSongSound(sound.getName() + " - " + song.getName(), sound, song);
            sampledSongs.add(sampledSong);
        }

        listAdapter = new SongsListAdapter(this, sampledSongs);
        songList.setAdapter(listAdapter);
        songList.setOnItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(false);
        searchView.setIconified(false);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_stop:
                soundPlayer.stop();
                break;
            case R.id.action_settings:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final SampledSongSound sound = (SampledSongSound) parent.getItemAtPosition(position);
        if (sound.hasInitialized()) {
            soundPlayer.playSound(sound);
        } else {
            final SongsListAdapter.ViewHolder viewHolder = (SongsListAdapter.ViewHolder) view.getTag();
            if (!viewHolder.isInitializing) {
                final ViewSwitcher viewSwitcher = (ViewSwitcher) view;
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected void onPreExecute() {
                        viewHolder.isInitializing = true;
                        viewSwitcher.showNext();
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        viewHolder.isInitializing = false;
                        viewSwitcher.showPrevious();
                        soundPlayer.playSound(sound);
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            sound.initialize();
                        } catch (IOException e) {
                            Log.e(TAG, "Error initializing sound", e);
                        }
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        listAdapter.getFilter().filter(newText);
        return true;
    }

    public static class SongsListAdapter extends BaseAdapter implements Filterable {
        private final Context context;
        private final List<SampledSongSound> songs;
        private List<SampledSongSound> filteredSongs;
        private Filter filter;

        public SongsListAdapter(Context context, List<SampledSongSound> songs) {
            this.context = context;
            this.songs = songs;
            filteredSongs = songs;
            filter = new SongFilter();
        }

        @Override
        public int getCount() {
            return filteredSongs.size();
        }

        @Override
        public SampledSongSound getItem(int position) {
            return filteredSongs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder viewHolder;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_song, parent, false);
                viewHolder = new ViewHolder(view);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            final SampledSongSound item = getItem(position);
            viewHolder.songName.setText(item.getName());
            return view;
        }

        @Override
        public Filter getFilter() {
            return filter;
        }

        public class ViewHolder {
            @InjectView(R.id.song_icon)
            public ImageView songIcon;
            @InjectView(R.id.song_name)
            public TextView songName;

            public boolean isInitializing;

            public ViewHolder(View view) {
                ButterKnife.inject(this, view);
            }
        }

        private class SongFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null && constraint.length() > 0) {
                    ArrayList<SampledSongSound> filtered = new ArrayList<>();

                    for (SampledSongSound song : songs) {
                        if (song.getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
                            filtered.add(song);
                        }
                    }

                    filterResults.count = filtered.size();
                    filterResults.values = filtered;
                } else {
                    filterResults.count = songs.size();
                    filterResults.values = songs;
                }

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredSongs = (List<SampledSongSound>) results.values;
                notifyDataSetChanged();
            }
        }
    }
}
