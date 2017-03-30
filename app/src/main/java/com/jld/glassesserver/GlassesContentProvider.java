package com.jld.glassesserver;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.jld.glassesserver.db.DataBaseHelper;

/**
 * Created by lz on 2016/10/20.
 */

public class GlassesContentProvider extends ContentProvider {

    private static final int CONTACT = 1;
    private static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI("com.jld.glassesserver","contact",CONTACT);
    }

    private DataBaseHelper dbHelp;
    private ContentResolver contentResolver;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        contentResolver = context.getContentResolver();
        dbHelp = new DataBaseHelper(context);
        return true;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not yet implemented");
//        Uri u = null;
//        if (uriMatcher.match(uri) == CONTACT){
//            SQLiteDatabase database = dbHelp.getWritableDatabase();
//            long d = database.insert("contact","_id",values);
//            u = ContentUris.withAppendedId(uri,d);
//            contentResolver.notifyChange(u,null);
//        }
//        return u;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
//        int id = 0;
//        if (uriMatcher.match(uri) == CONTACT){
//            SQLiteDatabase database = dbHelp.getWritableDatabase();
//            id = database.delete("contact",selection,selectionArgs);
//            contentResolver.notifyChange(uri,null);
//        }
//        return id;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        if (uriMatcher.match(uri) == CONTACT){
            SQLiteDatabase database = dbHelp.getReadableDatabase();
            cursor = database.query("contact",projection,selection,selectionArgs,null,null,sortOrder);
            cursor.setNotificationUri(contentResolver,uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
