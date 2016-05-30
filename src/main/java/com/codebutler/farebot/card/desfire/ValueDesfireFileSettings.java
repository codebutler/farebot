package com.codebutler.farebot.card.desfire;

import com.codebutler.farebot.util.Utils;

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.io.ByteArrayInputStream;

/**
 * Contains FileSettings for Value file types.
 * See GetFileSettings for schemadata.
 */
@Root(name="settings")
public class ValueDesfireFileSettings extends DesfireFileSettings {
    @Element(name="min") private int mLowerLimit;
    @Element(name="max") private int mUpperLimit;
    @Element(name="limitcredit") private int mLimitedCreditValue;
    @Element(name="limitcreditenabled") private boolean mLimitedCreditEnabled;

    private ValueDesfireFileSettings() { /* For XML Serializer */ }

    public ValueDesfireFileSettings(
            byte fileType,
            byte commSetting,
            byte[] accessRights,
            int lowerLimit,
            int upperLimit,
            int limitedCreditValue,
            boolean limitedCreditEnabled) {
        super(fileType, commSetting, accessRights);

        this.mLowerLimit = lowerLimit;
        this.mUpperLimit = upperLimit;
        this.mLimitedCreditValue = limitedCreditValue;
        this.mLimitedCreditEnabled = limitedCreditEnabled;
    }

    public ValueDesfireFileSettings(ByteArrayInputStream stream) {
        super(stream);

        byte[] buf = new byte[4];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        mLowerLimit = Utils.byteArrayToInt(buf);

        buf = new byte[4];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        mUpperLimit = Utils.byteArrayToInt(buf);

        buf = new byte[4];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        mLimitedCreditValue = Utils.byteArrayToInt(buf);

        buf = new byte[1];
        stream.read(buf, 0, buf.length);
        mLimitedCreditEnabled = buf[0] != 0x00;
    }

    public int getLowerLimit() {
        return mLowerLimit;
    }

    public int getUpperLimit() {
        return mUpperLimit;
    }

    public int getLimitedCreditValue() {
        return mLimitedCreditValue;
    }

    public boolean getLimitedCreditEnabled() {
        return mLimitedCreditEnabled;
    }
}
