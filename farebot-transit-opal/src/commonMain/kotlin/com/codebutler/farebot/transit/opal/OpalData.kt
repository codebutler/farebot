/*
 * OpalData.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.transit.opal

import com.codebutler.farebot.base.util.StringResource
import farebot.farebot_transit_opal.generated.resources.*
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

object OpalData {

    // Opal travel modes
    const val MODE_RAIL = 0x00
    private const val MODE_FERRY_LR = 0x01 // Ferry and Light Rail
    const val MODE_BUS = 0x02

    // Opal actions
    private const val ACTION_NONE = 0x00
    private const val ACTION_NEW_JOURNEY = 0x01
    private const val ACTION_TRANSFER_SAME_MODE = 0x02
    private const val ACTION_TRANSFER_DIFF_MODE = 0x03
    private const val ACTION_MANLY_NEW_JOURNEY = 0x04
    private const val ACTION_MANLY_TRANSFER_SAME_MODE = 0x05
    private const val ACTION_MANLY_TRANSFER_DIFF_MODE = 0x06
    const val ACTION_JOURNEY_COMPLETED_DISTANCE = 0x07
    private const val ACTION_JOURNEY_COMPLETED_FLAT_RATE = 0x08
    private const val ACTION_JOURNEY_COMPLETED_AUTO_ON = 0x09
    private const val ACTION_JOURNEY_COMPLETED_AUTO_OFF = 0x0a
    private const val ACTION_TAP_ON_REVERSAL = 0x0b
    private const val ACTION_TAP_ON_REJECTED = 0x0c

    private val MODES: Map<Int, ComposeStringResource> = mapOf(
        MODE_RAIL to Res.string.opal_vehicle_rail,
        MODE_FERRY_LR to Res.string.opal_vehicle_ferry_lr,
        MODE_BUS to Res.string.opal_vehicle_bus
    )

    private val ACTIONS: Map<Int, ComposeStringResource> = mapOf(
        ACTION_NONE to Res.string.opal_action_none,
        ACTION_NEW_JOURNEY to Res.string.opal_action_new_journey,
        ACTION_TRANSFER_SAME_MODE to Res.string.opal_action_transfer_same_mode,
        ACTION_TRANSFER_DIFF_MODE to Res.string.opal_action_transfer_diff_mode,
        ACTION_MANLY_NEW_JOURNEY to Res.string.opal_action_manly_new_journey,
        ACTION_MANLY_TRANSFER_SAME_MODE to Res.string.opal_action_manly_transfer_same_mode,
        ACTION_MANLY_TRANSFER_DIFF_MODE to Res.string.opal_action_manly_transfer_diff_mode,
        ACTION_JOURNEY_COMPLETED_DISTANCE to Res.string.opal_action_journey_completed_distance,
        ACTION_JOURNEY_COMPLETED_FLAT_RATE to Res.string.opal_action_journey_completed_flat_rate,
        ACTION_JOURNEY_COMPLETED_AUTO_OFF to Res.string.opal_action_journey_completed_auto_off,
        ACTION_JOURNEY_COMPLETED_AUTO_ON to Res.string.opal_action_journey_completed_auto_on,
        ACTION_TAP_ON_REVERSAL to Res.string.opal_action_tap_on_reversal,
        ACTION_TAP_ON_REJECTED to Res.string.opal_action_tap_on_rejected
    )

    fun getLocalisedMode(stringResource: StringResource, mode: Int): String {
        MODES[mode]?.let { return stringResource.getString(it) }
        return stringResource.getString(Res.string.opal_unknown_format, "0x${mode.toString(16)}")
    }

    fun getLocalisedAction(stringResource: StringResource, action: Int): String {
        ACTIONS[action]?.let { return stringResource.getString(it) }
        return stringResource.getString(Res.string.opal_unknown_format, "0x${action.toString(16)}")
    }
}
