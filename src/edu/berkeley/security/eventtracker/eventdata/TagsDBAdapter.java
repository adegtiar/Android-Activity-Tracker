/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package edu.berkeley.security.eventtracker.eventdata;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

public class TagsDBAdapter extends AbstractDbAdapter {
	public static final String KEY_TAG = "tag";
	public static final String KEY_ROWID = "_id";
	private static final String DATABASE_TABLE = "tagData";

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public TagsDBAdapter(Context ctx) {
		super(ctx);
	}

	public long createTagEntry(String tag) {

		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TAG, tag);
		
		long toBeReturned = mDb.insert(DATABASE_TABLE, null, initialValues);
		return toBeReturned;

	}

	/**
	 * Delete the tag with the given rowId
	 * 
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteEntry(Long rowId) {

		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Return a Cursor positioned at the tag that matches the given rowId
	 * 
	 * @param rowId
	 *            id of tag to retrieve
	 * @return Cursor positioned to matching tag, if found
	 * @throws SQLException
	 *             if note could not be found/retrieved
	 */
	public Cursor fetchTag(long rowId) throws SQLException {

		Cursor mCursor =

		mDb.query(true, DATABASE_TABLE, new String[] { KEY_ROWID,
				KEY_TAG },
				KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	/**
	 * @return a cursor for all of the tags in the database
	 */
	public Cursor getTags() {
		return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_TAG}, null, null,
				null, null, null);


	}

}