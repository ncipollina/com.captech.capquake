package com.captech.capquake;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;
import android.database.Cursor;

import com.captech.capquake.Adapters.CursorTransformation;

public class DateTransformation extends CursorTransformation {
	DateFormat format = DateFormat.getDateTimeInstance();
	public DateTransformation(Context context) {
		super(context);
	}

	@Override
	public String transform(Cursor cursor, int columnIndex) {
		long lDate = cursor.getLong(columnIndex);
		return format.format(lDate);
	}

}
