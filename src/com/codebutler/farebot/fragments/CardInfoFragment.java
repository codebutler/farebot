/*
 * CardInfoFragment.java
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

import android.os.Bundle;
import android.view.View;
import com.actionbarsherlock.app.SherlockListFragment;
import com.codebutler.farebot.activities.AdvancedCardInfoActivity;
import com.codebutler.farebot.activities.CardInfoActivity;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.transit.TransitData;

public class CardInfoFragment extends SherlockListFragment {
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

        setListAdapter(new ListItemAdapter(getActivity(), mTransitData.getInfo()));
    }
}
