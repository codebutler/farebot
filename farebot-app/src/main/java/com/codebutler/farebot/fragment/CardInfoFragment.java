/*
 * CardInfoFragment.java
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

package com.codebutler.farebot.fragment;

import android.app.ListFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.codebutler.farebot.activity.AdvancedCardInfoActivity;
import com.codebutler.farebot.activity.CardInfoActivity;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.core.ui.ListItem;
import com.codebutler.farebot.core.ui.UriListItem;
import com.codebutler.farebot.transit.TransitData;

public class CardInfoFragment extends ListFragment {
    private Card mCard;
    private TransitData mTransitData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCard = getArguments().getParcelable(AdvancedCardInfoActivity.EXTRA_CARD);
        mTransitData = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setListAdapter(new ListItemAdapter(getActivity(), mTransitData.getInfo(getContext())));
    }

    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
        ListItem listItem = (ListItem) getListAdapter().getItem(position);

        if (listItem instanceof UriListItem) {
            Uri uri = ((UriListItem) listItem).getUri();
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }
}
