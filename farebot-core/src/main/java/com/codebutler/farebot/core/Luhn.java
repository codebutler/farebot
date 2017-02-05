package com.codebutler.farebot.core;

public final class Luhn {

    private Luhn() { }

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

        return checksum % 10;
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
}
