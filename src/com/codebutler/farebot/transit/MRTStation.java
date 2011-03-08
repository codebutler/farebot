package com.codebutler.farebot.transit;

public class MRTStation extends Station {
	private final String mCode;
	private final String mAbbreviation;

	public MRTStation (String name, String code, String abbreviation, String latitude, String longitude) {
    	super(name, latitude, longitude);
    	mCode = code;
    	mAbbreviation = abbreviation;
    }
    
    public String getCode() {
    	return mCode;
    }
    
    public String getAbbreviation() {
    	return mAbbreviation;
    }
}
