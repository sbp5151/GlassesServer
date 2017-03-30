package com.jld.glassesserver.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

/**
 * Created by lz on 2016/10/20.
 */

public class NumberDao {

    private DataBaseHelper baseHelper;
    private SQLiteDatabase db;
    private static NumberDao numberDao;

    private NumberDao(Context context){
        this.baseHelper = new DataBaseHelper(context);
    }

    public static NumberDao getInstance(Context context){
        if (null == numberDao){
            numberDao = new NumberDao(context);
        }
        return numberDao;
    }

    public boolean addOrUpdateNumber(String number){
        if (findIdExist()){
            return updateNumber(number);
        }else {
            return insertNumber(number);
        }
    }

    public boolean insertNumber(String number){
        boolean flag = false;
        if (TextUtils.isEmpty(number))
            return flag;
        this.db = baseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("insert into contact(number)values(?)",new Object[]{number});
            db.setTransactionSuccessful();
            db.endTransaction();
            flag = true;
        }catch (Exception e){
           e.printStackTrace();
        }
        return flag;
    }

    public boolean updateNumber(String number){
        boolean flag = false;
        if (TextUtils.isEmpty(number))
            return flag;
        db = baseHelper.getWritableDatabase();
        try {
            db.execSQL("update contact set number = ? where _id = 1 ",new Object[]{number});
            flag = true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return flag;
    }

    public String queryNumber(){
        String number = "";
        Cursor cursor = null;
        db = baseHelper.getReadableDatabase();
        try {
            cursor = db.rawQuery("select * from contact where _id = ?",new String[]{"1"});
            while (cursor.moveToNext()){
                number = cursor.getString(cursor.getColumnIndex("number"));
            }
        }catch (Exception e){
        }finally {
            if (null != cursor){
                cursor.close();
            }
        }
        return number;
    }

    public boolean findIdExist(){
        boolean isExist = false;
        Cursor cursor = null;
        db = baseHelper.getReadableDatabase();
        try {
            cursor = db.rawQuery("select * from contact where _id = ?",new String[]{"1"});
            while (cursor.moveToNext()){
                isExist = true;
            }
        }catch (Exception e){
        }finally {
            if (null != cursor){
                cursor.close();
            }
        }
        return isExist;
    }
}
