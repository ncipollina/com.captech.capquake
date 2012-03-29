package com.captech.capquake;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.captech.capquake.Quake.Quakes;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class Adapters {
	private static final String ADAPTER_CURSOR = "cursor-adapter";

	public static abstract class CursorBinder {
		protected final Context mContext;

		protected final CursorTransformation mTransformation;

		public CursorBinder(Context context, CursorTransformation transformation) {
			mContext = context;
			mTransformation = transformation;
		}

		public abstract boolean bind(View view, Cursor cursor, int columnIndex);
	}

	public static abstract class CursorTransformation {
		protected final Context mContext;

		public CursorTransformation(Context context) {
			mContext = context;
		}

		public abstract String transform(Cursor cursor, int columnIndex);

		public int transformToResource(Cursor cursor, int columnIndex) {
			return cursor.getInt(columnIndex);
		}
	}

	public static CursorAdapter loadCursorAdapter(Context context, int id,
			String uri, Object... parameters) {
		XmlCursorAdapter adapter = (XmlCursorAdapter) loadAdapter(context, id,
				ADAPTER_CURSOR, parameters);

		if (uri != null) {
			adapter.setUri(uri);
		}
		adapter.load();

		return adapter;
	}

	private static BaseAdapter loadAdapter(Context context, int id,
			String assertName, Object... parameters) {

		XmlResourceParser parser = null;
		try {
			parser = context.getResources().getXml(id);
			return createAdapterFromXml(context, parser,
					Xml.asAttributeSet(parser), id, parameters, assertName);
		} catch (XmlPullParserException ex) {
			Resources.NotFoundException rnf = new Resources.NotFoundException(
					"Can't load adapter resource ID "
							+ context.getResources().getResourceEntryName(id));
			rnf.initCause(ex);
			throw rnf;
		} catch (IOException ex) {
			Resources.NotFoundException rnf = new Resources.NotFoundException(
					"Can't load adapter resource ID "
							+ context.getResources().getResourceEntryName(id));
			rnf.initCause(ex);
			throw rnf;
		} finally {
			if (parser != null)
				parser.close();
		}
	}

	private static BaseAdapter createAdapterFromXml(Context c,
			XmlPullParser parser, AttributeSet attrs, int id,
			Object[] parameters, String assertName)
			throws XmlPullParserException, IOException {

		BaseAdapter adapter = null;

		// Make sure we are on a start tag.
		int type;
		int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser
				.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			String name = parser.getName();
			if (assertName != null && !assertName.equals(name)) {
				throw new IllegalArgumentException("The adapter defined in "
						+ c.getResources().getResourceEntryName(id)
						+ " must be a <" + assertName + " />");
			}

			if (ADAPTER_CURSOR.equals(name)) {
				adapter = createCursorAdapter(c, parser, attrs, id, parameters);
			} else {
				throw new IllegalArgumentException("Unknown adapter name "
						+ parser.getName() + " in "
						+ c.getResources().getResourceEntryName(id));
			}
		}

		return adapter;

	}

	private static XmlCursorAdapter createCursorAdapter(Context c,
			XmlPullParser parser, AttributeSet attrs, int id,
			Object[] parameters) throws IOException, XmlPullParserException {

		return new XmlCursorAdapterParser(c, parser, attrs, id)
				.parse(parameters);
	}

	private static class XmlCursorAdapterParser {
		private static final String ADAPTER_CURSOR_BIND = "bind";
		private static final String ADAPTER_CURSOR_SELECT = "select";
		private static final String ADAPTER_CURSOR_AS_STRING = "string";
		private static final String ADAPTER_CURSOR_AS_IMAGE = "image";
		private static final String ADAPTER_CURSOR_AS_TAG = "tag";
		private static final String ADAPTER_CURSOR_AS_IMAGE_URI = "image-uri";
		private static final String ADAPTER_CURSOR_AS_DRAWABLE = "drawable";
		private static final String ADAPTER_CURSOR_AS_COLOR = "color";
		private static final String ADAPTER_CURSOR_MAP = "map";
		private static final String ADAPTER_CURSOR_TRANSFORM = "transform";
		private static final String ADAPTER_CURSOR_COLOR = "colorSet";

		private final Context mContext;
		private final XmlPullParser mParser;
		private final AttributeSet mAttrs;
		private final int mId;

		private final HashMap<String, CursorBinder> mBinders;
		private final ArrayList<String> mFrom;
		private final ArrayList<Integer> mTo;
		private final CursorTransformation mIdentity;
		private final Resources mResources;

		public XmlCursorAdapterParser(Context c, XmlPullParser parser,
				AttributeSet attrs, int id) {
			mContext = c;
			mParser = parser;
			mAttrs = attrs;
			mId = id;

			mResources = mContext.getResources();
			mBinders = new HashMap<String, CursorBinder>();
			mFrom = new ArrayList<String>();
			mTo = new ArrayList<Integer>();
			mIdentity = new IdentityTransformation(mContext);
		}

		public XmlCursorAdapter parse(Object[] parameters) throws IOException,
				XmlPullParserException {

			Resources resources = mResources;
			TypedArray a = resources.obtainAttributes(mAttrs,
					R.styleable.CursorAdapter);

			String uri = a.getString(R.styleable.CursorAdapter_uri);
			String selection = a.getString(R.styleable.CursorAdapter_selection);
			String sortOrder = a.getString(R.styleable.CursorAdapter_sortOrder);
			int layout = a.getResourceId(R.styleable.CursorAdapter_layout, 0);
			if (layout == 0) {
				throw new IllegalArgumentException("The layout specified in "
						+ resources.getResourceEntryName(mId)
						+ " does not exist");
			}

			a.recycle();

			XmlPullParser parser = mParser;
			int type;
			int depth = parser.getDepth();

			while (((type = parser.next()) != XmlPullParser.END_TAG || parser
					.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

				if (type != XmlPullParser.START_TAG) {
					continue;
				}

				String name = parser.getName();

				if (ADAPTER_CURSOR_BIND.equals(name)) {
					parseBindTag();
				} else if (ADAPTER_CURSOR_SELECT.equals(name)) {
					parseSelectTag();
				} else {
					throw new RuntimeException("Unknown tag name "
							+ parser.getName() + " in "
							+ resources.getResourceEntryName(mId));
				}
			}

			String[] fromArray = mFrom.toArray(new String[mFrom.size()]);
			int[] toArray = new int[mTo.size()];
			for (int i = 0; i < toArray.length; i++) {
				toArray[i] = mTo.get(i);
			}

			String[] selectionArgs = null;
			if (parameters != null) {
				selectionArgs = new String[parameters.length];
				for (int i = 0; i < selectionArgs.length; i++) {
					selectionArgs[i] = (String) parameters[i];
				}
			}

			return new XmlCursorAdapter(mContext, layout, uri, fromArray,
					toArray, selection, selectionArgs, sortOrder, mBinders);
		}

		private void parseSelectTag() {
			TypedArray a = mResources.obtainAttributes(mAttrs,
					R.styleable.CursorAdapter_SelectItem);

			String fromName = a
					.getString(R.styleable.CursorAdapter_SelectItem_column);
			if (fromName == null) {
				throw new IllegalArgumentException("A select item in "
						+ mResources.getResourceEntryName(mId)
						+ " does not have a 'column' attribute");
			}

			a.recycle();

			mFrom.add(fromName);
			mTo.add(View.NO_ID);
		}

		private void parseBindTag() throws IOException, XmlPullParserException {
			Resources resources = mResources;
			TypedArray a = resources.obtainAttributes(mAttrs,
					R.styleable.CursorAdapter_BindItem);

			String fromName = a
					.getString(R.styleable.CursorAdapter_BindItem_from);
			if (fromName == null) {
				throw new IllegalArgumentException("A bind item in "
						+ resources.getResourceEntryName(mId)
						+ " does not have a 'from' attribute");
			}

			int toName = a.getResourceId(R.styleable.CursorAdapter_BindItem_to,
					0);
			if (toName == 0) {
				throw new IllegalArgumentException("A bind item in "
						+ resources.getResourceEntryName(mId)
						+ " does not have a 'to' attribute");
			}

			String asType = a.getString(R.styleable.CursorAdapter_BindItem_as);
			if (asType == null) {
				throw new IllegalArgumentException("A bind item in "
						+ resources.getResourceEntryName(mId)
						+ " does not have an 'as' attribute");
			}

			mFrom.add(fromName);
			mTo.add(toName);
			mBinders.put(fromName, findBinder(asType));

			a.recycle();
		}

		private CursorBinder findBinder(String type) throws IOException,
				XmlPullParserException {
			final XmlPullParser parser = mParser;
			final Context context = mContext;
			CursorTransformation transformation = mIdentity;

			int tagType;
			int depth = parser.getDepth();

			final boolean isDrawable = ADAPTER_CURSOR_AS_DRAWABLE.equals(type);

			while (((tagType = parser.next()) != XmlPullParser.END_TAG || parser
					.getDepth() > depth)
					&& tagType != XmlPullParser.END_DOCUMENT) {

				if (tagType != XmlPullParser.START_TAG) {
					continue;
				}

				String name = parser.getName();

				if (ADAPTER_CURSOR_TRANSFORM.equals(name)) {
					transformation = findTransformation();
				} else if (ADAPTER_CURSOR_MAP.equals(name)) {
					if (!(transformation instanceof MapTransformation)) {
						transformation = new MapTransformation(context);
					}
					findMap(((MapTransformation) transformation), isDrawable);
				} else if (ADAPTER_CURSOR_COLOR.equals(name)) {
					if (!(transformation instanceof ColorTransformation)) {
						transformation = new ColorTransformation(context);
					}
					findColor(((ColorTransformation) transformation));
				} else {
					throw new RuntimeException("Unknown tag name "
							+ parser.getName() + " in "
							+ context.getResources().getResourceEntryName(mId));
				}
			}

			if (ADAPTER_CURSOR_AS_STRING.equals(type)) {
				return new StringBinder(context, transformation);
			} else if (ADAPTER_CURSOR_AS_TAG.equals(type)) {
				return new TagBinder(context, transformation);
			} else if (ADAPTER_CURSOR_AS_IMAGE.equals(type)) {
				return new ImageBinder(context, transformation);
			} else if (ADAPTER_CURSOR_AS_IMAGE_URI.equals(type)) {
				return new ImageUriBinder(context, transformation);
			} else if (ADAPTER_CURSOR_AS_COLOR.equals(type)) {
				return new ColorBinder(context, transformation);
			} else if (isDrawable) {
				return new DrawableBinder(context, transformation);
			} else {
				return createBinder(type, transformation);
			}
		}

		private CursorBinder createBinder(String type,
				CursorTransformation transformation) {
			if (mContext.isRestricted())
				return null;

			try {
				final Class<?> klass = Class.forName(type, true,
						mContext.getClassLoader());
				if (CursorBinder.class.isAssignableFrom(klass)) {
					final Constructor<?> c = klass.getDeclaredConstructor(
							Context.class, CursorTransformation.class);
					return (CursorBinder) c.newInstance(mContext,
							transformation);
				}
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(
						"Cannot instanciate binder type in "
								+ mContext.getResources().getResourceEntryName(
										mId) + ": " + type, e);
			} catch (NoSuchMethodException e) {
				throw new IllegalArgumentException(
						"Cannot instanciate binder type in "
								+ mContext.getResources().getResourceEntryName(
										mId) + ": " + type, e);
			} catch (InvocationTargetException e) {
				throw new IllegalArgumentException(
						"Cannot instanciate binder type in "
								+ mContext.getResources().getResourceEntryName(
										mId) + ": " + type, e);
			} catch (InstantiationException e) {
				throw new IllegalArgumentException(
						"Cannot instanciate binder type in "
								+ mContext.getResources().getResourceEntryName(
										mId) + ": " + type, e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(
						"Cannot instanciate binder type in "
								+ mContext.getResources().getResourceEntryName(
										mId) + ": " + type, e);
			}

			return null;
		}

		private void findMap(MapTransformation transformation, boolean drawable) {
			Resources resources = mResources;

			TypedArray a = resources.obtainAttributes(mAttrs,
					R.styleable.CursorAdapter_MapItem);

			String from = a
					.getString(R.styleable.CursorAdapter_MapItem_fromValue);
			if (from == null) {
				throw new IllegalArgumentException("A map item in "
						+ resources.getResourceEntryName(mId)
						+ " does not have a 'fromValue' attribute");
			}

			if (!drawable) {
				String to = a
						.getString(R.styleable.CursorAdapter_MapItem_toValue);
				if (to == null) {
					throw new IllegalArgumentException("A map item in "
							+ resources.getResourceEntryName(mId)
							+ " does not have a 'toValue' attribute");
				}
				transformation.addStringMapping(from, to);
			} else {
				int to = a.getResourceId(
						R.styleable.CursorAdapter_MapItem_toValue, 0);
				if (to == 0) {
					throw new IllegalArgumentException("A map item in "
							+ resources.getResourceEntryName(mId)
							+ " does not have a 'toValue' attribute");
				}
				transformation.addResourceMapping(from, to);
			}

			a.recycle();
		}

		private void findColor(ColorTransformation transformation) {
			Resources resources = mResources;

			TypedArray a = resources.obtainAttributes(mAttrs,
					R.styleable.CursorAdapter_ColorItem);

			String threshold = a
					.getString(R.styleable.CursorAdapter_ColorItem_greaterThanOrEqualTo);

			if (threshold == null) {
				throw new IllegalArgumentException("A color item in "
						+ resources.getResourceEntryName(mId)
						+ " does not have a 'greaterThanOrEqualTo' attribute");
			}

			String color = a
					.getString(R.styleable.CursorAdapter_ColorItem_color);
			if (color == null) {
				throw new IllegalArgumentException("A map item in "
						+ resources.getResourceEntryName(mId)
						+ " does not have a 'color' attribute");
			}
			transformation.addColorMapping(threshold, color);

			a.recycle();
		}

		private CursorTransformation findTransformation() {
			Resources resources = mResources;
			CursorTransformation transformation = null;
			TypedArray a = resources.obtainAttributes(mAttrs,
					R.styleable.CursorAdapter_TransformItem);

			String className = a
					.getString(R.styleable.CursorAdapter_TransformItem_withClass);
			if (className == null) {
				String expression = a
						.getString(R.styleable.CursorAdapter_TransformItem_withExpression);
				transformation = createExpressionTransformation(expression);
			} else if (!mContext.isRestricted()) {
				try {
					final Class<?> klas = Class.forName(className, true,
							mContext.getClassLoader());
					if (CursorTransformation.class.isAssignableFrom(klas)) {
						final Constructor<?> c = klas
								.getDeclaredConstructor(Context.class);
						transformation = (CursorTransformation) c
								.newInstance(mContext);
					}
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException(
							"Cannot instanciate transform type in "
									+ mContext.getResources()
											.getResourceEntryName(mId) + ": "
									+ className, e);
				} catch (NoSuchMethodException e) {
					throw new IllegalArgumentException(
							"Cannot instanciate transform type in "
									+ mContext.getResources()
											.getResourceEntryName(mId) + ": "
									+ className, e);
				} catch (InvocationTargetException e) {
					throw new IllegalArgumentException(
							"Cannot instanciate transform type in "
									+ mContext.getResources()
											.getResourceEntryName(mId) + ": "
									+ className, e);
				} catch (InstantiationException e) {
					throw new IllegalArgumentException(
							"Cannot instanciate transform type in "
									+ mContext.getResources()
											.getResourceEntryName(mId) + ": "
									+ className, e);
				} catch (IllegalAccessException e) {
					throw new IllegalArgumentException(
							"Cannot instanciate transform type in "
									+ mContext.getResources()
											.getResourceEntryName(mId) + ": "
									+ className, e);
				}
			}

			a.recycle();

			if (transformation == null) {
				throw new IllegalArgumentException("A transform item in "
						+ resources.getResourceEntryName(mId)
						+ " must have a 'withClass' or "
						+ "'withExpression' attribute");
			}

			return transformation;
		}

		private CursorTransformation createExpressionTransformation(
				String expression) {
			return new ExpressionTransformation(mContext, expression);
		}
	}

	public static interface IManagedAdapter {
		void load();
		void remove(int rowPosition);
	}

	private static class XmlCursorAdapter extends SimpleCursorAdapter implements
			IManagedAdapter {
		private Context mContext;
		private String mUri;
		private final String mSelection;
		private final String[] mSelectionArgs;
		private final String mSortOrder;
		private final int[] mTo;
		private final String[] mFrom;
		private final String[] mColumns;
		private final CursorBinder[] mBinders;
		private AsyncTask<Void, Void, Cursor> mLoadTask;

		XmlCursorAdapter(Context context, int layout, String uri,
				String[] from, int[] to, String selection,
				String[] selectionArgs, String sortOrder,
				HashMap<String, CursorBinder> binders) {

			super(context, layout, null, from, to);
			mContext = context;
			mUri = uri;
			mFrom = from;
			mTo = to;
			mSelection = selection;
			mSelectionArgs = selectionArgs;
			mSortOrder = sortOrder;
			mColumns = new String[from.length + 1];
			// This is mandatory in CursorAdapter
			mColumns[0] = "_id";
			System.arraycopy(from, 0, mColumns, 1, from.length);

			CursorBinder basic = new StringBinder(context,
					new IdentityTransformation(context));
			final int count = from.length;
			mBinders = new CursorBinder[count];

			for (int i = 0; i < count; i++) {
				CursorBinder binder = binders.get(from[i]);
				if (binder == null)
					binder = basic;
				mBinders[i] = binder;
			}
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final int count = mTo.length;
			final int[] to = mTo;
			final CursorBinder[] binders = mBinders;

			for (int i = 0; i < count; i++) {
				final View v = view.findViewById(to[i]);
				if (v != null) {
					// Not optimal, the column index could be cached
					binders[i].bind(v, cursor, cursor.getColumnIndex(mFrom[i]));
				}
			}
		}

		public void load() {
			if (mUri != null) {
				mLoadTask = new QueryTask().execute();
			}
		}

		void setUri(String uri) {
			mUri = uri;
		}

		@Override
		public void changeCursor(Cursor c) {
			if (mLoadTask != null
					&& mLoadTask.getStatus() != QueryTask.Status.FINISHED) {
				mLoadTask.cancel(true);
				mLoadTask = null;
			}
			super.changeCursor(c);
		}

		class QueryTask extends AsyncTask<Void, Void, Cursor> {
			@Override
			protected Cursor doInBackground(Void... params) {
				if (mContext instanceof Activity) {
					return ((Activity) mContext).managedQuery(Uri.parse(mUri),
							mColumns, mSelection, mSelectionArgs, mSortOrder);
				} else {
					return mContext.getContentResolver().query(Uri.parse(mUri),
							mColumns, mSelection, mSelectionArgs, mSortOrder);
				}
			}

			@Override
			protected void onPostExecute(Cursor cursor) {
				if (!isCancelled()) {
					XmlCursorAdapter.super.changeCursor(cursor);
				}
			}
		}

		@Override
		public void remove(int rowPosition) {
			Cursor c = getCursor();
			c.moveToPosition(rowPosition);
			long id = c.getLong(c.getColumnIndex(Quakes.QUAKE_ID));
			mContext.getContentResolver().delete(Uri.parse(mUri), Quakes.QUAKE_ID + "=?", new String[] {String.valueOf(id)});
		}
	}

	private static class IdentityTransformation extends CursorTransformation {
		public IdentityTransformation(Context context) {
			super(context);
		}

		@Override
		public String transform(Cursor cursor, int columnIndex) {
			return cursor.getString(columnIndex);
		}
	}

	private static class ExpressionTransformation extends CursorTransformation {
		private final ExpressionNode mFirstNode = new ConstantExpressionNode("");
		private final StringBuilder mBuilder = new StringBuilder();

		public ExpressionTransformation(Context context, String expression) {
			super(context);

			parse(expression);
		}

		private void parse(String expression) {
			ExpressionNode node = mFirstNode;
			int segmentStart;
			int count = expression.length();

			for (int i = 0; i < count; i++) {
				char c = expression.charAt(i);
				// Start a column name segment
				segmentStart = i;
				if (c == '{') {
					while (i < count && (c = expression.charAt(i)) != '}') {
						i++;
					}
					// We've reached the end, but the expression didn't close
					if (c != '}') {
						throw new IllegalStateException(
								"The transform expression contains a "
										+ "non-closed column name: "
										+ expression.substring(
												segmentStart + 1, i));
					}
					node.next = new ColumnExpressionNode(expression.substring(
							segmentStart + 1, i));
				} else {
					while (i < count && (c = expression.charAt(i)) != '{') {
						i++;
					}
					node.next = new ConstantExpressionNode(
							expression.substring(segmentStart, i));
					// Rewind if we've reached a column expression
					if (c == '{')
						i--;
				}
				node = node.next;
			}
		}

		@Override
		public String transform(Cursor cursor, int columnIndex) {
			final StringBuilder builder = mBuilder;
			builder.delete(0, builder.length());

			ExpressionNode node = mFirstNode;
			// Skip the first node
			while ((node = node.next) != null) {
				builder.append(node.asString(cursor));
			}

			return builder.toString();
		}

		static abstract class ExpressionNode {
			public ExpressionNode next;

			public abstract String asString(Cursor cursor);
		}

		static class ConstantExpressionNode extends ExpressionNode {
			private final String mConstant;

			ConstantExpressionNode(String constant) {
				mConstant = constant;
			}

			@Override
			public String asString(Cursor cursor) {
				return mConstant;
			}
		}

		static class ColumnExpressionNode extends ExpressionNode {
			private final String mColumnName;
			private Cursor mSignature;
			private int mColumnIndex = -1;

			ColumnExpressionNode(String columnName) {
				mColumnName = columnName;
			}

			@Override
			public String asString(Cursor cursor) {
				if (cursor != mSignature || mColumnIndex == -1) {
					mColumnIndex = cursor.getColumnIndex(mColumnName);
					mSignature = cursor;
				}

				return cursor.getString(mColumnIndex);
			}
		}
	}

	private static class MapTransformation extends CursorTransformation {
		private final HashMap<String, String> mStringMappings;
		private final HashMap<String, Integer> mResourceMappings;

		public MapTransformation(Context context) {
			super(context);
			mStringMappings = new HashMap<String, String>();
			mResourceMappings = new HashMap<String, Integer>();
		}

		void addStringMapping(String from, String to) {
			mStringMappings.put(from, to);
		}

		void addResourceMapping(String from, int to) {
			mResourceMappings.put(from, to);
		}

		@Override
		public String transform(Cursor cursor, int columnIndex) {
			final String value = cursor.getString(columnIndex);
			final String transformed = mStringMappings.get(value);
			return transformed == null ? value : transformed;
		}

		@Override
		public int transformToResource(Cursor cursor, int columnIndex) {
			final String value = cursor.getString(columnIndex);
			final Integer transformed = mResourceMappings.get(value);
			try {
				return transformed == null ? Integer.parseInt(value)
						: transformed;
			} catch (NumberFormatException e) {
				return 0;
			}
		}
	}

	private static class ColorTransformation extends CursorTransformation {
		private final TreeMap<String, String> mColorMappings;

		public ColorTransformation(Context context) {
			super(context);
			mColorMappings = new TreeMap<String, String>();
		}

		void addColorMapping(String threshold, String color) {
			mColorMappings.put(threshold, color);
		}

		@Override
		public String transform(Cursor cursor, int columnIndex) {
			final String value = cursor.getString(columnIndex);
			String transformed = mColorMappings.get(value);
			if (transformed == null) {
				mColorMappings.put(value, value);
				Entry<String, String> entry = mColorMappings.lowerEntry(value);
				mColorMappings.remove(value); // We have to remove the value
												// because we just put it in
												// temporarily to find
				// the value that we were greater than
				if (entry != null) {
					transformed = entry.getValue();
					return transformed;
				}
			} else
				return transformed;
			return "#000000";
		}
	}

	private static class StringBinder extends CursorBinder {
		public StringBinder(Context context, CursorTransformation transformation) {
			super(context, transformation);
		}

		@Override
		public boolean bind(View view, Cursor cursor, int columnIndex) {
			if (view instanceof TextView) {
				final String text = mTransformation.transform(cursor,
						columnIndex);
				((TextView) view).setText(text);
				return true;
			}
			return false;
		}
	}

	private static class ImageBinder extends CursorBinder {
		public ImageBinder(Context context, CursorTransformation transformation) {
			super(context, transformation);
		}

		@Override
		public boolean bind(View view, Cursor cursor, int columnIndex) {
			if (view instanceof ImageView) {
				final byte[] data = cursor.getBlob(columnIndex);
				((ImageView) view).setImageBitmap(BitmapFactory
						.decodeByteArray(data, 0, data.length));
				return true;
			}
			return false;
		}
	}

	private static class TagBinder extends CursorBinder {
		public TagBinder(Context context, CursorTransformation transformation) {
			super(context, transformation);
		}

		@Override
		public boolean bind(View view, Cursor cursor, int columnIndex) {
			final String text = mTransformation.transform(cursor, columnIndex);
			view.setTag(text);
			return true;
		}
	}

	private static class ImageUriBinder extends CursorBinder {
		public ImageUriBinder(Context context,
				CursorTransformation transformation) {
			super(context, transformation);
		}

		@Override
		public boolean bind(View view, Cursor cursor, int columnIndex) {
			if (view instanceof ImageView) {
				((ImageView) view).setImageURI(Uri.parse(mTransformation
						.transform(cursor, columnIndex)));
				return true;
			}
			return false;
		}
	}

	private static class DrawableBinder extends CursorBinder {
		public DrawableBinder(Context context,
				CursorTransformation transformation) {
			super(context, transformation);
		}

		@Override
		public boolean bind(View view, Cursor cursor, int columnIndex) {
			if (view instanceof ImageView) {
				final int resource = mTransformation.transformToResource(
						cursor, columnIndex);
				if (resource == 0)
					return false;

				((ImageView) view).setImageResource(resource);
				return true;
			}
			return false;
		}
	}

	private static class ColorBinder extends CursorBinder {
		public ColorBinder(Context context, CursorTransformation transformation) {
			super(context, transformation);
		}

		@Override
		public boolean bind(View view, Cursor cursor, int columnIndex) {
			final String color = mTransformation.transform(cursor, columnIndex);
			if (color != null) {
				try {
					view.setBackgroundColor(Color.parseColor(color));
				} catch (IllegalArgumentException e) {
					Log.w("Adapters","Could not parse color: " + color,e);
				}
			}

			return true;
		}
	}

}
