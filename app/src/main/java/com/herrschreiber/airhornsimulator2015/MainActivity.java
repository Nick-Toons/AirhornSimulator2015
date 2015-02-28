package com.herrschreiber.airhornsimulator2015;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Adapter;

import java.io.IOException;
import java.util.List;

import butterknife.ButterKnife;


public class MainActivity extends ActionBarActivity {
    SoundPlayer player;

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
        Log.e("MainActivity", sounds.toString());
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
