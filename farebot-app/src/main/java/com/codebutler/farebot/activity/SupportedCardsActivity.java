/*
 * SupportedCardsActivity.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.codebutler.farebot.R;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.transit.manly_fast_ferry.ManlyFastFerryTransitInfo;
import com.codebutler.farebot.transit.myki.MykiTransitInfo;
import com.codebutler.farebot.transit.octopus.OctopusTransitInfo;
import com.codebutler.farebot.transit.opal.OpalTransitInfo;
import com.codebutler.farebot.transit.seq_go.SeqGoTransitInfo;

import java.util.ArrayList;

/**
 * @author Eric Butler, Michael Farrell
 */
public class SupportedCardsActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supported_cards);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        ((ListView) findViewById(R.id.gallery)).setAdapter(new CardsAdapter(this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }
        return false;
    }

    public boolean getMifareClassicSupport() {
        return getPackageManager().hasSystemFeature("com.nxp.mifare");
    }

    private class CardsAdapter extends ArrayAdapter<CardInfo> {
        CardsAdapter(Context context) {
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

            add(new CardInfo(R.drawable.seqgo_card, SeqGoTransitInfo.NAME,
                    R.string.location_brisbane_seq_australia,
                    CardType.MifareClassic,
                    true,
                    true,
                    R.string.seqgo_card_note
            ));

            add(new CardInfo(R.drawable.hsl_card, "HSL",
                    R.string.location_helsinki_finland,
                    CardType.MifareDesfire
            ));

            add(new CardInfo(R.drawable.icoca_card, "ICOCA",
                    R.string.location_kansai,
                    CardType.FeliCa
            ));

            add(new CardInfo(R.drawable.manly_fast_ferry_card, ManlyFastFerryTransitInfo.NAME,
                    R.string.location_sydney_australia,
                    CardType.MifareClassic,
                    true
            ));

            add(new CardInfo(R.drawable.myki_card, MykiTransitInfo.NAME,
                    R.string.location_victoria_australia,
                    CardType.MifareDesfire,
                    false,
                    false,
                    R.string.myki_card_note
            ));

            add(new CardInfo(R.drawable.nets_card, "NETS FlashPay",
                    R.string.location_singapore,
                    CardType.CEPAS
            ));

            add(new CardInfo(R.drawable.octopus_card, OctopusTransitInfo.OCTOPUS_NAME,
                    R.string.location_hong_kong,
                    CardType.FeliCa
            ));

            add(new CardInfo(R.drawable.opal_card, OpalTransitInfo.NAME,
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

        @Override
        public View getView(int position, View convertView, ViewGroup group) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.supported_card, null);
            }

            CardInfo info = getItem(position);
            Spanned text = Html.fromHtml(String.format("<b>%s</b><br>%s", info.getName(),
                    getString(info.getLocationId())));

            ((ImageView) convertView.findViewById(R.id.image)).setImageResource(info.getImageId());
            ((TextView) convertView.findViewById(R.id.text)).setText(text);

            String notes = "";

            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
            boolean nfcAvailable = nfcAdapter != null;

            if (nfcAvailable) {
                if (info.getCardType() == CardType.MifareClassic && !getMifareClassicSupport()) {
                    // Mifare Classic is not supported by this device.
                    notes += getString(R.string.card_not_supported_on_device) + " ";
                }

                if (info.getCardType() == CardType.CEPAS) {
                    // TODO: Implement feature detection for CEPAS like Mifare Classic.
                    // TODO: It is probably exposed in hasSystemFeature().
                    notes += getString(R.string.card_note_cepas) + " ";
                }
            } else {
                // This device does not support NFC, so all cards are not supported.
                notes += getString(R.string.card_not_supported_on_device) + " ";
            }

            // Keys being required is secondary to the card not being supported.
            if (info.getKeysRequired()) {
                notes += getString(R.string.keys_required) + " ";
            }

            if (info.getPreview()) {
                notes += getString(R.string.card_preview_reader) + " ";
            }

            if (info.getResourceExtraNote() != 0) {
                notes += getString(info.getResourceExtraNote()) + " ";
            }

            ((TextView) convertView.findViewById(R.id.note)).setText(notes);

            return convertView;
        }
    }

    private static class CardInfo {
        private final int mImageId;
        private final String mName;
        private final int mLocationId;
        private final CardType mCardType;
        private final boolean mKeysRequired;
        private final boolean mPreview;
        private final int mResourceExtraNote;

        private CardInfo(int imageId, String name, int locationId, CardType cardType) {
            this(imageId, name, locationId, cardType, false);
        }

        private CardInfo(int imageId, String name, int locationId, CardType cardType, boolean keysRequired) {
            this(imageId, name, locationId, cardType, keysRequired, false);
        }

        private CardInfo(
                int imageId,
                String name,
                int locationId,
                CardType cardType,
                boolean keysRequired,
                boolean preview) {
            this(imageId, name, locationId, cardType, keysRequired, false, 0);
        }

        private CardInfo(
                int imageId,
                String name,
                int locationId,
                CardType cardType,
                boolean keysRequired,
                boolean preview,
                int resourceExtraNote) {
            mImageId = imageId;
            mName = name;
            mLocationId = locationId;
            mCardType = cardType;
            mKeysRequired = keysRequired;
            mPreview = preview;
            mResourceExtraNote = resourceExtraNote;
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

        /**
         * Indicates if the card is a "preview" / beta decoder, with possibly
         * incomplete / incorrect data.
         *
         * @return true if this is a beta version of the card decoder.
         */
        public boolean getPreview() {
            return mPreview;
        }

        public int getResourceExtraNote() {
            return mResourceExtraNote;
        }
    }
}
