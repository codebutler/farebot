package au.id.micolous.farebot.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import au.id.micolous.farebot.R;
import au.id.micolous.farebot.util.Utils;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        ((TextView) this.findViewById(R.id.lblDebugText)).setText(Utils.getDeviceInfoString());

    }

    public void onWebsiteClick(View view) {
        Uri.Builder b = Uri.parse("https://micolous.github.io/farebot/").buildUpon();
        int version = -1;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException ex) {
            // Shouldn't hit this...
        }

        // Pass the version number to the website.
        // This allows the website to have a hook showing if the user's version is out of date
        // and flag specifically which cards *won't* be supported (or have problems).
        b.appendQueryParameter("ver", Integer.toString(version));
        startActivity(new Intent(Intent.ACTION_VIEW, b.build()));
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }
}
