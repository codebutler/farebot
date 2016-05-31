/*
 * DBUtil.java
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * Based on code from https://github.com/Kazzz/nfc-felica
 * nfc-felica by Kazzz. See project URL for complete author information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codebutler.farebot.card.felica;

import android.content.Context;

import com.codebutler.farebot.util.DBUtil;

public class FelicaDBUtil extends DBUtil {
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_AREACODE = "AreaCode";
    public static final String COLUMN_LINECODE = "LineCode";
    public static final String COLUMN_STATIONCODE = "StationCode";
    public static final String COLUMN_COMPANYNAME = "CompanyName";
    public static final String COLUMN_LINENAME = "LineName";
    public static final String COLUMN_STATIONNAME = "StationName";
    public static final String COLUMN_COMPANYNAME_EN = "CompanyName_en";
    public static final String COLUMN_LINENAME_EN = "LineName_en";
    public static final String COLUMN_STATIONNAME_EN = "StationName_en";
    public static final String COLUMN_LATITUDE = "Latitude";
    public static final String COLUMN_LONGITUDE = "Longitude";

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

    private static final String DB_NAME = "felica_stations.db3";

    private static final int VERSION = 2;

    public FelicaDBUtil(Context context) {
        super(context);
    }


    @Override
    protected String getDBName() {
        return DB_NAME;
    }

    @Override
    protected int getDesiredVersion() {
        return VERSION;
    }
}

