/*
 * OVChipSubscription.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012-2013 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ovc;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.codebutler.farebot.base.util.ByteUtils;
import com.codebutler.farebot.transit.Subscription;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.Date;
import java.util.Map;

@AutoValue
abstract class OVChipSubscription extends Subscription {

    private static final Map<Integer, String> SUBSCRIPTIONS = ImmutableMap.<Integer, String>builder()
            /* It seems that all the IDs are unique, so why bother with the companies? */
            /* NS */
            .put(0x0005, "OV-jaarkaart")
            .put(0x0007, "OV-Bijkaart 1e klas")
            .put(0x0011, "NS Businesscard")
            .put(0x0019, "Voordeelurenabonnement (twee jaar)")
            .put(0x00AF, "Studenten OV-chipkaart week (2009)")
            .put(0x00B0, "Studenten OV-chipkaart weekend (2009)")
            .put(0x00B1, "Studentenkaart korting week (2009)")
            .put(0x00B2, "Studentenkaart korting weekend (2009)")
            .put(0x00C9, "Reizen op saldo bij NS, 1e klasse")
            .put(0x00CA, "Reizen op saldo bij NS, 2de klasse")
            .put(0x00CE, "Voordeelurenabonnement reizen op saldo")
            .put(0x00E5, "Reizen op saldo (tijdelijk eerste klas)")
            .put(0x00E6, "Reizen op saldo (tijdelijk tweede klas)")
            .put(0x00E7, "Reizen op saldo (tijdelijk eerste klas korting)")
            /* Arriva */
            .put(0x059A, "Dalkorting")
            /* Veolia */
            .put(0x0626, "DALU Dalkorting")
            /* Connexxion */
            .put(0x0692, "Daluren Oost-Nederland")
            .put(0x069C, "Daluren Oost-Nederland")
            /* DUO */
            .put(0x09C6, "Student weekend-vrij")
            .put(0x09C7, "Student week-korting")
            .put(0x09C9, "Student week-vrij")
            .put(0x09CA, "Student weekend-korting")
            /* GVB */
            .put(0x0BBD, "Fietssupplement")
            .build();

    @NonNull
    static OVChipSubscription create(int subscriptionAddress, byte[] data, int type1, int type2, int used, int rest) {
        if (data == null) {
            data = new byte[48];
        }

        int id = 0;
        int company = 0;
        int subscription = 0;
        int unknown1 = 0;
        int validFromDate = 0;
        int validFromTime = 0;
        int validToDate = 0;
        int validToTime = 0;
        int unknown2 = 0;
        int machineId = 0;

        int iBitOffset = 0;
        int fieldbits = ByteUtils.getBitsFromBuffer(data, 0, 28);
        iBitOffset += 28;
        int subfieldbits = 0;

        if (fieldbits != 0x00) {
            if ((fieldbits & 0x0000200) != 0x00) {
                company = ByteUtils.getBitsFromBuffer(data, iBitOffset, 8);
                iBitOffset += 8;
            }

            if ((fieldbits & 0x0000400) != 0x00) {
                subscription = ByteUtils.getBitsFromBuffer(data, iBitOffset, 16);
                // skipping the first 8 bits, as they are not used OR don't belong to subscriptiontype at all
                iBitOffset += 24;
            }

            if ((fieldbits & 0x0000800) != 0x00) {
                id = ByteUtils.getBitsFromBuffer(data, iBitOffset, 24);
                iBitOffset += 24;
            }

            if ((fieldbits & 0x0002000) != 0x00) {
                unknown1 = ByteUtils.getBitsFromBuffer(data, iBitOffset, 10);
                iBitOffset += 10;
            }

            if ((fieldbits & 0x0200000) != 0x00) {
                subfieldbits = ByteUtils.getBitsFromBuffer(data, iBitOffset, 9);
                iBitOffset += 9;
            }

            if (subfieldbits != 0x00) {
                if ((subfieldbits & 0x0000001) != 0x00) {
                    validFromDate = ByteUtils.getBitsFromBuffer(data, iBitOffset, 14);
                    iBitOffset += 14;
                }

                if ((subfieldbits & 0x0000002) != 0x00) {
                    validFromTime = ByteUtils.getBitsFromBuffer(data, iBitOffset, 11);
                    iBitOffset += 11;
                }

                if ((subfieldbits & 0x0000004) != 0x00) {
                    validToDate = ByteUtils.getBitsFromBuffer(data, iBitOffset, 14);
                    iBitOffset += 14;
                }

                if ((subfieldbits & 0x0000008) != 0x00) {
                    validToTime = ByteUtils.getBitsFromBuffer(data, iBitOffset, 11);
                    iBitOffset += 11;
                }

                if ((subfieldbits & 0x0000010) != 0x00) {
                    unknown2 = ByteUtils.getBitsFromBuffer(data, iBitOffset, 53);
                    iBitOffset += 53;
                }
            }

            if ((fieldbits & 0x0800000) != 0x00) {
                machineId = ByteUtils.getBitsFromBuffer(data, iBitOffset, 24);
                iBitOffset += 24;
            }
        } else {
            throw new IllegalArgumentException("Not valid");
        }

        return new AutoValue_OVChipSubscription.Builder()
            .subscriptionAddress(subscriptionAddress)
            .type1(type1)
            .type2(type2)
            .used(used)
            .rest(rest)
            .id(id)
            .agency(company)
            .subscription(subscription)
            .unknown1(unknown1)
            .validFromDate(validFromDate)
            .validFromTime(validFromTime)
            .validToDate(validToDate)
            .validToTime(validToTime)
            .unknown2(unknown2)
            .machineId(machineId)
            .build();
    }

    @Override
    public Date getValidFrom() {
        if (getValidFromTime() != 0) {
            return OVChipUtil.convertDate((int) getValidFromDate(), (int) getValidFromTime());
        } else {
            return OVChipUtil.convertDate((int) getValidFromDate());
        }
    }

    @Override
    public Date getValidTo() {
        if (getValidToTime() != 0) {
            return OVChipUtil.convertDate((int) getValidToDate(), (int) getValidToTime());
        } else {
            return OVChipUtil.convertDate((int) getValidToDate());
        }
    }

    @Override
    public String getSubscriptionName(@NonNull Resources resources) {
        if (SUBSCRIPTIONS.containsKey(getSubscription())) {
            return SUBSCRIPTIONS.get(getSubscription());
        }
        return "Unknown Subscription (0x" + Long.toString(getSubscription(), 16) + ")";
    }

    @Override
    public String getActivation() {
        if (getType1() != 0) {
            return getUsed() != 0 ? "Activated and used" : "Activated but not used";
        }
        return "Deactivated";
    }

    @Override
    public String getAgencyName(@NonNull Resources resources) {
        return OVChipTransitInfo.getShortAgencyName(resources, getAgency());    // Nobody uses most of the long names
    }

    @Override
    public String getShortAgencyName(@NonNull Resources resources) {
        return OVChipTransitInfo.getShortAgencyName(resources, getAgency());
    }

    abstract int getUnknown1();

    abstract long getValidFromDate();

    abstract long getValidFromTime();

    abstract long getValidToDate();

    abstract long getValidToTime();

    abstract int getUnknown2();

    abstract int getAgency();

    abstract int getSubscription();

    abstract int getSubscriptionAddress();

    abstract int getType1();

    abstract int getType2();

    abstract int getUsed();

    abstract int getRest();

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder id(int id);

        abstract Builder unknown1(int unknown1);

        abstract Builder validFromDate(long validFromDate);

        abstract Builder validFromTime(long validFromTime);

        abstract Builder validToDate(long validToDate);

        abstract Builder validToTime(long validToTime);

        abstract Builder unknown2(int unknown2);

        abstract Builder agency(int agency);

        abstract Builder machineId(int machineId);

        abstract Builder subscription(int subscription);

        abstract Builder subscriptionAddress(int subscriptionAddress);

        abstract Builder type1(int type1);

        abstract Builder type2(int type2);

        abstract Builder used(int used);

        abstract Builder rest(int rest);

        abstract OVChipSubscription build();
    }
}
