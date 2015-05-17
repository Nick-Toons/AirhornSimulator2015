package com.herrschreiber.airhornsimulator2015;

import android.content.Context;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";
    @InjectView(R.id.sounds)
    protected GridView soundsList;
    private SoundPlayer soundPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        soundPlayer = ((AirhornSimulatorApplication) getApplication()).getSoundPlayer();

        try {
            soundPlayer.loadSounds();
        } catch (IOException e) {
            Log.e(TAG, "Error loading sounds", e);
        }

        List<AssetSound> sounds = soundPlayer.listSounds();
        soundsList.setAdapter(new SoundListAdapter(this, sounds));
        soundsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                soundPlayer.playSound((Sound) parent.getItemAtPosition(position));
            }
        });
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

    public boolean areAllItemsEnabled() {
        boolean x = true;
        for (int i = 0; i < soundPlayer.getSounds().size() && x; i++) {
            x = isEnabled(i);
        }
        return x;
    }

    public boolean isEnabled(int position) {
        if (position > soundPlayer.getSounds().size() || position < 0) {
            throw new ArrayIndexOutOfBoundsException();
        } else {
            return true;
        }
    }

    public static class SoundListAdapter extends ArrayAdapter<AssetSound> {
        public SoundListAdapter(Context context, List<AssetSound> sounds) {
            super(context, R.layout.item_sound, sounds);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder viewHolder;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_sound, parent, false);
                viewHolder = new ViewHolder(view);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            final AssetSound item = getItem(position);
            viewHolder.soundIcon.setImageDrawable(item.getIcon());
            viewHolder.soundName.setText(item.getName());
            viewHolder.buttonSongs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), SongsActivity.class);
                    intent.putExtra(SongsActivity.KEY_SOUND_NAME, item.getName());
                    getContext().startActivity(intent);
                }
            });
            return view;
        }

        public class ViewHolder {
            @InjectView(R.id.sound_icon)
            public ImageView soundIcon;
            @InjectView(R.id.sound_name)
            public TextView soundName;
            @InjectView(R.id.button_songs)
            public Button buttonSongs;

            public ViewHolder(View view) {
                ButterKnife.inject(this, view);
            }
        }
    }
}
