package com.herrschreiber.airhornsimulator2015;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Adapter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import java.io.IOException;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends ActionBarActivity {
    SoundPlayer player;
    @InjectView(R.id.sounds)
    GridView soundsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        player = new SoundPlayer();
        try {
            player.loadSounds(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<SoundPlayer.Sound> sounds = player.getSounds();
        soundsList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sounds));
        soundsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                player.playSound((SoundPlayer.Sound) parent.getItemAtPosition(position));
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

    public boolean areAllItemsEnabled(){
        return true;
    }

    public boolean isEnabled(int position){
        if(position > player.getSounds().size() || position < 0) {
            throw new ArrayIndexOutOfBoundsException();
        } else {
            return true;
        }
    }
}
