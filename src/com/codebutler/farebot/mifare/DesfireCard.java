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

package com.codebutler.farebot.mifare;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifare.DesfireFile.InvalidDesfireFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public class DesfireCard extends MifareCard
{
    private DesfireManufacturingData mManfData;
    private DesfireApplication[]     mApplications;

    public static DesfireCard dumpTag (byte[] tagId, Object tagObject) throws Exception
    {
        List<DesfireApplication> apps = new ArrayList<DesfireApplication>();

        DesfireProtocol desfireTag = new DesfireProtocol(tagObject);

        DesfireManufacturingData manufData = desfireTag.getManufacturingData();

        for (int appId : desfireTag.getAppList()) {
            desfireTag.selectApp(appId);

            List<DesfireFile> files = new ArrayList<DesfireFile>();

            for (int fileId : desfireTag.getFileList()) {
                try {
                    DesfireFileSettings settings = desfireTag.getFileSettings(fileId);
                    byte[] data = null;
                    if (settings instanceof DesfireFileSettings.StandardDesfireFileSettings)
                        data = desfireTag.readFile(fileId);
                    else
                        data = desfireTag.readRecord(fileId);
                    files.add(new DesfireFile(fileId, settings, data));
                } catch (Exception ex) {
                    files.add(new DesfireFile.InvalidDesfireFile(fileId, ex.toString()));
                }
            }

            DesfireFile[] filesArray = new DesfireFile[files.size()];
            files.toArray(filesArray);
            
            apps.add(new DesfireApplication(appId, filesArray));
        }

        DesfireApplication[] appsArray = new DesfireApplication[apps.size()];
        apps.toArray(appsArray);

        return new DesfireCard(tagId, manufData, appsArray);
    }

    DesfireCard(byte[] tagId, DesfireManufacturingData manfData, DesfireApplication apps[])
    {
        super(tagId);
        mManfData     = manfData;
        mApplications = apps;
    }

    @Override
    public CardType getCardType () {
        return CardType.MifareDesfire;
    }

    public DesfireApplication[] getApplications () {
        return mApplications;
    }

    public DesfireApplication getApplication (int appId)
    {
        for (DesfireApplication app : mApplications) {
            if (app.getId() == appId)
                return app;
        }
        return null;
    }

    public DesfireManufacturingData getManufacturingData () {
        return mManfData;
    }

    public static final Parcelable.Creator<DesfireCard> CREATOR = new Parcelable.Creator<DesfireCard>() {
        public DesfireCard createFromParcel(Parcel source) {
            int tagIdLength = source.readInt();
            byte[] tagId = new byte[tagIdLength];
            source.readByteArray(tagId);

            DesfireManufacturingData manfData = source.readParcelable(DesfireManufacturingData.class.getClassLoader());

            DesfireApplication[] apps = new DesfireApplication[source.readInt()];
            source.readTypedArray(apps, DesfireApplication.CREATOR);

            return new DesfireCard(tagId, manfData, apps);
        }

        public DesfireCard[] newArray (int size) {
            return new DesfireCard[size];
        }
    };

    public void writeToParcel (Parcel parcel, int flags)
    {
        super.writeToParcel(parcel, flags);
        parcel.writeParcelable(mManfData, flags);
        parcel.writeInt(mApplications.length);
        parcel.writeTypedArray(mApplications, flags);
    }
    
    public int describeContents ()
    {
        return 0;
    }

    // FIXME: This is such a mess!
    
    public static DesfireCard fromXml (byte[] cardId, Element element)
    {
        Element appsElement = (Element) element.getElementsByTagName("applications").item(0);

        NodeList appElements = appsElement.getElementsByTagName("application");

        DesfireApplication[] apps = new DesfireApplication[appElements.getLength()];

        for (int x = 0; x < appElements.getLength(); x++) {
            Element appElement = (Element) appElements.item(x);

            int appId = Integer.parseInt(appElement.getAttribute("id"));

            Element filesElement = (Element) appElement.getElementsByTagName("files").item(0);

            NodeList fileElements = filesElement.getElementsByTagName("file");

            DesfireFile[] files = new DesfireFile[fileElements.getLength()];

            for (int y = 0; y < fileElements.getLength(); y++) {
                Element fileElement = (Element) fileElements.item(y);
                int fileId = Integer.parseInt(fileElement.getAttribute("id"));

                DesfireFileSettings fileSettings = null;

                Element settingsElement = (Element) fileElement.getElementsByTagName("settings").item(0);

                Element dataElement = (Element) fileElement.getElementsByTagName("data").item(0);

                if (dataElement != null) {
                    Element e = (Element) settingsElement.getElementsByTagName("filetype").item(0);
                    byte fileType = Byte.parseByte(e.getTextContent());

                    e = (Element) settingsElement.getElementsByTagName("commsetting").item(0);
                    byte commSetting = Byte.parseByte(e.getTextContent());

                    e = (Element) settingsElement.getElementsByTagName("accessrights").item(0);
                    byte[] accessRights = Utils.hexStringToByteArray(e.getTextContent());

                    switch (fileType) {
                        case DesfireFileSettings.STANDARD_DATA_FILE:
                        case DesfireFileSettings.BACKUP_DATA_FILE:
                            e = (Element) settingsElement.getElementsByTagName("filesize").item(0);
                            int fileSize = Integer.parseInt(e.getTextContent());
                            fileSettings = new DesfireFileSettings.StandardDesfireFileSettings(fileType, commSetting, accessRights, fileSize);
                            break;

                        case DesfireFileSettings.LINEAR_RECORD_FILE:
                        case DesfireFileSettings.CYCLIC_RECORD_FILE:
                            e = (Element) settingsElement.getElementsByTagName("recordsize").item(0);
                            int recordSize = Integer.parseInt(e.getTextContent());

                            e = (Element) settingsElement.getElementsByTagName("maxrecords").item(0);
                            int maxRecords = Integer.parseInt(e.getTextContent());

                            e = (Element) settingsElement.getElementsByTagName("currecords").item(0);
                            int curRecords = Integer.parseInt(e.getTextContent());

                            fileSettings = new DesfireFileSettings.RecordDesfireFileSettings(fileType, commSetting, accessRights, recordSize, maxRecords, curRecords);
                            break;

                        default:
                            throw new UnsupportedOperationException("Unknown file type: " + fileType);
                    }

                    byte[] fileData = Base64.decode(dataElement.getTextContent().trim(), Base64.DEFAULT);
                    files[y] = new DesfireFile(fileId, fileSettings, fileData);
                } else {
                    Element errorElement = (Element) fileElement.getElementsByTagName("error").item(0);
                    files[y] = new InvalidDesfireFile(fileId, errorElement.getTextContent());
                }
            }

            apps[x] = new DesfireApplication(appId, files);
        }

        DesfireManufacturingData manfData = DesfireManufacturingData.fromXml((Element)element.getElementsByTagName("manufacturing-data").item(0));
        
        return new DesfireCard(cardId, manfData, apps);
    }

    public Element toXML() throws Exception
    {
        Element root = super.toXML();

        Document doc = root.getOwnerDocument();

        Element appsElement = doc.createElement("applications");

        for (DesfireApplication app : mApplications) {
            Element appElement = doc.createElement("application");
            appElement.setAttribute("id", String.valueOf(app.getId()));

            Element filesElement = doc.createElement("files");
            for (DesfireFile file : app.getFiles()) {
                Element fileElement = doc.createElement("file");
                fileElement.setAttribute("id", String.valueOf(file.getId()));

                if (file instanceof InvalidDesfireFile) {
                    Element fileDataElement = doc.createElement("error");
                    fileDataElement.setTextContent(((InvalidDesfireFile)file).getErrorMessage());
                    fileElement.appendChild(fileDataElement);
                } else {
                    DesfireFileSettings settings = file.getFileSettings();

                    Element fileSettingsElement = doc.createElement("settings");

                    Element element = doc.createElement("filetype");
                    element.setTextContent(String.valueOf(settings.fileType));
                    fileSettingsElement.appendChild(element);

                    element = doc.createElement("commsetting");
                    element.setTextContent(String.valueOf(settings.commSetting));
                    fileSettingsElement.appendChild(element);

                    element = doc.createElement("accessrights");
                    element.setTextContent(Utils.getHexString(settings.accessRights));
                    fileSettingsElement.appendChild(element);

                    if (settings instanceof DesfireFileSettings.StandardDesfireFileSettings) {
                        int fileSize = ((DesfireFileSettings.StandardDesfireFileSettings)settings).fileSize;
                        element = doc.createElement("filesize");
                        element.setTextContent(String.valueOf(fileSize));
                        fileSettingsElement.appendChild(element);
                    } else if (settings instanceof DesfireFileSettings.RecordDesfireFileSettings) {
                        int recordSize = ((DesfireFileSettings.RecordDesfireFileSettings)settings).recordSize;
                        int maxRecords = ((DesfireFileSettings.RecordDesfireFileSettings)settings).maxRecords;
                        int curRecords = ((DesfireFileSettings.RecordDesfireFileSettings)settings).curRecords;

                        element = doc.createElement("recordsize");
                        element.setTextContent(String.valueOf(recordSize));
                        fileSettingsElement.appendChild(element);

                        element = doc.createElement("maxrecords");
                        element.setTextContent(String.valueOf(maxRecords));
                        fileSettingsElement.appendChild(element);

                        element = doc.createElement("currecords");
                        element.setTextContent(String.valueOf(curRecords));
                        fileSettingsElement.appendChild(element);
                    } else {
                        throw new Exception("Unknown file type: " + Integer.toHexString(settings.fileType));
                    }

                    fileElement.appendChild(fileSettingsElement);

                    Element fileDataElement = doc.createElement("data");
                    fileDataElement.setTextContent(Base64.encodeToString(file.getData(), Base64.DEFAULT));
                    fileElement.appendChild(fileDataElement);
                }

                filesElement.appendChild(fileElement);
            }
            appElement.appendChild(filesElement);

            appsElement.appendChild(appElement);
        }
        root.appendChild(appsElement);

        DesfireManufacturingData manfData = getManufacturingData();

        Element manfDataElement = doc.createElement("manufacturing-data");

        Element element = doc.createElement("hw-vendor-id");
        element.setTextContent(Integer.toString(manfData.hwVendorID));
        manfDataElement.appendChild(element);

        element = doc.createElement("hw-type");
        element.setTextContent(Integer.toString(manfData.hwType));
        manfDataElement.appendChild(element);

        element = doc.createElement("hw-sub-type");
        element.setTextContent(Integer.toString(manfData.hwSubType));
        manfDataElement.appendChild(element);

        element = doc.createElement("hw-major-version");
        element.setTextContent(Integer.toString(manfData.hwMajorVersion));
        manfDataElement.appendChild(element);

        element = doc.createElement("hw-minor-version");
        element.setTextContent(Integer.toString(manfData.hwMinorVersion));
        manfDataElement.appendChild(element);

        element = doc.createElement("hw-storage-size");
        element.setTextContent(Integer.toString(manfData.hwStorageSize));
        manfDataElement.appendChild(element);

        element = doc.createElement("hw-protocol");
        element.setTextContent(Integer.toString(manfData.hwProtocol));
        manfDataElement.appendChild(element);

        element = doc.createElement("sw-vendor-id");
        element.setTextContent(Integer.toString(manfData.swVendorID));
        manfDataElement.appendChild(element);

        element = doc.createElement("sw-type");
        element.setTextContent(Integer.toString(manfData.swType));
        manfDataElement.appendChild(element);

        element = doc.createElement("sw-sub-type");
        element.setTextContent(Integer.toString(manfData.swSubType));
        manfDataElement.appendChild(element);

        element = doc.createElement("sw-major-version");
        element.setTextContent(Integer.toString(manfData.swMajorVersion));
        manfDataElement.appendChild(element);

        element = doc.createElement("sw-minor-version");
        element.setTextContent(Integer.toString(manfData.swMinorVersion));
        manfDataElement.appendChild(element);

        element = doc.createElement("sw-storage-size");
        element.setTextContent(Integer.toString(manfData.swStorageSize));
        manfDataElement.appendChild(element);

        element = doc.createElement("sw-protocol");
        element.setTextContent(Integer.toString(manfData.swProtocol));
        manfDataElement.appendChild(element);

        element = doc.createElement("uid");
        element.setTextContent(Integer.toString(manfData.uid));
        manfDataElement.appendChild(element);

        element = doc.createElement("batch-no");
        element.setTextContent(Integer.toString(manfData.batchNo));
        manfDataElement.appendChild(element);

        element = doc.createElement("week-prod");
        element.setTextContent(Integer.toString(manfData.weekProd));
        manfDataElement.appendChild(element);

        element = doc.createElement("year-prod");
        element.setTextContent(Integer.toString(manfData.yearProd));
        manfDataElement.appendChild(element);

        root.appendChild(manfDataElement);

        return root;
    }
}
