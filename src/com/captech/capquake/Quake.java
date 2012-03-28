package com.captech.capquake;

import android.net.Uri;
import android.provider.BaseColumns;

public class Quake {
	
	public Quake(){}

	public static final class Quakes implements BaseColumns {
		private Quakes(){
		}
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ QuakeProvider.AUTHORITY + "/quakes");
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/com.captech.capquake.quake";
		
		public static final String QUAKE_ID = "_id";
		
		public static final String TITLE = "title";
		
		public static final String LATITUDE = "latitude";
		
		public static final String LONGITUDE = "longitude";
		
		public static final String LINK = "link";
		
		public static final String MAGNITUDE = "magnitude";
		
		public static final String TIME = "quake_time";
	}
	
	public static final class RssQuakes implements BaseColumns{
		private RssQuakes(){
		}
		
		public static final String TITLE = "/title";
		
		public static final String LATITUDE = "/geo:lat";
		
		public static final String LONGITUDE = "/geo:long";
		
		public static final String LINK = "/link";
		
		public static final String MAGNITUDE = "/dc:subject";
		
		public static final String TIME = "/description";
	}
}
