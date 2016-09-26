/*
 * OrcaData.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2015 Sean CyberKitsune McClenaghan <cyberkitsune09@gmail.com>
 *
 * Thanks to:
 * Karl Koscher <supersat@cs.washington.edu>
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

public final class OrcaData {

    private OrcaData() { }

    static final int TRANS_TYPE_TAP_IN = 0x03;
    static final int TRANS_TYPE_TAP_OUT = 0x07;
    static final int TRANS_TYPE_PURSE_USE = 0x0c;
    static final int TRANS_TYPE_CANCEL_TRIP = 0x01;
    static final int TRANS_TYPE_PASS_USE = 0x60;
}
