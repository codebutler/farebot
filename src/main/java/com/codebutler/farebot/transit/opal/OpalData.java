/*
 * OpalData.java
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

package com.codebutler.farebot.transit.opal;

import com.codebutler.farebot.R;
import com.codebutler.farebot.util.ImmutableMapBuilder;

import java.util.Map;

final class OpalData {

    static final Map<Integer, Integer> VEHICLES;
    static final Map<Integer, Integer> ACTIONS;

    private static final int VEHICLE_RAIL = 0x00;
    private static final int VEHICLE_FERRY_LR = 0x01; // also Light Rail
    private static final int VEHICLE_BUS = 0x02;

    private static final int ACTION_NONE = 0x00;
    private static final int ACTION_NEW_JOURNEY = 0x01;
    private static final int ACTION_TRANSFER_SAME_MODE = 0x02;
    private static final int ACTION_TRANSFER_DIFF_MODE = 0x03;
    private static final int ACTION_MANLY_NEW_JOURNEY = 0x04;
    private static final int ACTION_MANLY_TRANSFER_SAME_MODE = 0x05;
    private static final int ACTION_MANLY_TRANSFER_DIFF_MODE = 0x06;
    private static final int ACTION_JOURNEY_COMPLETED_DISTANCE = 0x07;
    private static final int ACTION_JOURNEY_COMPLETED_FLAT_RATE = 0x08;
    private static final int ACTION_JOURNEY_COMPLETED_AUTO_ON = 0x09;
    private static final int ACTION_JOURNEY_COMPLETED_AUTO_OFF = 0x0a;
    private static final int ACTION_TAP_ON_REVERSAL = 0x0b;

    static {
        VEHICLES = new ImmutableMapBuilder<Integer, Integer>()
                .put(VEHICLE_RAIL, R.string.opal_vehicle_rail)
                .put(VEHICLE_FERRY_LR, R.string.opal_vehicle_ferry_lr)
                .put(VEHICLE_BUS, R.string.opal_vehicle_bus)
                .build();

        ACTIONS = new ImmutableMapBuilder<Integer, Integer>()
                .put(ACTION_NONE, R.string.opal_action_none)
                .put(ACTION_NEW_JOURNEY, R.string.opal_action_new_journey)
                .put(ACTION_TRANSFER_SAME_MODE, R.string.opal_action_transfer_same_mode)
                .put(ACTION_TRANSFER_DIFF_MODE, R.string.opal_action_transfer_diff_mode)
                .put(ACTION_MANLY_NEW_JOURNEY, R.string.opal_action_manly_new_journey)
                .put(ACTION_MANLY_TRANSFER_SAME_MODE, R.string.opal_action_manly_transfer_same_mode)
                .put(ACTION_MANLY_TRANSFER_DIFF_MODE, R.string.opal_action_manly_transfer_diff_mode)
                .put(ACTION_JOURNEY_COMPLETED_DISTANCE, R.string.opal_action_journey_completed_distance)
                .put(ACTION_JOURNEY_COMPLETED_FLAT_RATE, R.string.opal_action_journey_completed_flat_rate)
                .put(ACTION_JOURNEY_COMPLETED_AUTO_OFF, R.string.opal_action_journey_completed_auto_off)
                .put(ACTION_JOURNEY_COMPLETED_AUTO_ON, R.string.opal_action_journey_completed_auto_on)
                .put(ACTION_TAP_ON_REVERSAL, R.string.opal_action_tap_on_reversal)
                .build();
    }

    private OpalData() { }
}
