package com.codebutler.farebot.transit.opal;

import android.support.annotation.IntegerRes;

import com.codebutler.farebot.R;

import java.util.HashMap;
import java.util.Map;

final class OpalData {
    static final int VEHICLE_RAIL = 0x00;
    static final int VEHICLE_FERRY = 0x01; // also Light Rail
    static final int VEHICLE_BUS = 0x02;

    static final int ACTION_NEW_JOURNEY_IN_PROGRESS = 0x01;
    static final int ACTION_MANLY_FERRY_JOURNEY = 0x04;
    static final int ACTION_JOURNEY_COMPLETED_1 = 0x07;
    static final int ACTION_JOURNEY_COMPLETED_2 = 0x08;
    static final int ACTION_TAP_ON_REVERSAL = 0x0b;



    static final Map<Integer, Integer> VEHICLES = new HashMap<Integer, Integer>() {{
        put(VEHICLE_RAIL, R.string.opal_vehicle_rail);
        put(VEHICLE_FERRY, R.string.opal_vehicle_ferry);
        put(VEHICLE_BUS, R.string.opal_vehicle_bus);
    }};

    static final Map<Integer, Integer> ACTIONS = new HashMap<Integer, Integer>() {{
        put(ACTION_NEW_JOURNEY_IN_PROGRESS, R.string.opal_action_new_journey_in_progress);
        put(ACTION_MANLY_FERRY_JOURNEY, R.string.opal_action_manly_ferry_journey);
        put(ACTION_JOURNEY_COMPLETED_1, R.string.opal_action_journey_completed);
        put(ACTION_JOURNEY_COMPLETED_2, R.string.opal_action_journey_completed);
        put(ACTION_TAP_ON_REVERSAL, R.string.opal_action_tap_on_reversal);
    }};
}
