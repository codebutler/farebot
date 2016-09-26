/*
 * HSLTransitData.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2013 Lauri Andler <lauri.andler@gmail.com>
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.hsl;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.desfire.DesfireFile;
import com.codebutler.farebot.card.desfire.RecordDesfireFile;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@AutoValue
public abstract class HSLTransitData extends TransitData {

    private static final long EPOCH = 0x32C97ED0;

    /*
    private static final String[] regionNames = {
        "N/A", "Helsinki", "Espoo", "Vantaa", "Koko alue", "Seutu", "", "", "", "",  // 0-9
        "", "", "", "", "", "", "", "", "", "", // 10-19
        "", "", "", "", "", "", "", "", "", "", // 20-29
        "", "", "", "", "", "", "", "", "", ""}; // 30-39
    private static final Map<Long,String> vehicleNames =  Collections.unmodifiableMap(new HashMap<Long, String>() {{
        put(1L, "Metro");
        put(18L, "Bus");
        put(16L, "Tram");
    }});
    */

    @NonNull
    public static HSLTransitData create(@NonNull Card card) {
        try {
            DesfireCard desfireCard = (DesfireCard) card;

            byte[] data = desfireCard.getApplication(0x1120ef).getFile(0x08).getData().bytes();
            String serialNumber = Utils.getHexString(data).substring(2, 20);  //Utils.byteArrayToInt(data, 1, 9);

            data = desfireCard.getApplication(0x1120ef).getFile(0x02).getData().bytes();
            long balance = bitsToLong(0, 20, data);
            HSLRefill lastRefill = HSLRefill.create(data);

            List<HSLTrip> trips = parseTrips(desfireCard);

            int balanceIndex = -1;

            for (int i = 0; i < trips.size(); ++i) {
                if (trips.get(i).getArvo() == 1) {
                    balanceIndex = i;
                    break;
                }
            }

            data = desfireCard.getApplication(0x1120ef).getFile(0x03).getData().bytes();
            long arvoMystery1 = bitsToLong(0, 9, data);
            long arvoDiscoGroup = bitsToLong(9, 5, data);
            long arvoDuration = bitsToLong(14, 13, data);
            long arvoRegional = bitsToLong(27, 5, data);

            long arvoExit = cardDateToTimestamp(bitsToLong(32, 14, data), bitsToLong(46, 11, data));
            long arvoPurchasePrice = bitsToLong(68, 14, data);
            //mArvoDiscoGroup = bitsToLong(82, 6,data);

            //68 price, 82 zone?
            long arvoPurchase = cardDateToTimestamp(bitsToLong(88, 14, data), bitsToLong(102, 11, data));

            //68 price, 82 zone?
            long arvoExpire = cardDateToTimestamp(bitsToLong(113, 14, data), bitsToLong(127, 11, data));

            long arvoPax = bitsToLong(138, 6, data);

            //68 price, 82 zone?
            long arvoXfer = cardDateToTimestamp(bitsToLong(144, 14, data), bitsToLong(158, 11, data));

            long arvoVehicleNumber = bitsToLong(169, 14, data);

            long arvoUnknown = bitsToLong(183, 2, data);

            long arvoLineJORE = bitsToLong(185, 14, data);
            long arvoJOREExt = bitsToLong(199, 4, data);
            long arvoDirection = bitsToLong(203, 1, data);

            if (balanceIndex > -1) {
                trips.set(balanceIndex, HSLTrip.builder(trips.get(balanceIndex))
                        .line(Long.toString(arvoLineJORE))
                        .vehicleNumber(arvoVehicleNumber)
                        .build());
            } else if (arvoPurchase > 2) {
                trips.add(HSLTrip.builder()
                        .arvo(1)
                        .expireTimestamp(arvoExpire)
                        .fare(arvoPurchasePrice)
                        .pax(arvoPax)
                        .timestamp(arvoPurchase)
                        .vehicleNumber(arvoVehicleNumber)
                        .line(Long.toString(arvoLineJORE))
                        .build());
                Collections.sort(trips, new Trip.Comparator());
            }

            int seasonIndex = -1;
            for (int i = 0; i < trips.size(); ++i) {
                if (trips.get(i).getArvo() == 0) {
                    seasonIndex = i;
                    break;
                }
            }

            data = desfireCard.getApplication(0x1120ef).getFile(0x01).getData().bytes();

            boolean kausiNoData = false;
            if (bitsToLong(19, 14, data) == 0 && bitsToLong(67, 14, data) == 0) {
                kausiNoData = true;
            }

            long kausiStart = cardDateToTimestamp(bitsToLong(19, 14, data), 0);
            long kausiEnd = cardDateToTimestamp(bitsToLong(33, 14, data), 0);
            long kausiPrevStart = cardDateToTimestamp(bitsToLong(67, 14, data), 0);
            long kausiPrevEnd = cardDateToTimestamp(bitsToLong(81, 14, data), 0);
            if (kausiPrevStart > kausiStart) {
                final long temp = kausiStart;
                final long temp2 = kausiEnd;
                kausiStart = kausiPrevStart;
                kausiEnd = kausiPrevEnd;
                kausiPrevStart = temp;
                kausiPrevEnd = temp2;
            }
            boolean hasKausi = kausiEnd > (System.currentTimeMillis() / 1000.0);
            long kausiPurchase = cardDateToTimestamp(bitsToLong(110, 14, data), bitsToLong(124, 11, data));
            long kausiPurchasePrice = bitsToLong(149, 15, data);
            long kausiLastUse = cardDateToTimestamp(bitsToLong(192, 14, data), bitsToLong(206, 11, data));
            long kausiVehicleNumber = bitsToLong(217, 14, data);
            //mTrips[0].mVehicleNumber = mArvoVehicleNumber;

            long kausiUnknown = bitsToLong(231, 2, data);

            long kausiLineJORE = bitsToLong(233, 14, data);
            //mTrips[0].mLine = Long.toString(mArvoLineJORE).substring(1);

            long kausiJOREExt = bitsToLong(247, 4, data);
            long kausiDirection = bitsToLong(241, 1, data);
            if (seasonIndex > -1) {
                trips.set(seasonIndex, HSLTrip.builder(trips.get(seasonIndex))
                        .vehicleNumber(kausiVehicleNumber)
                        .line(Long.toString(kausiLineJORE))
                        .build());
            } else if (kausiVehicleNumber > 0) {
                trips.add(HSLTrip.builder()
                        .arvo(0)
                        .expireTimestamp(kausiPurchase)
                        .fare(kausiPurchasePrice)
                        .pax(1)
                        .timestamp(kausiPurchase)
                        .vehicleNumber(kausiVehicleNumber)
                        .line(Long.toString(kausiLineJORE))
                        .build());
                Collections.sort(trips, new Trip.Comparator());
            }

            return new AutoValue_HSLTransitData.Builder()
                    .serialNumber(serialNumber)
                    .trips(ImmutableList.<Trip>copyOf(trips))
                    .refills(Collections.<Refill>singletonList(lastRefill))
                    .balance(balance)
                    .hasKausi(hasKausi)
                    .kausiStart(kausiStart)
                    .kausiEnd(kausiEnd)
                    .kausiPrevStart(kausiPrevStart)
                    .kausiPrevEnd(kausiPrevEnd)
                    .kausiPurchasePrice(kausiPurchasePrice)
                    .kausiLastUse(kausiLastUse)
                    .kausiPurchase(kausiPurchase)
                    .kausiNoData(kausiNoData)
                    .arvoExit(arvoExit)
                    .arvoPurchase(arvoPurchase)
                    .arvoExpire(arvoExpire)
                    .arvoPax(arvoPax)
                    .arvoPurchasePrice(arvoPurchasePrice)
                    .arvoXfer(arvoXfer)
                    .arvoDiscoGroup(arvoDiscoGroup)
                    .arvoMystery1(arvoMystery1)
                    .arvoDuration(arvoDuration)
                    .arvoRegional(arvoRegional)
                    .arvoJOREExt(arvoJOREExt)
                    .arvoVehicleNumber(arvoVehicleNumber)
                    .arvoUnknown(arvoUnknown)
                    .arvoLineJORE(arvoLineJORE)
                    .kausiVehicleNumber(kausiVehicleNumber)
                    .kausiUnknown(kausiUnknown)
                    .kausiLineJORE(kausiLineJORE)
                    .kausiJOREExt(kausiJOREExt)
                    .arvoDirection(arvoDirection)
                    .kausiDirection(kausiDirection)
                    .build();
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL data", ex);
        }
    }

    public static boolean check(Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x1120ef) != null);
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        try {
            byte[] data = ((DesfireCard) card).getApplication(0x1120ef).getFile(0x08).getData().bytes();
            return new TransitIdentity("HSL", Utils.getHexString(data).substring(2, 20));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL serial", ex);
        }
    }

    static long bitsToLong(int start, int len, byte[] data) {
        long ret = 0;
        for (int i = start; i < start + len; ++i) {
            long bit = ((data[i / 8] >> (7 - i % 8)) & 1);
            ret = ret | (bit << ((start + len - 1) - i));
        }
        return ret;
    }

    static long bitsToLong(int start, int len, long[] data) {
        long ret = 0;
        for (int i = start; i < start + len; ++i) {
            long bit = ((data[i / 8] >> (7 - i % 8)) & 1);
            ret = ret | (bit << ((start + len - 1) - i));
        }
        return ret;
    }

    static long cardDateToTimestamp(long day, long minute) {
        return (EPOCH) + day * (60 * 60 * 24) + minute * 60;
    }

    @NonNull
    @Override
    public String getCardName() {
        return "HSL";
    }

    @NonNull
    @Override
    public String getBalanceString() {
        FareBotApplication app = FareBotApplication.getInstance();
        String ret = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(getBalance() / 100);
        if (getHasKausi()) {
            ret += "\n" + app.getString(R.string.hsl_pass_is_valid);
        }
        if (getArvoExpire() * 1000.0 > System.currentTimeMillis()) {
            ret += "\n" + app.getString(R.string.hsl_value_ticket_is_valid) + "!";
        }
        return ret;
    }

    /*
    public String getCustomString() {
        DateFormat shortDateTimeFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        DateFormat shortDateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT);

        StringBuilder ret = new StringBuilder();
        if (!mKausiNoData) {
            ret.append(GR(R.string.hsl_season_ticket)).append(":\n");
            ret.append(GR(R.string.hsl_value_ticket_vehicle_number)).append(": ")
            .append(mKausiVehicleNumber).append("\n");
            ret.append(GR(R.string.hsl_value_ticket_line_number)).append(": ")
            .append(Long.toString(mKausiLineJORE).substring(1)).append("\n");
            ret.append("JORE extension").append(": ").append(mKausiJOREExt).append("\n");
            ret.append("Direction").append(": ").append(mKausiDirection).append("\n");

            ret.append(GR(R.string.hsl_season_ticket_starts)).append(": ")
            .append(shortDateFormat.format(mKausiStart * 1000.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_season_ticket_ends)).append(": ")
            .append(shortDateFormat.format(mKausiEnd * 1000.0));
            ret.append("\n\n");
            ret.append(GR(R.string.hsl_season_ticket_bought_on)).append(": ")
            .append(shortDateTimeFormat.format(mKausiPurchase * 1000.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_season_ticket_price_was)).append(": ")
            .append(NumberFormat.getCurrencyInstance(Locale.GERMANY).format(mKausiPurchasePrice / 100.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_you_last_used_this_ticket)).append(": ")
            .append(shortDateTimeFormat.format(mKausiLastUse * 1000.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_previous_season_ticket)).append(": ")
            .append(shortDateFormat.format(mKausiPrevStart * 1000.0));
            ret.append(" - ").append(shortDateFormat.format(mKausiPrevEnd * 1000.0));
            ret.append("\n\n");
        }

        ret.append(GR(R.string.hsl_value_ticket)).append(":\n");
        ret.append(GR(R.string.hsl_value_ticket_bought_on)).append(": ")
        .append(shortDateTimeFormat.format(mArvoPurchase * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_expires_on)).append(": ")
        .append(shortDateTimeFormat.format(mArvoExpire * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_last_transfer)).append(": ")
        .append(shortDateTimeFormat.format(mArvoXfer * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_last_sign)).append(": ")
        .append(shortDateTimeFormat.format(mArvoExit * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_price)).append(": ")
        .append(NumberFormat.getCurrencyInstance(Locale.GERMANY).format(mArvoPurchasePrice / 100.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_disco_group)).append(": ").append(mArvoDiscoGroup).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_pax)).append(": ").append(mArvoPax).append("\n");
        ret.append("Mystery1").append(": ").append(mArvoMystery1).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_duration)).append(": ").append(mArvoDuration).append(" min\n");
        ret.append(GR(R.string.hsl_value_ticket_vehicle_number)).append(": ").append(mArvoVehicleNumber).append("\n");
        ret.append("Region").append(": ").append(regionNames[(int) mArvoRegional]).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_line_number)).append(": ")
        .append(Long.toString(mArvoLineJORE).substring(1)).append("\n");
        ret.append("JORE extension").append(": ").append(mArvoJOREExt).append("\n");
        ret.append("Direction").append(": ").append(mArvoDirection).append("\n");

        return ret.toString();
    }
    */

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        return null;
    }

    @Nullable
    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @NonNull
    private static List<HSLTrip> parseTrips(@NonNull DesfireCard card) {
        DesfireFile file = card.getApplication(0x1120ef).getFile(0x04);
        if (file instanceof RecordDesfireFile) {
            RecordDesfireFile recordFile = (RecordDesfireFile) card.getApplication(0x1120ef).getFile(0x04);
            List<HSLTrip> useLog = new ArrayList<>();
            for (int i = 0; i < recordFile.getRecords().size(); i++) {
                useLog.add(HSLTrip.create(recordFile.getRecords().get(i)));
            }
            Collections.sort(useLog, new Trip.Comparator());
            return useLog;
        }
        return new ArrayList<>();
    }

    abstract double getBalance();

    abstract boolean getHasKausi();

    abstract long getKausiStart();

    abstract long getKausiEnd();

    abstract long getKausiPrevStart();

    abstract long getKausiPrevEnd();

    abstract long getKausiPurchasePrice();

    abstract long getKausiLastUse();

    abstract long getKausiPurchase();

    abstract boolean getKausiNoData();

    abstract long getArvoExit();

    abstract long getArvoPurchase();

    abstract long getArvoExpire();

    abstract long getArvoPax();

    abstract long getArvoPurchasePrice();

    abstract long getArvoXfer();

    abstract long getArvoDiscoGroup();

    abstract long getArvoMystery1();

    abstract long getArvoDuration();

    abstract long getArvoRegional();

    abstract long getArvoJOREExt();

    abstract long getArvoVehicleNumber();

    abstract long getArvoUnknown();

    abstract long getArvoLineJORE();

    abstract long getKausiVehicleNumber();

    abstract long getKausiUnknown();

    abstract long getKausiLineJORE();

    abstract long getKausiJOREExt();

    abstract long getArvoDirection();

    abstract long getKausiDirection();

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder serialNumber(String serialNumber);

        abstract Builder trips(List<Trip> trips);

        abstract Builder refills(List<Refill> refills);

        abstract Builder balance(double balance);

        abstract Builder hasKausi(boolean hasKausi);

        abstract Builder kausiStart(long kausiStart);

        abstract Builder kausiEnd(long kausiEnd);

        abstract Builder kausiPrevStart(long kausiPrevStart);

        abstract Builder kausiPrevEnd(long kausiPrevEnd);

        abstract Builder kausiPurchasePrice(long kausiPurchasePrice);

        abstract Builder kausiLastUse(long kausiLastUse);

        abstract Builder kausiPurchase(long kausiPurchase);

        abstract Builder kausiNoData(boolean kausiNoData);

        abstract Builder arvoExit(long arvoExit);

        abstract Builder arvoPurchase(long arvoPurchase);

        abstract Builder arvoExpire(long arvoExpire);

        abstract Builder arvoPax(long arvoPax);

        abstract Builder arvoPurchasePrice(long arvoPurchasePrice);

        abstract Builder arvoXfer(long arvoXfer);

        abstract Builder arvoDiscoGroup(long arvoDiscoGroup);

        abstract Builder arvoMystery1(long arvoMystery1);

        abstract Builder arvoDuration(long arvoDuration);

        abstract Builder arvoRegional(long arvoRegional);

        abstract Builder arvoJOREExt(long arvoJOREExt);

        abstract Builder arvoVehicleNumber(long arvoVehicleNumber);

        abstract Builder arvoUnknown(long arvoUnknown);

        abstract Builder arvoLineJORE(long arvoLineJORE);

        abstract Builder kausiVehicleNumber(long kausiVehicleNumber);

        abstract Builder kausiUnknown(long kausiUnknown);

        abstract Builder kausiLineJORE(long kausiLineJORE);

        abstract Builder kausiJOREExt(long kausiJOREExt);

        abstract Builder arvoDirection(long arvoDirection);

        abstract Builder kausiDirection(long kausiDirection);

        abstract HSLTransitData build();
    }
}
