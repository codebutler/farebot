/*
 * OrcaFrefill.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2018 Karl Koscher <supersat@cs.washington.edu>
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

package com.codebutler.farebot.transit.orca;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.desfire.DesfireRecord;
import com.codebutler.farebot.transit.Refill;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.Locale;

@AutoValue
public abstract class OrcaRefill extends Refill {
    @NonNull
    static OrcaRefill create(@NonNull DesfireRecord record) {
        byte[] useData = record.getData().bytes();
        long[] usefulData = new long[useData.length];

        for (int i = 0; i < useData.length; i++) {
            usefulData[i] = ((long) useData[i]) & 0xFF;
        }

        long timestamp = ((0x0F & usefulData[3]) << 28)
                | (usefulData[4] << 20)
                | (usefulData[5] << 12)
                | (usefulData[6] << 4)
                | (usefulData[7] >> 4);

        long ftpType = ((usefulData[7] & 0xf) << 4) | ((usefulData[8] & 0xf0) >> 4);
        long ftpId = ((usefulData[8] & 0xf) << 20) | (usefulData[9] << 12)
                | (usefulData[10] << 4) | ((usefulData[11] & 0xf0) >> 4);

        long amount;
        amount = (usefulData[15] << 7) | (usefulData[16] >> 1);

        long newBalance = (usefulData[34] << 8) | usefulData[35];
        long agency = usefulData[3] >> 4;
        long transType = (usefulData[17]);

        return new AutoValue_OrcaRefill(timestamp, amount, agency);
    }

    @Override
    public String getAgencyName(@NonNull Resources resources) {
        switch ((int) getAgency()) {
            case OrcaTransitInfo.AGENCY_CT:
                return resources.getString(R.string.transit_orca_agency_ct);
            case OrcaTransitInfo.AGENCY_KCM:
                return resources.getString(R.string.transit_orca_agency_kcm);
            case OrcaTransitInfo.AGENCY_PT:
                return resources.getString(R.string.transit_orca_agency_pt);
            case OrcaTransitInfo.AGENCY_ST:
                return resources.getString(R.string.transit_orca_agency_st);
            case OrcaTransitInfo.AGENCY_WSF:
                return resources.getString(R.string.transit_orca_agency_wsf);
            case OrcaTransitInfo.AGENCY_ET:
                return resources.getString(R.string.transit_orca_agency_et);
            case OrcaTransitInfo.AGENCY_KT:
                return resources.getString(R.string.transit_orca_agency_kt);
        }
        return resources.getString(R.string.transit_orca_agency_unknown, Long.toString(getAgency()));
    }

    @Override
    public String getShortAgencyName(@NonNull Resources resources) {
        switch ((int) getAgency()) {
            case OrcaTransitInfo.AGENCY_CT:
                return "CT";
            case OrcaTransitInfo.AGENCY_KCM:
                return "KCM";
            case OrcaTransitInfo.AGENCY_PT:
                return "PT";
            case OrcaTransitInfo.AGENCY_ST:
                return "ST";
            case OrcaTransitInfo.AGENCY_WSF:
                return "WSF";
            case OrcaTransitInfo.AGENCY_ET:
                return "ET";
            case OrcaTransitInfo.AGENCY_KT:
                return "KT";
        }
        return resources.getString(R.string.transit_orca_agency_unknown, Long.toString(getAgency()));
    }

    @Override
    public String getAmountString(Resources resources) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(getAmount() / 100);
    }

    abstract long getAgency();
}