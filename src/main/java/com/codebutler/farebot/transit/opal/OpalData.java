package com.codebutler.farebot.transit.opal;

import android.annotation.SuppressLint;

import com.codebutler.farebot.R;

import java.util.HashMap;
import java.util.Map;

final class OpalData {
    static final int VEHICLE_RAIL = 0x00;
    static final int VEHICLE_FERRY_LR = 0x01; // also Light Rail
    static final int VEHICLE_BUS = 0x02;

    static final int ACTION_NONE = 0x00;
    static final int ACTION_NEW_JOURNEY = 0x01;
    static final int ACTION_TRANSFER_SAME_MODE = 0x02;
    static final int ACTION_TRANSFER_DIFF_MODE = 0x03;
    static final int ACTION_MANLY_NEW_JOURNEY = 0x04;
    static final int ACTION_MANLY_TRANSFER_SAME_MODE = 0x05;
    static final int ACTION_MANLY_TRANSFER_DIFF_MODE = 0x06;
    static final int ACTION_JOURNEY_COMPLETED_DISTANCE = 0x07;
    static final int ACTION_JOURNEY_COMPLETED_FLAT_RATE = 0x08;
    static final int ACTION_JOURNEY_COMPLETED_AUTO_ON = 0x09;
    static final int ACTION_JOURNEY_COMPLETED_AUTO_OFF = 0x0a;
    static final int ACTION_TAP_ON_REVERSAL = 0x0b;



    @SuppressLint("UseSparseArrays")
    static final Map<Integer, Integer> VEHICLES = new HashMap<Integer, Integer>() {{
        put(VEHICLE_RAIL, R.string.opal_vehicle_rail);
        put(VEHICLE_FERRY_LR, R.string.opal_vehicle_ferry_lr);
        put(VEHICLE_BUS, R.string.opal_vehicle_bus);
    }};

    @SuppressLint("UseSparseArrays")
    static final Map<Integer, Integer> ACTIONS = new HashMap<Integer, Integer>() {{
        put(ACTION_NONE, R.string.opal_action_none);
        put(ACTION_NEW_JOURNEY, R.string.opal_action_new_journey);
        put(ACTION_TRANSFER_SAME_MODE, R.string.opal_action_transfer_same_mode);
        put(ACTION_TRANSFER_DIFF_MODE, R.string.opal_action_transfer_diff_mode);
        put(ACTION_MANLY_NEW_JOURNEY, R.string.opal_action_manly_new_journey);
        put(ACTION_MANLY_TRANSFER_SAME_MODE, R.string.opal_action_manly_transfer_same_mode);
        put(ACTION_MANLY_TRANSFER_DIFF_MODE, R.string.opal_action_manly_transfer_diff_mode);
        put(ACTION_JOURNEY_COMPLETED_DISTANCE, R.string.opal_action_journey_completed_distance);
        put(ACTION_JOURNEY_COMPLETED_FLAT_RATE, R.string.opal_action_journey_completed_flat_rate);
        put(ACTION_JOURNEY_COMPLETED_AUTO_OFF, R.string.opal_action_journey_completed_auto_off);
        put(ACTION_JOURNEY_COMPLETED_AUTO_ON, R.string.opal_action_journey_completed_auto_on);
        put(ACTION_TAP_ON_REVERSAL, R.string.opal_action_tap_on_reversal);
    }};
}
