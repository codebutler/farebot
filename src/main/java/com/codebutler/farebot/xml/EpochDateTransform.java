package com.codebutler.farebot.xml;

import org.simpleframework.xml.transform.Transform;

import java.util.Date;

public class EpochDateTransform implements Transform<Date> {
    @Override public Date read(String value) throws Exception {
        return new Date(Long.parseLong(value));
    }

    @Override public String write(Date value) throws Exception {
        return String.valueOf(value.getTime());
    }
}
