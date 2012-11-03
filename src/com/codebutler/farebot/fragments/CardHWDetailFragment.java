/*
 * CardHWDetailActivity.java
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

package com.codebutler.farebot.fragments;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockListFragment;
import com.codebutler.farebot.HeaderListItem;
import com.codebutler.farebot.ListItem;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.activities.AdvancedCardInfoActivity;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.Card.CardType;
import com.codebutler.farebot.card.cepas.CEPASCard;
import com.codebutler.farebot.card.cepas.CEPASPurse;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.desfire.DesfireManufacturingData;
import com.codebutler.farebot.card.felica.FelicaCard;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CardHWDetailFragment extends SherlockListFragment {
    private Card mCard;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCard = getArguments().getParcelable(AdvancedCardInfoActivity.EXTRA_CARD);
        
        List<ListItem> items = new ArrayList<ListItem>();

        if (mCard.getCardType() == CardType.MifareDesfire) {
            DesfireManufacturingData data = ((DesfireCard)mCard).getManufacturingData();
            items.add(new HeaderListItem("Hardware Information"));
            items.add(new ListItem("Vendor ID",     Integer.toString(data.hwVendorID)));
            items.add(new ListItem("Type",          Integer.toString(data.hwType)));
            items.add(new ListItem("Subtype",       Integer.toString(data.hwSubType)));
            items.add(new ListItem("Major Version", Integer.toString(data.hwMajorVersion)));
            items.add(new ListItem("Minor Version", Integer.toString(data.hwMinorVersion)));
            items.add(new ListItem("Storage Size",  Integer.toString(data.hwStorageSize)));
            items.add(new ListItem("Protocol",      Integer.toString(data.hwProtocol)));

            items.add(new HeaderListItem("Software Information"));
            items.add(new ListItem("Vendor ID",     Integer.toString(data.swVendorID)));
            items.add(new ListItem("Type",          Integer.toString(data.swType)));
            items.add(new ListItem("Subtype",       Integer.toString(data.swSubType)));
            items.add(new ListItem("Major Version", Integer.toString(data.swMajorVersion)));
            items.add(new ListItem("Minor Version", Integer.toString(data.swMinorVersion)));
            items.add(new ListItem("Storage Size",  Integer.toString(data.swStorageSize)));
            items.add(new ListItem("Protocol",      Integer.toString(data.swProtocol)));

            items.add(new HeaderListItem("General Information"));
            items.add(new ListItem("Serial Number",      Integer.toString(data.uid)));
            items.add(new ListItem("Batch Number",       Integer.toString(data.batchNo)));
            items.add(new ListItem("Week of Production", Integer.toString(data.weekProd)));
            items.add(new ListItem("Year of Production", Integer.toString(data.yearProd)));

        } else if (mCard.getCardType() == CardType.CEPAS) {
            CEPASCard card = (CEPASCard)mCard;

            // FIXME: What about other purses?
            CEPASPurse purse = card.getPurse(3);
            items.add(new HeaderListItem("Purse Information"));
            items.add(new ListItem("CEPAS Version", Byte.toString(purse.getCepasVersion())));
            items.add(new ListItem("Purse ID",     Integer.toString(purse.getId())));
            items.add(new ListItem("Purse Status", Byte.toString(purse.getPurseStatus())));
            items.add(new ListItem("Purse Balance", NumberFormat.getCurrencyInstance(Locale.US).format(purse.getPurseBalance()/100.0)));

            items.add(new ListItem("Purse Creation Date", DateFormat.getDateInstance(DateFormat.LONG).format(purse.getPurseCreationDate()*1000L)));
            items.add(new ListItem("Purse Expiry Date", DateFormat.getDateInstance(DateFormat.LONG).format(purse.getPurseExpiryDate()*1000L)));
            items.add(new ListItem("Autoload Amount", Integer.toString(purse.getAutoLoadAmount())));
            items.add(new ListItem("CAN", Utils.getHexString(purse.getCAN(), "<Error>")));
            items.add(new ListItem("CSN", Utils.getHexString(purse.getCSN(), "<Error>")));

            items.add(new HeaderListItem("Last Transaction Information"));
            items.add(new ListItem("TRP", Integer.toString(purse.getLastTransactionTRP())));
            items.add(new ListItem("Credit TRP", Integer.toString(purse.getLastCreditTransactionTRP())));
            items.add(new ListItem("Credit Header", Utils.getHexString(purse.getLastCreditTransactionHeader(), "<Error>")));
            items.add(new ListItem("Debit Options", Byte.toString(purse.getLastTransactionDebitOptionsByte())));

            items.add(new HeaderListItem("Other Purse Information"));
            items.add(new ListItem("Logfile Record Count", Byte.toString(purse.getLogfileRecordCount())));
            items.add(new ListItem("Issuer Data Length", Integer.toString(purse.getIssuerDataLength())));
            items.add(new ListItem("Issuer-specific Data", Utils.getHexString(purse.getIssuerSpecificData(), "<Error>")));

        } else if (mCard.getCardType() == CardType.FeliCa) {
            FelicaCard card = (FelicaCard) mCard;
            items.add(new ListItem("IDm", Utils.getHexString(card.getIDm().getBytes(), "err")));
            items.add(new ListItem("PMm", Utils.getHexString(card.getPMm().getBytes(), "err")));
        }

        setListAdapter(new ListItemAdapter(getActivity(), items));
    }
}
