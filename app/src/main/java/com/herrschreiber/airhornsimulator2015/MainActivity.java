package com.herrschreiber.airhornsimulator2015;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";
    @InjectView(R.id.sounds)
    protected GridView soundsList;
    private SoundPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        player = new SoundPlayer();
        try {
            player.loadSounds(this);
        } catch (IOException e) {
            Log.e(TAG, "Error loading sounds", e);
        }
        Map<String, Sound> sounds = player.getSounds();
        List<NoteInfo> song = new ArrayList<>();
        song.add(new NoteInfo(0.0, 0.5, 261.63, 1.0));
        song.add(new NoteInfo(0.5, 0.5, 293.66, 1.0));
        song.add(new NoteInfo(1.0, 0.5, 329.63, 1.0));
        song.add(new NoteInfo(1.5, 0.5, 349.23, 1.0));
        song.add(new NoteInfo(2.0, 0.5, 392.00, 1.0));
        song.add(new NoteInfo(2.5, 0.5, 440.00, 1.0));
        song.add(new NoteInfo(3.0, 0.5, 493.88, 1.0));
        song.add(new NoteInfo(3.5, 0.5, 523.25, 1.0));
        AssetSound sample = (AssetSound) sounds.get("airhorn.mp3");
        SampledSongSound melody = new SampledSongSound("melody", sample, song);
        melody.init();
        sounds.put("melody", melody);
        soundsList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(sounds.values())));
        soundsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                player.playSound((Sound) parent.getItemAtPosition(position));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean areAllItemsEnabled() {
        boolean x = true;
        for (int i = 0; i < player.getSounds().size() && x; i++) {
            x = isEnabled(i);
        }
        return x;
    }

    public boolean isEnabled(int position) {
        if (position > player.getSounds().size() || position < 0) {
            throw new ArrayIndexOutOfBoundsException();
        } else {
            return true;
        }
    }
}
