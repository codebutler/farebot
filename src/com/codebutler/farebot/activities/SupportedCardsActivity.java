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
            add(new CardInfo(R.drawable.orca_card,    "ORCA",    "Seattle, WA"));
            add(new CardInfo(R.drawable.clipper_card, "Clipper", "San Francisco, CA"));
            add(new CardInfo(R.drawable.ezlink_card,  "EZ-Link", "Singapore"));
            add(new CardInfo(R.drawable.suica_card,   "Suica",   "Tokyo, Japan"));
            add(new CardInfo(R.drawable.pasmo_card,   "PASMO",   "Tokyo, Japan"));
            add(new CardInfo(R.drawable.icoca_card,   "ICOCA",   "Kansai, Japan"));
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup group) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.supported_card, null);
            }

            CardInfo info = getItem(position);
            ((ImageView) convertView.findViewById(R.id.image)).setImageResource(info.getImageId());
            ((TextView)  convertView.findViewById(R.id.text)).setText(Html.fromHtml(String.format("<b>%s</b><br>%s", info.getName(), info.getLocation())));
            
            return convertView;
        }
    }

    /*
        private class CardsAdapter extends PagerAdapter {
        private List<CardInfo> mCards = new ArrayList<CardInfo>();

        public CardsAdapter() {
            super();
            mCards.add(new CardInfo(R.drawable.orca_card,    "ORCA",    "Seattle, WA"));
            mCards.add(new CardInfo(R.drawable.clipper_card, "Clipper", "San Francisco, CA"));
            mCards.add(new CardInfo(R.drawable.ezlink_card,  "EZ-Link", "Singapore"));
            mCards.add(new CardInfo(R.drawable.suica_card,   "Suica",   "Tokyo, Japan"));
            mCards.add(new CardInfo(R.drawable.icoca_card,   "ICOCA",   "Kansai, Japan"));
            mCards.add(new CardInfo(R.drawable.pasmo_card,   "PASMO",   "Tokyo, Japan"));
        }

        @Override
        public int getCount() {
            return mCards.size();
        }

        @Override
        public Object instantiateItem(View collection, int position) {
            View view = getLayoutInflater().inflate(R.layout.supported_card, null);

            CardInfo info = mCards.get(position);
            ((ImageView) view.findViewById(R.id.image)).setImageResource(info.getImageId());
            ((TextView)  view.findViewById(R.id.text)).setText(Html.fromHtml(String.format("<b>%s</b><br>%s", info.getName(), info.getLocation())));

            view.setBackgroundColor(Color.BLUE);

            ((ViewPager) collection).addView(view);

            return view;
        }

        @Override
        public void destroyItem(View collection, int position, Object view) {
            ((ViewPager) collection).removeView((View) view);
        }

        @Override
        public void startUpdate(View collection) {
        }

        @Override
        public void finishUpdate(View collection) {
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return (view == object);
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable parcelable, ClassLoader classLoader) {
        }
    }
     */

    private static class CardInfo {
        private final int mImageId;
        private final String mName;
        private final String mLocation;

        private CardInfo(int imageId, String name, String location) {
            mImageId  = imageId;
            mName     = name;
            mLocation = location;
        }

        public int getImageId() {
            return mImageId;
        }

        public String getName() {
            return mName;
        }

        public String getLocation() {
            return mLocation;
        }
    }
}
