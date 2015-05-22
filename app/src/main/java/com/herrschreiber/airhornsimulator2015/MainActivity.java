package com.herrschreiber.airhornsimulator2015;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Button;
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

public class MainActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, SearchView.OnQueryTextListener {
    private static final String TAG = "MainActivity";
    private static final String KEY_SEARCH_OPENED = "searchOpened";
    private static final String KEY_SEARCH_QUERY = "searchQuery";
    @InjectView(R.id.sounds)
    protected GridView soundsList;
    private SoundPlayer soundPlayer;
    private SoundListAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        setTitle(getResources().getString(R.string.title_main_activity));

        soundPlayer = ((AirhornSimulatorApplication) getApplication()).getSoundPlayer();

        try {
            soundPlayer.loadSounds();
        } catch (IOException e) {
            Log.e(TAG, "Error loading sounds", e);
        }

        List<AssetSound> sounds = soundPlayer.listSounds();

        listAdapter = new SoundListAdapter(MainActivity.this, sounds);
        soundsList.setAdapter(listAdapter);
        soundsList.setOnItemClickListener(MainActivity.this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                break;
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
        final AssetSound sound = (AssetSound) parent.getItemAtPosition(position);
        if (sound.hasInitialized()) {
            soundPlayer.playSound(sound);
        } else {
            final SoundListAdapter.ViewHolder viewHolder = (SoundListAdapter.ViewHolder) view.getTag();
            if (!viewHolder.isInitializing) {
                final ViewSwitcher viewSwitcher = (ViewSwitcher) view.findViewById(R.id.sound_switcher);
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected void onPreExecute() {
                        viewSwitcher.showNext();
                        viewHolder.isInitializing = true;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        viewSwitcher.showPrevious();
                        soundPlayer.playSound(sound);
                        viewHolder.isInitializing = false;
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
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        listAdapter.getFilter().filter(s);
        return true;
    }

    public class SoundListAdapter extends BaseAdapter implements Filterable {
        private Context context;
        private List<AssetSound> sounds;
        private List<AssetSound> filteredSounds;
        private SoundFilter filter;

        public SoundListAdapter(Context context, List<AssetSound> sounds) {
            this.context = context;
            this.sounds = sounds;
            this.filteredSounds = this.sounds;
            filter = new SoundFilter();
        }

        @Override
        public int getCount() {
            return filteredSounds.size();
        }

        @Override
        public AssetSound getItem(int position) {
            return filteredSounds.get(position);
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
                    Intent intent = new Intent(context, SongsActivity.class);
                    intent.putExtra(SongsActivity.KEY_SOUND_NAME, item.getName());
                    context.startActivity(intent);
                }
            });
            return view;
        }

        @Override
        public Filter getFilter() {
            return filter;
        }

        public class ViewHolder {
            @InjectView(R.id.sound_icon)
            public ImageView soundIcon;
            @InjectView(R.id.sound_name)
            public TextView soundName;
            @InjectView(R.id.button_songs)
            public Button buttonSongs;
            public boolean isInitializing = false;

            public ViewHolder(View view) {
                ButterKnife.inject(this, view);
            }
        }

        private class SoundFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null && constraint.length() > 0) {
                    ArrayList<AssetSound> filtered = new ArrayList<>();

                    for (AssetSound sound : sounds) {
                        if (sound.getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
                            filtered.add(sound);
                        }
                    }

                    filterResults.count = filtered.size();
                    filterResults.values = filtered;
                } else {
                    filterResults.count = sounds.size();
                    filterResults.values = sounds;
                }

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredSounds = (List<AssetSound>) results.values;
                notifyDataSetChanged();
            }
        }
    }
}
