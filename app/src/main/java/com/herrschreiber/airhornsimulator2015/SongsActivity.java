package com.herrschreiber.airhornsimulator2015;

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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class SongsActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
    public static final String KEY_SOUND_NAME = "SOUND_NAME";
    private static final String TAG = "SongsActivity";
    @InjectView(R.id.songs)
    protected ListView songList;
    private SoundPlayer soundPlayer;

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

        songList.setAdapter(new SongsListAdapter(this, sampledSongs));
        songList.setOnItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
            final ViewSwitcher viewSwitcher = (ViewSwitcher) view;
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected void onPreExecute() {
                    viewSwitcher.showNext();
                }

                @Override
                protected void onPostExecute(Void aVoid) {
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

    public static class SongsListAdapter extends ArrayAdapter<SampledSongSound> {
        public SongsListAdapter(Context context, List<SampledSongSound> sounds) {
            super(context, R.layout.item_song, sounds);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder viewHolder;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getContext()
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

        public class ViewHolder {
            @InjectView(R.id.song_icon)
            public ImageView songIcon;
            @InjectView(R.id.song_name)
            public TextView songName;

            public ViewHolder(View view) {
                ButterKnife.inject(this, view);
            }
        }
    }
}
