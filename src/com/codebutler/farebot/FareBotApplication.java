/*
 * FareBotApplication.java
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

package com.codebutler.farebot;

import android.app.Application;
import android.os.StrictMode;
import com.codebutler.farebot.felica.DBUtil;

public class FareBotApplication extends Application {
    private static FareBotApplication sInstance;

    private DBUtil mSuicaDBUtil;

    public FareBotApplication() {
        sInstance = this;

        mSuicaDBUtil = new DBUtil(this);
    }

    public static FareBotApplication getInstance() {
        return sInstance;
    }

    public DBUtil getSuicaDBUtil() {
        return mSuicaDBUtil;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build());
    }
}
