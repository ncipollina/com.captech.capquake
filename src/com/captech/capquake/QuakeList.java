package com.captech.capquake;

import android.app.ListActivity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;

import com.captech.capquake.Quake.Quakes;

public class QuakeList extends ListActivity {
	private static final String FEED_URI = "http://earthquake.usgs.gov/earthquakes/shakemap/rss.xml";
	private CursorAdapter adapter;
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (adapter != null)
		{
			adapter.getCursor().close();
			adapter = null;
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quake_list);
        adapter = Adapters.loadCursorAdapter(this, R.xml.quake, Quakes.CONTENT_URI + "?url="
        		+ Uri.encode(FEED_URI) + "&reset=false");
        setListAdapter(adapter);
        
        getListView().setOnItemClickListener(new UrlIntentListener());
    }
}