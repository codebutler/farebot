/*
 * HSLTransitInfo.java
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

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.core.ui.ListItem;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@AutoValue
public abstract class HSLTransitInfo extends TransitInfo {

    @NonNull
    static Builder builder() {
        return AutoValue_HSLTransitInfo.builder();
    }

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
    @Override
    public String getCardName(@NonNull Resources resources) {
        return "HSL";
    }

    @NonNull
    @Override
    public String getBalanceString(@NonNull Resources resources) {
        String ret = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(getBalance() / 100);
        if (getHasKausi()) {
            ret += "\n" + resources.getString(R.string.hsl_pass_is_valid);
        }
        if (getArvoExpire() * 1000.0 > System.currentTimeMillis()) {
            ret += "\n" + resources.getString(R.string.hsl_value_ticket_is_valid) + "!";
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
    public List<ListItem> getInfo(@NonNull Context context) {
        return null;
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

        abstract HSLTransitInfo build();
    }
}
