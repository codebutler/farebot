/*
 * FelicaUtils.java
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

package com.codebutler.farebot.card.felica;

import net.kazzz.felica.lib.FeliCaLib;

/**
 * Utilities for working with FeliCa cards.
 */
public final class FelicaUtils {

    private FelicaUtils() {
    }

    /**
     * Translates the System name to something human readable.
     * <p>
     * Systems in FeliCa are like Applications in MIFARE.  They represent
     * a particular system operator's data.
     *
     * @param systemCode FeliCa system code to translate.
     * @return English string describing the operator of that System.
     */
    public static String getFriendlySystemName(int systemCode) {
        switch (systemCode) {
            case FeliCaLib.SYSTEMCODE_SUICA:
                return "Suica";
            case FeliCaLib.SYSTEMCODE_EDY:
                return "Common / Edy";
            case FeliCaLib.SYSTEMCODE_FELICA_LITE:
                return "FeliCa Lite";
            default:
                return "Unknown";
        }
    }

    public static String getFriendlyServiceName(int systemCode, int serviceCode) {
        switch (systemCode) {
            case FeliCaLib.SYSTEMCODE_SUICA:
                switch (serviceCode) {
                    case FeliCaLib.SERVICE_SUICA_HISTORY:
                        return "Suica History";
                    case FeliCaLib.SERVICE_SUICA_INOUT:
                        return "Suica In/Out";
                }
                break;

            case FeliCaLib.SYSTEMCODE_FELICA_LITE:
                switch (serviceCode) {
                    case FeliCaLib.SERVICE_FELICA_LITE_READONLY:
                        return "FeliCa Lite Read-only";
                    case FeliCaLib.SERVICE_FELICA_LITE_READWRITE:
                        return "Felica Lite Read-write";
                }
        }

        return "Unknown";
    }
}
