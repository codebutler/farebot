package com.codebutler.farebot.card;

public enum CardType {
    MifareClassic(0),
    MifareUltralight(1),
    MifareDesfire(2),
    CEPAS(3),
    FeliCa(4),
    Unknown(65535);

    public static CardType parseValue(String value) {
        return CardType.class.getEnumConstants()[Integer.parseInt(value)];
    }

    private int mValue;

    CardType(int value) {
        mValue = value;
    }

    public int toInteger() {
        return mValue;
    }

    public String toString() {
        switch (mValue) {
            case 0:
                return "MIFARE Classic";
            case 1:
                return "MIFARE Ultralight";
            case 2:
                return "MIFARE DESFire";
            case 3:
                return "CEPAS";
            case 4:
                return "FeliCa";
            case 65535:
            default:
                return "Unknown";
        }
    }
}
