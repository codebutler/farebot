/*
 * UnauthorizedCardFragment.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codebutler.farebot.R;
import com.codebutler.farebot.activity.AdvancedCardInfoActivity;
import com.codebutler.farebot.activity.CardInfoActivity;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.transit.TransitData;

public class UnauthorizedCardFragment extends Fragment {
    private Card mCard;
    private TransitData mTransitData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCard = getArguments().getParcelable(AdvancedCardInfoActivity.EXTRA_CARD);
        mTransitData = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_unauthorized_card, container, false);
        return view;
    }
}
