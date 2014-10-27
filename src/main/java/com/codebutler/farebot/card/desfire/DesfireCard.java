/*
 * DesfireCard.java
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

package com.codebutler.farebot.card.desfire;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardRawDataFragmentClass;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.fragment.DesfireCardRawDataFragment;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.clipper.ClipperTransitData;
import com.codebutler.farebot.transit.hsl.HSLTransitData;
import com.codebutler.farebot.transit.orca.OrcaTransitData;
import com.codebutler.farebot.util.Utils;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Root(name="card")
@CardRawDataFragmentClass(DesfireCardRawDataFragment.class)
public class DesfireCard extends Card {
    @Element(name="manufacturing-data") private DesfireManufacturingData mManfData;
    @ElementList(name="applications") private List<DesfireApplication> mApplications;

    public static DesfireCard dumpTag(Tag tag) throws Exception {
        List<DesfireApplication> apps = new ArrayList<>();

        IsoDep tech = IsoDep.get(tag);

        tech.connect();

        DesfireManufacturingData manufData;
        DesfireApplication[]     appsArray;
        
        try {
            DesfireProtocol desfireTag = new DesfireProtocol(tech);

            manufData = desfireTag.getManufacturingData();

            for (int appId : desfireTag.getAppList()) {
                desfireTag.selectApp(appId);

                List<DesfireFile> files = new ArrayList<>();

                for (int fileId : desfireTag.getFileList()) {
                    try {
                        DesfireFileSettings settings = desfireTag.getFileSettings(fileId);
                        byte[] data;
                        if (settings instanceof StandardDesfireFileSettings)
                            data = desfireTag.readFile(fileId);
                        else
                            data = desfireTag.readRecord(fileId);
                        files.add(DesfireFile.create(fileId, settings, data));
                    } catch (IOException ex) {
                        throw ex;
                    } catch (Exception ex) {
                        files.add(new InvalidDesfireFile(fileId, ex.toString()));
                    }
                }

                DesfireFile[] filesArray = new DesfireFile[files.size()];
                files.toArray(filesArray);

                apps.add(new DesfireApplication(appId, filesArray));
            }

            appsArray = new DesfireApplication[apps.size()];
            apps.toArray(appsArray);
        } finally {
            if (tech.isConnected())
                tech.close();
        }

        return new DesfireCard(tag.getId(), new Date(), manufData, appsArray);
    }

    private DesfireCard() { /* For XML Serializer */ }

    DesfireCard(byte[] tagId, Date scannedAt, DesfireManufacturingData manfData, DesfireApplication apps[]) {
        super(CardType.MifareDesfire, tagId, scannedAt);
        mManfData = manfData;
        mApplications = Utils.arrayAsList(apps);
    }

    @Override public TransitIdentity parseTransitIdentity() {
        if (OrcaTransitData.check(this))
            return OrcaTransitData.parseTransitIdentity(this);
        if (ClipperTransitData.check(this))
            return ClipperTransitData.parseTransitIdentity(this);
        if (HSLTransitData.check(this))
            return HSLTransitData.parseTransitIdentity(this);
        return null;
    }
    
    @Override public TransitData parseTransitData() {
        if (OrcaTransitData.check(this))
            return new OrcaTransitData(this);
        if (ClipperTransitData.check(this))
            return new ClipperTransitData(this);
        if (HSLTransitData.check(this))
            return new HSLTransitData(this);
        return null;
    }

    public List<DesfireApplication> getApplications() {
        return mApplications;
    }

    public DesfireApplication getApplication (int appId) {
        for (DesfireApplication app : mApplications) {
            if (app.getId() == appId)
                return app;
        }
        return null;
    }

    public DesfireManufacturingData getManufacturingData () {
        return mManfData;
    }
}
