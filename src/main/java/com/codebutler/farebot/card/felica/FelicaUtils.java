package com.codebutler.farebot.card.felica;

import net.kazzz.felica.lib.FeliCaLib;

/**
 * Utilities for working with FeliCa cards.
 *
 */
public final class FelicaUtils {

    private FelicaUtils() { }

    /**
     * Translates the System name to something human readable.
     *
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
