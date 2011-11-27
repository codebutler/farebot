/*
 * DBUtil.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *                                                                      
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codebutler.farebot.felica;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 駅名、停留所を取得するためのDBユーティリティクラスを提供します
 *
 * @author Kazzz
 * @date 2011/01/28
 * @since Android API Level 4
 *
 */

public class DBUtil extends SQLiteOpenHelper {
    public static final String COLUMN_ID             = "_id";
    public static final String COLUMN_AREACODE       = "AreaCode";
    public static final String COLUMN_LINECODE       = "LineCode";
    public static final String COLUMN_STATIONCODE    = "StationCode";
    public static final String COLUMN_COMPANYNAME    = "CompanyName";
    public static final String COLUMN_LINENAME       = "LineName";
    public static final String COLUMN_STATIONNAME    = "StationName";
    public static final String COLUMN_COMPANYNAME_EN = "CompanyName_en";
    public static final String COLUMN_LINENAME_EN    = "LineName_en";
    public static final String COLUMN_STATIONNAME_EN = "StationName_en";
    public static final String COLUMN_LATITUDE       = "Latitude";
    public static final String COLUMN_LONGITUDE      = "Longitude";

    public static final String TABLE_STATIONCODE = "StationCode";
    public static final String[] COLUMNS_STATIONCODE = {
        COLUMN_AREACODE,
        COLUMN_LINECODE,
        COLUMN_STATIONCODE,
        COLUMN_COMPANYNAME,
        COLUMN_LINENAME,
        COLUMN_STATIONNAME,
        COLUMN_COMPANYNAME_EN,
        COLUMN_LINENAME_EN,
        COLUMN_STATIONNAME_EN,
        COLUMN_LATITUDE,
        COLUMN_LONGITUDE
    };

    public static final String TABLE_IRUCA_STATIONCODE = "IruCaStationCode";
    public static final String[] COLUMNS_IRUCA_STATIONCODE = {
        COLUMN_LINECODE,
        COLUMN_STATIONCODE,
        COLUMN_COMPANYNAME,
        COLUMN_LINENAME,
        COLUMN_STATIONNAME,
        COLUMN_COMPANYNAME_EN,
        COLUMN_LINENAME_EN,
        COLUMN_STATIONNAME_EN
    };

    //The Android's default system path of your application database.
    private static final String DB_PATH = "/data/data/com.codebutler.farebot/databases/";

    private static final String DB_NAME = "StationCode.db";

    private SQLiteDatabase dataBase;

    private final Context context;
    /**
     * コンストラクタ
     * @param context コンテキストをセット
     */
    public DBUtil(Context context) {
        super(context, DB_NAME, null, 1);
        this.context = context;
    }
    /**
     * データベースを生成します
     * @throws java.io.IOException
     */
    public void createDataBase() throws IOException{

        boolean dbExist = this.isExsistDataBase();

        if (dbExist) {
            //DBはコピー済み
        } else {
            //Readonlyで開く
            this.getReadableDatabase();

            try {
                this.copyDataBase();
            } catch (IOException e) {
                throw new Error("Error copying database", e);

            }
        }

    }

    /**
     * データベースの有無を検査します
     * @return /data/data/パッケージ/に既にDBが存在している場合はtrueが戻ります
     */
    private boolean isExsistDataBase(){

        SQLiteDatabase checkDB = null;

        try{
            String path = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(
                    path, null, SQLiteDatabase.OPEN_READONLY);

        } catch ( SQLiteException e ){
            //database does't exist yet.
        }

        if(checkDB != null){
            checkDB.close();
        }

        return checkDB != null ? true : false;
    }

    /**
     * データベースをAssetsからシステム側へコピーします
     * @throws java.io.IOException
     */
    private void copyDataBase() throws IOException{

        //Open your local db as the input stream
        InputStream in = this.context.getAssets().open(DB_NAME);

        // Path to the just created empty db
        String outFileName = DB_PATH + DB_NAME;

        //Open the empty db as the output stream
        OutputStream out = new FileOutputStream(outFileName);

        try {
            //transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer))>0){
                out.write(buffer, 0, length);
            }
        } finally {
            out.flush();
            out.close();
            in.close();
        }
    }
    /**
     * データベースをオープンします
     * @return SQLiteDatabase データベースが戻ります
     * @throws android.database.SQLException
     * @throws java.io.IOException
     */
    public SQLiteDatabase openDataBase() throws SQLException, IOException{
        //必要ならAssetsからコピー
        this.createDataBase();

        //Open the database
        String path = DB_PATH + DB_NAME;
        this.dataBase = SQLiteDatabase.openDatabase(
                path, null, SQLiteDatabase.OPEN_READONLY);
        return this.dataBase;

    }
    /**
     * データベースをクローズします
     */
    @Override
    public synchronized void close() {
        if( this.dataBase != null )
            this.dataBase.close();
         super.close();

    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        //NOOP
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //NOOP
    }
}
