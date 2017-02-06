/*
 * CardSubscriptionsFragment.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.fragment;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.codebutler.farebot.R;
import com.codebutler.farebot.activity.CardInfoActivity;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.core.Constants;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.util.Utils;

import java.util.List;

public class CardSubscriptionsFragment extends ListFragment {
    private Card mCard;
    private TransitInfo mTransitInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCard = getArguments().getParcelable(Constants.EXTRA_CARD);
        mTransitInfo = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_INFO);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(new SubscriptionsAdapter(getActivity(), mTransitInfo.getSubscriptions()));
    }

    private class SubscriptionsAdapter extends ArrayAdapter<Subscription> {
        SubscriptionsAdapter(Context context, List<Subscription> subscriptions) {
            super(context, 0, subscriptions);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getActivity().getLayoutInflater().inflate(R.layout.subscription_item, parent, false);
            }

            Subscription subscription = getItem(position);

            String validFrom = Utils.dateFormat(getContext(), subscription.getValidFrom());
            String validTo = Utils.dateFormat(getContext(), subscription.getValidTo());

            ((TextView) view.findViewById(R.id.company)).setText(subscription.getShortAgencyName(getResources()));
            ((TextView) view.findViewById(R.id.name)).setText(subscription.getSubscriptionName(getResources()));
            ((TextView) view.findViewById(R.id.valid)).setText(getString(R.string.valid_format, validFrom, validTo));
            ((TextView) view.findViewById(R.id.used)).setText(subscription.getActivation());

            return view;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }
    }
}
