package com.captech.capquake;

import android.app.ListActivity;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;

import com.captech.capquake.Adapters.IManagedAdapter;
import com.captech.capquake.Quake.Quakes;

public class QuakeList extends ListActivity {
	private static final String FEED_URI = "http://earthquake.usgs.gov/earthquakes/shakemap/rss.xml";
	private CursorAdapter adapter;

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (adapter != null) {
			adapter.getCursor().close();
			adapter = null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(this).inflate(R.menu.option, menu);

		return (super.onCreateOptionsMenu(menu));
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
		initAdapter();

		getListView().setOnItemClickListener(new UrlIntentListener());

		getListView().setOnCreateContextMenuListener(
				new OnCreateContextMenuListener() {

					@Override
					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {
						new MenuInflater(v.getContext()).inflate(
								R.menu.context, menu);
					}

				});
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info=
				(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		switch (item.getItemId()) {
			case R.id.delete:
				if (adapter instanceof IManagedAdapter){
					((IManagedAdapter)adapter).remove(info.position);
					initAdapter();
					return (true);
				}
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			initAdapter(true);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void initAdapter() {
		initAdapter(false);
	}

	private void initAdapter(boolean reset) {
		adapter = Adapters.loadCursorAdapter(this, R.xml.quake,
				Quakes.CONTENT_URI + "?url=" + Uri.encode(FEED_URI)
						+ "&reset=" + reset);
		setListAdapter(adapter);
	}
}