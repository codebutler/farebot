package com.codebutler.farebot.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class BackgroundTagActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, ReadingTagActivity.class);
        intent.setAction(getIntent().getAction());
        intent.putExtras(getIntent().getExtras());
        startActivity(intent);

        finish();
    }
}
