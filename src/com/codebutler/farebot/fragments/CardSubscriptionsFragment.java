/*
 * CardSubscriptionsFragment.java
 *
 * Copyright (C) 2012 Eric Butler
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

package com.codebutler.farebot.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.codebutler.farebot.R;
import com.codebutler.farebot.activities.AdvancedCardInfoActivity;
import com.codebutler.farebot.activities.CardInfoActivity;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CardSubscriptionsFragment extends SherlockListFragment {
    private Card mCard;
    private TransitData mTransitData;

    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCard        = (Card)        getArguments().getParcelable(AdvancedCardInfoActivity.EXTRA_CARD);
        mTransitData = (TransitData) getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(new SubscriptionsAdapter(getActivity(), mTransitData.getSubscriptions()));
    }

    private class SubscriptionsAdapter extends ArrayAdapter<Subscription> {
        public SubscriptionsAdapter(Context context, Subscription[] subscriptions) {
            super(context, 0, subscriptions);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getActivity().getLayoutInflater().inflate(R.layout.subscription_item, parent, false);
            }

            Subscription subscription = getItem(position);

            Date validFrom = new Date(subscription.getValidFrom());
            Date validTo   = new Date(subscription.getValidTo());

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            ((TextView) view.findViewById(R.id.company)).setText(subscription.getShortAgencyName());
            ((TextView) view.findViewById(R.id.name)).setText(subscription.getSubscriptionName());
            ((TextView) view.findViewById(R.id.valid)).setText(getString(R.string.valid_format, dateFormat.format(validFrom), dateFormat.format(validTo)));

            return view;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }
    }
}
