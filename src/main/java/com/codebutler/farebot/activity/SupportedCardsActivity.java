/*
 * SupportedCardsActivity.java
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

package com.codebutler.farebot.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.transit.manly_fast_ferry.ManlyFastFerryTransitData;
import com.codebutler.farebot.transit.opal.OpalTransitData;
import com.codebutler.farebot.util.Utils;

import java.util.ArrayList;
import java.util.Comparator;

public class SupportedCardsActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supported_cards);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        ((Gallery) findViewById(R.id.gallery)).setAdapter(new CardsAdapter(this));
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private class CardsAdapter extends ArrayAdapter<CardInfo> {
        public CardsAdapter(Context context) {
            super(context, 0, new ArrayList<CardInfo>());

            add(new CardInfo(R.drawable.bilheteunicosp_card, "Bilhete Único",
                    R.string.location_sao_paulo,
                    CardType.MifareClassic,
                    true
            ));

            add(new CardInfo(R.drawable.clipper_card, "Clipper",
                    R.string.location_san_francisco,
                    CardType.MifareDesfire
            ));

            add(new CardInfo(R.drawable.edy_card, "Edy",
                    R.string.location_tokyo,
                    CardType.FeliCa
            ));

            add(new CardInfo(R.drawable.ezlink_card, "EZ-Link",
                    R.string.location_singapore,
                    CardType.CEPAS
            ));

            add(new CardInfo(R.drawable.hsl_card, "HSL",
                    R.string.location_helsinki_finland,
                    CardType.MifareDesfire
            ));

            add(new CardInfo(R.drawable.icoca_card, "ICOCA",
                    R.string.location_kansai,
                    CardType.FeliCa
            ));

            add(new CardInfo(R.drawable.manly_fast_ferry_card, ManlyFastFerryTransitData.NAME,
                    R.string.location_sydney_australia,
                    CardType.MifareClassic,
                    true
            ));

            add(new CardInfo(R.drawable.nets_card, "NETS FlashPay",
                    R.string.location_singapore,
                    CardType.CEPAS
            ));

            add(new CardInfo(R.drawable.opal_card, OpalTransitData.NAME,
                    R.string.location_sydney_australia,
                    CardType.MifareDesfire
            ));

            add(new CardInfo(R.drawable.orca_card, "ORCA",
                    R.string.location_seattle,
                    CardType.MifareDesfire
            ));

            add(new CardInfo(R.drawable.ovchip_card, "OV-chipkaart",
                    R.string.location_the_netherlands,
                    CardType.MifareClassic,
                    true
            ));

            add(new CardInfo(R.drawable.pasmo_card, "PASMO",
                    R.string.location_tokyo,
                    CardType.FeliCa
            ));

            add(new CardInfo(R.drawable.suica_card, "Suica",
                    R.string.location_tokyo,
                    CardType.FeliCa
            ));

        }

        @Override public View getView(int position, View convertView, ViewGroup group) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.supported_card, null);
            }

            CardInfo info = getItem(position);
            Spanned text = Html.fromHtml(String.format("<b>%s</b><br>%s", info.getName(),
                    getString(info.getLocationId())));

            ((ImageView) convertView.findViewById(R.id.image)).setImageResource(info.getImageId());
            ((TextView)  convertView.findViewById(R.id.text)).setText(text);

            String notes = "";

            FareBotApplication app = FareBotApplication.getInstance();
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(app);
            boolean nfcAvailable = nfcAdapter != null;

            if (nfcAvailable) {
                if (info.getCardType() == CardType.MifareClassic && !app.getMifareClassicSupport()) {
                    // Mifare Classic is not supported by this device.
                    notes += Utils.localizeString(R.string.card_not_supported_on_device) + " ";
                }

                if (info.getCardType() == CardType.CEPAS) {
                    // TODO: Implement feature detection for CEPAS like Mifare Classic.
                    // TODO: It is probably exposed in hasSystemFeature().
                    notes += Utils.localizeString(R.string.card_note_cepas) + " ";
                }
            } else {
                // This device does not support NFC, so all cards are not supported.
                notes += Utils.localizeString(R.string.card_not_supported_on_device) + " ";
            }

            // Keys being required is secondary to the card not being supported.
            if (info.getKeysRequired()) {
                notes += Utils.localizeString(R.string.keys_required) + " ";
            }

            if (!notes.equals("")) {
                ((TextView) convertView.findViewById(R.id.note)).setText(notes);
            }

            return convertView;
        }
    }

    private static class CardInfo {
        private final int mImageId;
        private final String mName;
        private final int mLocationId;
        private final CardType mCardType;
        private final boolean mKeysRequired;

        private CardInfo(int imageId, String name, int locationId) {
            this(imageId, name, locationId, CardType.Unknown);
        }

        private CardInfo(int imageId, String name, int locationId, CardType cardType) {
            this(imageId, name, locationId, cardType, false);
        }

        private CardInfo(int imageId, String name, int locationId, CardType cardType, boolean keysRequired) {
            mImageId      = imageId;
            mName         = name;
            mLocationId   = locationId;
            mCardType     = cardType;
            mKeysRequired = keysRequired;
        }

        public int getImageId() {
            return mImageId;
        }

        public String getName() {
            return mName;
        }

        public int getLocationId() {
            return mLocationId;
        }

        public CardType getCardType() {
            return mCardType;
        }

        public boolean getKeysRequired() {
            return mKeysRequired;
        }
    }
}
