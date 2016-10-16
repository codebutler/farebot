/*
 * Utils.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2016 Eric Butler <eric@codebutler.com>
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
import android.text.format.DateFormat;
import android.util.Log;
import android.view.WindowManager;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

public class Utils {

    private static final String TAG = "Utils";

    private Utils() { }

    public static void checkNfcEnabled(final Activity activity, NfcAdapter adapter) {
        if (adapter != null && adapter.isEnabled()) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.nfc_off_error)
                .setMessage(R.string.turn_on_nfc)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                })
                .setNeutralButton(R.string.wireless_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        activity.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    }
                })
                .show();
    }

    public static void showError(final Activity activity, Exception ex) {
        Log.e(activity.getClass().getName(), ex.getMessage(), ex);
        new AlertDialog.Builder(activity)
                .setMessage(Utils.getErrorMessage(ex))
                .show();
    }

    public static void showErrorAndFinish(final Activity activity, Exception ex) {
        try {
            Log.e(activity.getClass().getName(), Utils.getErrorMessage(ex));
            ex.printStackTrace();

            new AlertDialog.Builder(activity)
                    .setMessage(Utils.getErrorMessage(ex))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            activity.finish();
                        }
                    })
                    .show();
        } catch (WindowManager.BadTokenException unused) {
            /* Ignore... happens if the activity was destroyed */
        }
    }

    public static String getHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static String getHexString(byte[] b, String defaultResult) {
        try {
            return getHexString(b);
        } catch (Exception ex) {
            return defaultResult;
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        if ((s.length() % 2) != 0) {
            throw new IllegalArgumentException("Bad input string: " + s);
        }

        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
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

    private static int byteArrayToInt(byte[] b, int offset) {
        return byteArrayToInt(b, offset, b.length);
    }

    public static int byteArrayToInt(byte[] b, int offset, int length) {
        return (int) byteArrayToLong(b, offset, length);
    }

    public static long byteArrayToLong(byte[] b, int offset, int length) {
        if (b.length < offset + length) {
            throw new IllegalArgumentException("offset + length must be less than or equal to b.length");
        }

        long value = 0;
        for (int i = 0; i < length; i++) {
            int shift = (length - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

    public static BigInteger byteArrayToBigInteger(byte[] b, int offset, int length) {
        if (b.length < offset + length) {
            throw new IllegalArgumentException("offset + length must be less than or equal to b.length");
        }

        BigInteger value = BigInteger.valueOf(0);
        for (int i = 0; i < length; i++) {
            value = value.shiftLeft(8);
            value = value.add(BigInteger.valueOf(b[i + offset] & 0x000000ff));
        }
        return value;
    }

    public static byte[] byteArraySlice(byte[] b, int offset, int length) {
        byte[] ret = new byte[length];
        System.arraycopy(b, offset, ret, 0, length);
        return ret;
    }

    public static String getErrorMessage(Throwable ex) {
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
        FareBotApplication app = FareBotApplication.getInstance();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(app);
        boolean nfcAvailable = nfcAdapter != null;
        boolean nfcEnabled = false;
        if (nfcAvailable) {
            nfcEnabled = nfcAdapter.isEnabled();
        }

        return String.format("Version: %s\nModel: %s (%s)\nManufacturer: %s (%s)\nAndroid OS: %s (%s)\n\n"
                + "NFC: %s, Mifare Classic: %s\n\n",
                // Version:
                getVersionString(),
                // Model
                Build.MODEL,
                Build.DEVICE,
                // Manufacturer / brand:
                Build.MANUFACTURER,
                Build.BRAND,
                // OS:
                Build.VERSION.RELEASE,
                Build.ID,
                // NFC:
                nfcAvailable ? (nfcEnabled ? "enabled" : "disabled") : "not available",
                app.getMifareClassicSupport() ? "supported" : "not supported"
        );
    }

    private static String getVersionString() {
        PackageInfo info = getPackageInfo();
        return String.format("%s (%s)", info.versionName, info.versionCode);
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

    public static int convertBCDtoInteger(byte data) {
        return (((data & (char) 0xF0) >> 4) * 10) + ((data & (char) 0x0F));
    }

    public static int getBitsFromInteger(int buffer, int iStartBit, int iLength) {
        return (buffer >> (iStartBit)) & ((char) 0xFF >> (8 - iLength));
    }

    /**
     * Reverses a byte array, such that the last byte is first, and the first byte is last.
     *
     * @param buffer     Source buffer to reverse
     * @param iStartByte Start position in the buffer to read from
     * @param iLength    Number of bytes to read
     * @return A new byte array, of length iLength, with the bytes reversed
     */
    public static byte[] reverseBuffer(byte[] buffer, int iStartByte, int iLength) {
        byte[] reversed = new byte[iLength];
        int iEndByte = iStartByte + iLength;
        for (int x = 0; x < iLength; x++) {
            reversed[x] = buffer[iEndByte - x - 1];
        }
        return reversed;
    }

    /**
     * Given an unsigned integer value, calculate the two's complement of the value if it is
     * actually a negative value
     *
     * @param input      Input value to convert
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
            return ((char) buffer[iEByte] >> (7 - iEBit)) & ((char) 0xFF >> (8 - iLength));
        } else {
            int uRet = (((char) buffer[iSByte] & (char) ((char) 0xFF >> iSBit)) << (((iEByte - iSByte - 1) * 8)
                    + (iEBit + 1)));

            for (int i = iSByte + 1; i < iEByte; i++) {
                uRet |= (((char) buffer[i] & (char) 0xFF) << (((iEByte - i - 1) * 8) + (iEBit + 1)));
            }

            uRet |= (((char) buffer[iEByte] & (char) 0xFF)) >> (7 - iEBit);

            return uRet;
        }
    }

    /**
     * Given a string resource (R.string), localize the string according to the language preferences
     * on the device.
     *
     * @param stringResource R.string to localize.
     * @param formatArgs     Formatting arguments to pass
     * @return Localized string
     */
    public static String localizeString(int stringResource, Object... formatArgs) {
        Resources res = FareBotApplication.getInstance().getResources();
        return res.getString(stringResource, formatArgs);
    }

    /**
     * Given a plural resource (R.plurals), localize the string according to the language preferences
     * on the device.
     *
     * @param pluralResource R.plurals to localize.
     * @param quantity       Quantity to use for pluaralisation rules
     * @param formatArgs     Formatting arguments to pass
     * @return Localized string
     */
    public static String localizePlural(int pluralResource, int quantity, Object... formatArgs) {
        Resources res = FareBotApplication.getInstance().getResources();
        return res.getQuantityString(pluralResource, quantity, formatArgs);
    }

    public static String longDateFormat(Date date) {
        return DateFormat.getLongDateFormat(FareBotApplication.getInstance()).format(date);
    }

    public static String longDateFormat(long milliseconds) {
        return longDateFormat(new Date(milliseconds));
    }

    public static String dateFormat(Date date) {
        return DateFormat.getDateFormat(FareBotApplication.getInstance()).format(date);
    }

    public static String dateFormat(long milliseconds) {
        return dateFormat(new Date(milliseconds));
    }

    public static String timeFormat(Date date) {
        return DateFormat.getTimeFormat(FareBotApplication.getInstance()).format(date);
    }

    public static String timeFormat(long milliseconds) {
        return timeFormat(new Date(milliseconds));
    }

    public static String dateTimeFormat(Date date) {
        return dateFormat(date) + " " + timeFormat(date);
    }

    private static int[] digitsOf(int integer) {
        return digitsOf((long) integer);
    }

    private static int[] digitsOf(long integer) {
        return digitsOf(String.valueOf(integer));
    }

    private static int[] digitsOf(String integer) {
        int[] out = new int[integer.length()];
        for (int index = 0; index < integer.length(); index++) {
            out[index] = Integer.valueOf(integer.substring(index, index + 1));
        }

        return out;
    }

    /**
     * Sum an array of integers.
     *
     * @param ints Input array of integers.
     * @return All the values added together.
     */
    private static int sum(int[] ints) {
        int sum = 0;
        for (int i : ints) {
            sum += i;
        }
        return sum;
    }

    private static int luhnChecksum(String cardNumber) {
        int[] digits = digitsOf(cardNumber);
        // even digits, counting from the last digit on the card
        int[] evenDigits = new int[(int) Math.ceil(cardNumber.length() / 2.0)];
        int checksum = 0;
        int p = 0;
        int q = cardNumber.length() - 1;

        for (int i = 0; i < cardNumber.length(); i++) {
            if (i % 2 == 1) {
                // we treat it as a 1-indexed array
                // so the first digit is odd
                evenDigits[p++] = digits[q - i];
            } else {
                checksum += digits[q - i];
            }
        }

        for (int d : evenDigits) {
            checksum += sum(digitsOf(d * 2));
        }

        Log.d(TAG, String.format("luhnChecksum(%s) = %d", cardNumber, checksum));
        return checksum % 10;
    }

    /**
     * Given a partial card number, calculate the Luhn check digit.
     *
     * @param partialCardNumber Partial card number.
     * @return Final digit for card number.
     */
    public static int calculateLuhn(String partialCardNumber) {
        int checkDigit = luhnChecksum(partialCardNumber + "0");
        return checkDigit == 0 ? 0 : 10 - checkDigit;
    }

    /**
     * Given a complete card number, validate the Luhn check digit.
     *
     * @param cardNumber Complete card number.
     * @return true if valid, false if invalid.
     */
    public static boolean validateLuhn(String cardNumber) {
        return luhnChecksum(cardNumber) == 0;
    }

    public interface Matcher<T> {
        boolean matches(T t);
    }
}
