package com.codebutler.farebot.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesUtil;

import com.codebutler.farebot.R;

public class LicenseActivity extends Activity {

    static String mLicenseIntro = "Farebot M\n" +
            "Copyright 2011-2013 Eric Butler <eric@codebutler.com> and contributors\n" +
            "Copyright 2015 Michael Farrell <micolous@gmail.com>\n" +
            "\n" +
            "This program is free software: you can redistribute it and/or modify " +
            "it under the terms of the GNU General Public License as published by " +
            "the Free Software Foundation, either version 3 of the License, or " +
            "(at your option) any later version.\n" +
            "\n" +
            "This program is distributed in the hope that it will be useful, " +
            "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
            "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the " +
            "GNU General Public License for more details.\n" +
            "\n" +
            "You should have received a copy of the GNU General Public License " +
            "along with this program.  If not, see <http://www.gnu.org/licenses/>.\n\n\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        TextView lblLicenseText = (TextView)findViewById(R.id.lblLicenseText);
        lblLicenseText.setText(mLicenseIntro);
        lblLicenseText.append(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this.getApplicationContext()));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
