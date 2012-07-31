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

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.codebutler.farebot.R;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.activities.AdvancedCardInfoActivity;
import com.codebutler.farebot.cepas.CEPASCard;
import com.codebutler.farebot.cepas.CEPASPurse;
import com.codebutler.farebot.felica.FelicaCard;
import com.codebutler.farebot.mifare.Card;
import com.codebutler.farebot.mifare.Card.CardType;
import com.codebutler.farebot.mifare.DesfireCard;
import com.codebutler.farebot.mifare.DesfireManufacturingData;
import com.codebutler.farebot.ovchip.OVChipCard;
import com.codebutler.farebot.transit.OVChipTransitData;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CardHWDetailFragment extends SherlockListFragment
{
    private Card mCard;

    @Override
    public void onCreate (Bundle savedInstanceState)
    {
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
        } else if (mCard.getCardType() == CardType.MifareClassic) {
        	OVChipCard card = (OVChipCard)mCard;
        	
        	/*
        	 * TODO: Get the following somewhere (they don't need access to the actual tag):
        	 * Log.d("INFO", "Blocks: " + tech.getBlockCount());
        	 * Log.d("INFO", "Sectors: " + tech.getSectorCount());
        	 * Log.d("INFO", "Size: " + tech.getSize());
        	 * Log.d("INFO", "Type: " + tech.getType());
        	 */
        	
        	items.add(new HeaderListItem("Hardware Information"));
        	items.add(new ListItem("Manufacturer ID",	card.getOVCPreamble().getManufacturer()));
        	items.add(new ListItem("Publisher ID",      card.getOVCPreamble().getPublisher()));
        	//items.add(new ListItem("Type",      .....));
        	//items.add(new ListItem("Blocks",      .....));
        	//items.add(new ListItem("Sectors",      .....));
        	//items.add(new ListItem("Size",      .....));

        	items.add(new HeaderListItem("General Information"));
	        items.add(new ListItem("Serial Number",		card.getOVChipPreamble().getId()));
	        items.add(new ListItem("Expiration Date",	DateFormat.getDateInstance(DateFormat.LONG).format(OVChipTransitData.convertDate(card.getOVChipPreamble().getExpdate()))));
	        items.add(new ListItem("Card Type",			(card.getOVChipPreamble().getType() == 2 ? "Personal" : "Anonymous")));
	        
	        if (card.getComplete()) {
		        items.add(new ListItem("Banned", ((card.getOVChipCredit().getBanbits() & (byte)0xC0) == (byte)0xC0) ? "Yes" : "No"));
	        	
	        	items.add(new HeaderListItem("Recent Slots"));
	        	items.add(new ListItem("Transaction Slot",		"0x" + Integer.toHexString((char)card.getOVChipIndex().getRecentTransactionSlot())));
	        	items.add(new ListItem("Info Slot",				"0x" + Integer.toHexString((char)card.getOVChipIndex().getRecentInfoSlot())));
	        	items.add(new ListItem("Subscription Slot",		"0x" + Integer.toHexString((char)card.getOVChipIndex().getRecentSubscriptionSlot())));
	        	items.add(new ListItem("Travelhistory Slot",	"0x" + Integer.toHexString((char)card.getOVChipIndex().getRecentTravelhistorySlot())));
	        	items.add(new ListItem("Credit Slot",			"0x" + Integer.toHexString((char)card.getOVChipIndex().getRecentCreditSlot())));
			    
	        	if (card.getOVChipPreamble().getType() == 2)
	        	{
		        	items.add(new HeaderListItem("Personal Information"));
		        	items.add(new ListItem("Birthdate",     DateFormat.getDateInstance(DateFormat.LONG).format(card.getOVChipInfo().getBirthdate())));
		        }

	        	items.add(new HeaderListItem("Credit Information"));
	        	items.add(new ListItem("Credit Slot ID",	Integer.toString(card.getOVChipCredit().getId())));
	        	items.add(new ListItem("Last Credit ID",	Integer.toString(card.getOVChipCredit().getCreditId())));
	        	items.add(new ListItem("Credit",			OVChipTransitData.convertAmount(card.getOVChipCredit().getCredit())));
	        	items.add(new ListItem("Autocharge",		(card.getOVChipInfo().getActive() == (byte)0x05 ? "Yes" : "No")));
	        	items.add(new ListItem("Autocharge Limit",	OVChipTransitData.convertAmount(card.getOVChipInfo().getLimit())));
	        	items.add(new ListItem("Autocharge Charge",	OVChipTransitData.convertAmount(card.getOVChipInfo().getCharge())));
        	}
        }

        setListAdapter(new HWDetailListAdapter(getActivity(), items));
    }

    private class HWDetailListAdapter extends ArrayAdapter<ListItem> {
        private HWDetailListAdapter (Context context, List<ListItem> items)
        {
            super(context, 0, items);
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent)
        {
            ListItem item = (ListItem) getListAdapter().getItem(position);
            if (convertView != null) {
                Log.i("CardHWDetailFragment", "ID: " + convertView.getId());
            }
            if (item instanceof HeaderListItem) {
                if (convertView == null || convertView.getId() != android.R.id.text1)
                    convertView = getActivity().getLayoutInflater().inflate(R.layout.list_header, null);
                ((TextView) convertView.findViewById(android.R.id.text1)).setText(item.getText1());
            } else {
                if (convertView == null || convertView.getId() == android.R.id.text1)
                    convertView = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
                ((TextView) convertView.findViewById(android.R.id.text1)).setText(item.getText1());
                ((TextView) convertView.findViewById(android.R.id.text2)).setText(item.getText2());
            }
            return convertView;
        }
    }

    private class ListItem
    {
        protected final String mText1;
        protected final String mText2;

        public ListItem (String name, String value)
        {
            mText1 = name;
            mText2 = value;
        }

        public String getText1 () {
            return mText1;
        }

        public String getText2 () {
            return mText2;
        }
    }

    private class HeaderListItem extends ListItem
    {
        public HeaderListItem (String title)
        {
            super(title, null);
        }
    }
}
