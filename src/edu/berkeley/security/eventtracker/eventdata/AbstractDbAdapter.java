package edu.berkeley.security.eventtracker.eventdata;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import edu.berkeley.security.eventtracker.eventdata.EventDbAdapter.EventKey;

public abstract class AbstractDbAdapter {

	protected static final String TAG = "DbAdapter";
	protected DatabaseHelper mDbHelper;
	protected SQLiteDatabase mDb;

	protected static final String TABLE_CREATE_EVENTS;

	static { // initialize dynamic String that creates the events table.
		StringBuilder createEventsBuilder = new StringBuilder();
		createEventsBuilder.append("create table eventData (");
		boolean isFirst = true;
		for (EventKey dbKey : EventKey.values()) {
			if (!isFirst)
				createEventsBuilder.append(",");
			isFirst = false;
			createEventsBuilder.append(dbKey.getRowCreateString());
		}
		createEventsBuilder.append(");");
		TABLE_CREATE_EVENTS = createEventsBuilder.toString();
	}

	protected static final String TABLE_CREATE_GPSDATA = "create table gpsData (_id integer primary key autoincrement, "
			+ "eventRowID long,"
			+ "latitude real,"
			+ "longitude real,"
			+ "timeOfRecording Long);";

	protected static final String DATABASE_NAME = "data";
	protected static final int DATABASE_VERSION = 4;

	protected final Context mCtx;

	protected static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(TABLE_CREATE_EVENTS);
			db.execSQL(TABLE_CREATE_GPSDATA);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
			onCreate(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public AbstractDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open or create the routes database.
	 * 
	 * @return this
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public AbstractDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

}