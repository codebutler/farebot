/*
 * AddKeyActivity.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012-2014, 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcF;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.classic.key.ClassicCardKeys;
import com.codebutler.farebot.card.classic.key.ClassicSectorKey;
import com.codebutler.farebot.core.ByteUtils;
import com.codebutler.farebot.serialize.CardKeysSerializer;
import com.codebutler.farebot.persist.CardKeysPersister;
import com.codebutler.farebot.persist.model.SavedKey;
import com.codebutler.farebot.util.BetterAsyncTask;
import com.codebutler.farebot.util.Utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class AddKeyActivity extends Activity {
    private static final int REQUEST_SELECT_FILE = 1;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private String[][] mTechLists = new String[][]{
            new String[]{IsoDep.class.getName()},
            new String[]{MifareClassic.class.getName()},
            new String[]{MifareUltralight.class.getName()},
            new String[]{NfcF.class.getName()}
    };

    private CardKeysPersister mCardKeysPersister;
    private CardKeysSerializer mCardKeysSerializer;

    private byte[] mKeyData;
    private String mTagId;
    private CardType mCardType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_key);
        getWindow().setLayout(WRAP_CONTENT, MATCH_PARENT);

        FareBotApplication app = (FareBotApplication) getApplication();
        mCardKeysPersister = app.getCardKeysPersister();
        mCardKeysSerializer = app.getCardKeysSerializer();

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String keyType = ((RadioButton) findViewById(R.id.is_key_a)).isChecked()
                        ? ClassicSectorKey.TYPE_KEYA : ClassicSectorKey.TYPE_KEYB;

                new BetterAsyncTask<Long>(AddKeyActivity.this, true, false) {
                    @Override
                    protected Long doInBackground() throws Exception {
                        ClassicCardKeys keys = ClassicCardKeys.fromDump(keyType, mKeyData);
                        SavedKey savedKey = SavedKey.create(mTagId, mCardType, mCardKeysSerializer.serialize(keys));
                        return mCardKeysPersister.insert(savedKey);
                    }

                    @Override
                    protected void onResult(Long id) {
                        Intent intent = new Intent(AddKeyActivity.this, KeysActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }
                }.execute();
            }
        });

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            finish();
            return;
        }

        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        showFileSelector();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_FILE && resultCode == RESULT_OK) {
            loadFile(data.getData());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, mTechLists);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra("android.nfc.extra.TAG");
        mTagId = ByteUtils.getHexString(tag.getId(), "");

        if (ArrayUtils.contains(tag.getTechList(), "android.nfc.tech.MifareClassic")) {
            mCardType = CardType.MifareClassic;
            ((TextView) findViewById(R.id.card_type)).setText(R.string.mifare_classic);
            ((TextView) findViewById(R.id.card_id)).setText(mTagId);
            ((TextView) findViewById(R.id.key_data)).setText(ByteUtils.getHexString(mKeyData, "").toUpperCase());

            findViewById(R.id.directions).setVisibility(View.GONE);
            findViewById(R.id.info).setVisibility(View.VISIBLE);
            findViewById(R.id.add).setVisibility(View.VISIBLE);

        } else {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.card_keys_not_supported)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void showFileSelector() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent.setAction(Intent.ACTION_GET_CONTENT);
        }
        startActivityForResult(intent, REQUEST_SELECT_FILE);
    }

    private void loadFile(Uri uri) {
        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            if (stream != null) {
                mKeyData = IOUtils.toByteArray(stream);
            } else {
                finish();
            }
        } catch (IOException e) {
            Utils.showErrorAndFinish(this, e);
        }
    }
}
