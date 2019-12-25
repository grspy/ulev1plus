package com.grspy.ulev1plus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class DBHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "tagsdb";
    private static final String TABLE_Tags = "tagdetails";
    private static final String KEY_ID = "id";
    private static final String KEY_UID = "uid";
    private static final String KEY_PWD = "pass";
    private static final String KEY_PACK = "pack";

    DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = String.format("CREATE TABLE %s(%s INTEGER PRIMARY KEY AUTOINCREMENT,%s TEXT,%s TEXT,%s TEXT)", TABLE_Tags, KEY_ID, KEY_UID, KEY_PWD, KEY_PACK);
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if exist
        db.execSQL(String.format("DROP TABLE IF EXISTS %s", TABLE_Tags));
        // Create tables again
        onCreate(db);
    }

    // Adding new Tag
    long insertTag(String uid, String pass, String pack) {
        //Get the Data Repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        //Create a new map of values, where column names are the keys
        ContentValues cValues = new ContentValues();
        cValues.put(KEY_UID, uid);
        cValues.put(KEY_PWD, pass);
        cValues.put(KEY_PACK, pack);
        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(TABLE_Tags, null, cValues);
        db.close();
        return newRowId;
    }

    // Adding new Tag (ULTag)
    long insertTag(ULTag tag) {
        //Get the Data Repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        //Create a new map of values, where column names are the keys
        ContentValues cValues = new ContentValues();
        cValues.put(KEY_UID, tag.getUid());
        cValues.put(KEY_PWD, tag.getPwd());
        cValues.put(KEY_PACK, tag.getPack());
        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(TABLE_Tags, null, cValues);
        db.close();
        return newRowId;
    }

    // Get Tags
    ArrayList<HashMap<String, String>> GetTags() {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> tagList = new ArrayList<>();
        String query = String.format("SELECT uid, pass, pack FROM %s", TABLE_Tags);
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> tag = new HashMap<>();
            tag.put("uid", cursor.getString(cursor.getColumnIndex(KEY_UID)));
            tag.put("pass", cursor.getString(cursor.getColumnIndex(KEY_PWD)));
            tag.put("pack", cursor.getString(cursor.getColumnIndex(KEY_PACK)));
            tagList.add(tag);
        }
        cursor.close();
        return tagList;
    }

    // Get Tags (ULTag)
    ArrayList<ULTag> GetULTags() {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<ULTag> tagList = new ArrayList<>();
        String query = String.format("SELECT uid, pass, pack FROM %s", TABLE_Tags);
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            ULTag tag = new ULTag();
            tag.setUid(cursor.getString(cursor.getColumnIndex(KEY_UID)));
            tag.setPwd(cursor.getString(cursor.getColumnIndex(KEY_PWD)));
            tag.setPack(cursor.getString(cursor.getColumnIndex(KEY_PACK)));
            tagList.add(tag);
        }
        cursor.close();
        return tagList;
    }

    // Get Tag based on UID
    ULTag GetTagByUID(String uid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ULTag ultag = null;

        Cursor cursor = db.query(TABLE_Tags, new String[]{KEY_UID, KEY_PWD, KEY_PACK}, KEY_UID + "=?",
                new String[]{uid}, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.getCount() > 0) {
                ultag = new ULTag();
                ultag.setUid(cursor.getString(cursor.getColumnIndex(KEY_UID)));
                ultag.setPwd(cursor.getString(cursor.getColumnIndex(KEY_PWD)));
                ultag.setPack(cursor.getString(cursor.getColumnIndex(KEY_PACK)));
            }
            cursor.close();
        }

        return ultag;
    }

    // Delete Tag
    void DeleteTag(String uid) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_Tags, KEY_UID + " = ?", new String[]{uid});
        db.close();
    }

    // Update Tag
    int UpdateTag(String uid, String pass, String pack) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cVals = new ContentValues();
        cVals.put(KEY_PWD, pass);
        cVals.put(KEY_PACK, pack);
        return db.update(TABLE_Tags, cVals, KEY_UID + " = ?", new String[]{uid});
    }
}
