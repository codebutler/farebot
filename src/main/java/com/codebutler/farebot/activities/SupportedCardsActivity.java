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

package com.codebutler.farebot.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.codebutler.farebot.R;

import java.util.ArrayList;

public class SupportedCardsActivity extends SherlockActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supported_cards);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        ((Gallery) findViewById(R.id.gallery)).setAdapter(new CardsAdapter(this));
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
    
    private class CardsAdapter extends ArrayAdapter<CardInfo> {
        public CardsAdapter(Context context) {
            super(context, 0, new ArrayList<CardInfo>());
            add(new CardInfo(R.drawable.orca_card,           "ORCA",          R.string.location_seattle));
            add(new CardInfo(R.drawable.clipper_card,        "Clipper",       R.string.location_san_francisco));
            add(new CardInfo(R.drawable.ezlink_card,         "EZ-Link",       R.string.location_singapore,       R.string.card_note_ezlink));
            add(new CardInfo(R.drawable.nets_card,           "NETS FlashPay", R.string.location_singapore));
            add(new CardInfo(R.drawable.suica_card,          "Suica",         R.string.location_tokyo));
            add(new CardInfo(R.drawable.pasmo_card,          "PASMO",         R.string.location_tokyo));
            add(new CardInfo(R.drawable.edy_card,            "Edy",           R.string.location_tokyo));
            add(new CardInfo(R.drawable.icoca_card,          "ICOCA",         R.string.location_kansai));
            add(new CardInfo(R.drawable.ovchip_card,         "OV-chipkaart",  R.string.location_the_netherlands, R.string.card_note_ovchip));
            add(new CardInfo(R.drawable.bilheteunicosp_card, "Bilhete Único", R.string.location_sao_paulo,       R.string.card_note_bilheteunicosp));
            add(new CardInfo(R.drawable.hsl_card,            "HSL",           R.string.location_helsinki_finland));
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup group) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.supported_card, null);
            }

            CardInfo info = getItem(position);
            Spanned text = Html.fromHtml(String.format("<b>%s</b><br>%s", info.getName(), getString(info.getLocationId())));

            ((ImageView) convertView.findViewById(R.id.image)).setImageResource(info.getImageId());
            ((TextView)  convertView.findViewById(R.id.text)).setText(text);

            if (info.getNoteId() >= 0) {
                ((TextView) convertView.findViewById(R.id.note)).setText(info.getNoteId());
            }
            
            return convertView;
        }
    }

    private static class CardInfo {
        private final int mImageId;
        private final String mName;
        private final int mLocationId;
        private final int mNoteId;

        private CardInfo(int imageId, String name, int locationId) {
            this(imageId, name, locationId, -1);
        }

        private CardInfo(int imageId, String name, int locationId, int noteId) {
            mImageId    = imageId;
            mName       = name;
            mLocationId = locationId;
            mNoteId     = noteId;
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

        public int getNoteId() {
            return mNoteId;
        }
    }
}
