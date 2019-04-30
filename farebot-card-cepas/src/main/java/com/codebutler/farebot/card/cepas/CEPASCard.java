/*
 * CEPASCard.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014, 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
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

package com.codebutler.farebot.card.cepas;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.base.ui.FareBotUiTree;
import com.codebutler.farebot.base.util.ByteArray;
import com.google.auto.value.AutoValue;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@AutoValue
public abstract class CEPASCard extends Card {

    @NonNull
    public static CEPASCard create(
            @NonNull ByteArray tagId,
            @NonNull Date scannedAt,
            @NonNull List<CEPASPurse> purses,
            @NonNull List<CEPASHistory> histories) {
        return new AutoValue_CEPASCard(tagId, scannedAt, purses, histories);
    }

    @NonNull
    @Override
    public CardType getCardType() {
        return CardType.CEPAS;
    }

    @NonNull
    public abstract List<CEPASPurse> getPurses();

    @NonNull
    public abstract List<CEPASHistory> getHistories();

    @Nullable
    public CEPASPurse getPurse(int purse) {
        return getPurses().get(purse);
    }

    @Nullable
    public CEPASHistory getHistory(int purse) {
        return getHistories().get(purse);
    }

    @NonNull
    @Override
    public FareBotUiTree getAdvancedUi(Context context) {
        FareBotUiTree.Builder cardUiBuilder = FareBotUiTree.builder(context);

        FareBotUiTree.Item.Builder pursesUiBuilder = cardUiBuilder.item().title("Purses");
        for (CEPASPurse purse : getPurses()) {
            FareBotUiTree.Item.Builder purseUiBuilder = pursesUiBuilder.item()
                    .title(String.format("Purse ID %s", purse.getId()));
            purseUiBuilder.item().title("CEPAS Version").value(purse.getCepasVersion());
            purseUiBuilder.item().title("Purse Status").value(purse.getPurseStatus());
            purseUiBuilder.item().title("Purse Balance")
                    .value(NumberFormat.getCurrencyInstance(Locale.US).format(purse.getPurseBalance() / 100.0));
            purseUiBuilder.item().title("Purse Creation Date")
                    .value(DateFormat.getDateInstance(DateFormat.LONG).format(purse.getPurseCreationDate() * 1000L));
            purseUiBuilder.item().title("Purse Expiry Date")
                    .value(DateFormat.getDateInstance(DateFormat.LONG).format(purse.getPurseExpiryDate() * 1000L));
            purseUiBuilder.item().title("Autoload Amount").value(purse.getAutoLoadAmount());
            purseUiBuilder.item().title("CAN").value(purse.getCAN());
            purseUiBuilder.item().title("CSN").value(purse.getCSN());

            FareBotUiTree.Item.Builder transactionUiBuilder
                    = cardUiBuilder.item().title("Last Transaction Information");
            transactionUiBuilder.item().title("TRP").value(purse.getLastTransactionTRP());
            transactionUiBuilder.item().title("Credit TRP").value(purse.getLastCreditTransactionTRP());
            transactionUiBuilder.item().title("Credit Header").value(purse.getLastCreditTransactionHeader());
            transactionUiBuilder.item().title("Debit Options").value(purse.getLastTransactionDebitOptionsByte());

            FareBotUiTree.Item.Builder otherUiBuilder = cardUiBuilder.item().title("Other Purse Information");
            otherUiBuilder.item().title("Logfile Record Count").value(purse.getLogfileRecordCount());
            otherUiBuilder.item().title("Issuer Data Length").value(purse.getIssuerDataLength());
            otherUiBuilder.item().title("Issuer-specific Data").value(purse.getIssuerSpecificData());
        }

        return cardUiBuilder.build();
    }
}
