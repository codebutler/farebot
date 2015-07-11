/*
 * Utils.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {
    public static <T> List<T> arrayAsList(T... array) {
        if (array == null) {
            return new ArrayList<>();
        }
        return Arrays.asList(array);
    }

    public static void checkNfcEnabled(final Activity activity, NfcAdapter adapter) {
        if (adapter != null && adapter.isEnabled()) {
            return;
        }
        new AlertDialog.Builder(activity)
            .setTitle(R.string.nfc_off_error)
            .setMessage(R.string.turn_on_nfc)
            .setCancelable(true)
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            })
            .setNeutralButton(R.string.wireless_settings, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    activity.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                }
            })
            .show();
    }

    public static void showError (final Activity activity, Exception ex) {
        Log.e(activity.getClass().getName(), ex.getMessage(), ex);
        new AlertDialog.Builder(activity)
            .setMessage(Utils.getErrorMessage(ex))
            .show();
    }

    public static void showErrorAndFinish (final Activity activity, Exception ex) {
        try {
            Log.e(activity.getClass().getName(), Utils.getErrorMessage(ex));
            ex.printStackTrace();

            new AlertDialog.Builder(activity)
                .setMessage(Utils.getErrorMessage(ex))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        activity.finish();
                    }
                })
                .show();
        } catch (WindowManager.BadTokenException unused) {
            /* Ignore... happens if the activity was destroyed */
        }
    }

    public static String getHexString (byte[] b) {
        String result = "";
        for (int i=0; i < b.length; i++) {
            result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }

    public static String getHexString (byte[] b, String defaultResult) {
        try {
            return getHexString(b);
        } catch (Exception ex) {
            return defaultResult;
        }
    }

    public static byte[] hexStringToByteArray (String s) {
        if ((s.length() % 2) != 0) {
            throw new IllegalArgumentException("Bad input string: " + s);
        }
        
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /*
    public static byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }
    */
    
    public static int byteArrayToInt(byte[] b) {
        return byteArrayToInt(b, 0);
    }
    
    public static int byteArrayToInt(byte[] b, int offset) {
        return byteArrayToInt(b, offset, b.length);
    }
    
    public static int byteArrayToInt(byte[] b, int offset, int length) {
        return (int) byteArrayToLong(b, offset, length);
    }

    public static long byteArrayToLong(byte[] b, int offset, int length) {
        if (b.length < length)
            throw new IllegalArgumentException("length must be less than or equal to b.length");

        long value = 0;
        for (int i = 0; i < length; i++) {
            int shift = (length - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

    public static byte[] byteArraySlice(byte[] b, int offset, int length) {
        byte[] ret = new byte[length];
        System.arraycopy(b, offset, ret, 0, length);
        return ret;
    }

    public static String getErrorMessage (Throwable ex) {
        if (ex.getCause() != null) {
            ex = ex.getCause();
        }
        String errorMessage = ex.getLocalizedMessage();
        if (TextUtils.isEmpty(errorMessage)) {
            errorMessage = ex.getMessage();
        }
        if (TextUtils.isEmpty(errorMessage)) {
            errorMessage = ex.toString();
        }
        return errorMessage;
    }

    public static String getDeviceInfoString() {
        return String.format("Version: %s\nModel: %s (%s %s)\nOS: %s\n\n",
            getVersionString(),
            Build.MODEL,
            Build.MANUFACTURER,
            Build.BRAND,
            Build.VERSION.RELEASE);
    }

    private static String getVersionString() {
        PackageInfo info = getPackageInfo();
        return String.format("%s (Build %s)", info.versionName, info.versionCode);
    }

    private static PackageInfo getPackageInfo() {
        try {
            FareBotApplication app = FareBotApplication.getInstance();
            return app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T findInList(List<T> list, Matcher matcher) {
        for (T item : list) {
            if (matcher.matches(item)) {
                return item;
            }
        }
        return null;
    }

    public static interface Matcher<T> {
        public boolean matches(T t);
    }

    public static int convertBCDtoInteger(byte data) {
        return (((data & (char)0xF0) >> 4) * 10) + ((data & (char)0x0F));
    }

    public static int getBitsFromInteger(int buffer, int iStartBit, int iLength) {
        return (buffer >> (iStartBit)) & ((char)0xFF >> (8 - iLength));
    }

    /**
     * Reverses a byte array, such that the last byte is first, and the first byte is last.
     *
     * @param buffer Source buffer to reverse
     * @param iStartByte Start position in the buffer to read from
     * @param iLength Number of bytes to read
     * @return A new byte array, of length iLength, with the bytes reversed
     */
    public static byte[] reverseBuffer(byte[] buffer, int iStartByte, int iLength) {
        byte[] reversed = new byte[iLength];
        int iEndByte = iStartByte + iLength;
        for (int x=0; x<iLength; x++) {
            reversed[x] = buffer[iEndByte - x - 1];
        }
        return reversed;
    }

    /**
     * Given an unsigned integer value, calculate the two's complement of the value if it is
     * actually a negative value
     * @param input Input value to convert
     * @param highestBit The position of the highest bit in the number, 0-indexed.
     * @return A signed integer containing it's converted value.
     */
    public static int unsignedToTwoComplement(int input, int highestBit) {
        if (getBitsFromInteger(input, highestBit, 1) == 1) {
            // inverse all bits
            input ^= (2 << highestBit) - 1;
            return -(1 + input);
        }

        return input;
    }

    /* Based on function from mfocGUI by 'Huuf' (http://www.huuf.info/OV/) */
    public static int getBitsFromBuffer(byte[] buffer, int iStartBit, int iLength) {
        // Note: Assumes big-endian
        int iEndBit = iStartBit + iLength - 1;
        int iSByte = iStartBit / 8;
        int iSBit = iStartBit % 8;
        int iEByte = iEndBit / 8;
        int iEBit = iEndBit % 8;

        if (iSByte == iEByte) {
            return ((char)buffer[iEByte] >> (7 - iEBit)) & ((char)0xFF >> (8 - iLength));
        } else {
            int uRet = (((char)buffer[iSByte] & (char)((char)0xFF >> iSBit)) << (((iEByte - iSByte - 1) * 8) + (iEBit + 1)));

            for (int i = iSByte + 1; i < iEByte; i++) {
                uRet |= (((char)buffer[i] & (char)0xFF) << (((iEByte - i - 1) * 8) + (iEBit + 1)));
            }

            uRet |= (((char)buffer[iEByte] & (char)0xFF)) >> (7 - iEBit);

            return uRet;
        }
    }

    /**
     * Given a string resource (R.string), localize the string according to the language preferences
     * on the device.
     * @param stringResource R.string to localize.
     * @param formatArgs Formatting arguments to pass
     * @return Localized string
     */
    public static String localizeString(int stringResource, Object... formatArgs) {
        Resources res = FareBotApplication.getInstance().getResources();
        return res.getString(stringResource, formatArgs);
    }

}
