/*
 * DesfireTagReader.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.TagReader;
import com.codebutler.farebot.card.desfire.raw.RawDesfireApplication;
import com.codebutler.farebot.card.desfire.raw.RawDesfireCard;
import com.codebutler.farebot.card.desfire.raw.RawDesfireFile;
import com.codebutler.farebot.card.desfire.raw.RawDesfireFileSettings;
import com.codebutler.farebot.card.desfire.raw.RawDesfireManufacturingData;

import java.io.IOException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.codebutler.farebot.card.desfire.DesfireFileSettings.BACKUP_DATA_FILE;
import static com.codebutler.farebot.card.desfire.DesfireFileSettings.CYCLIC_RECORD_FILE;
import static com.codebutler.farebot.card.desfire.DesfireFileSettings.LINEAR_RECORD_FILE;
import static com.codebutler.farebot.card.desfire.DesfireFileSettings.STANDARD_DATA_FILE;
import static com.codebutler.farebot.card.desfire.DesfireFileSettings.VALUE_FILE;

public class DesfireTagReader extends TagReader<IsoDep, RawDesfireCard> {

    public DesfireTagReader(@NonNull byte[] tagId, @NonNull Tag tag) {
        super(tagId, tag);
    }

    @NonNull
    @Override
    protected IsoDep getTech(@NonNull Tag tag) {
        return IsoDep.get(tag);
    }

    @Override
    @NonNull
    protected RawDesfireCard readTag(@NonNull byte[] tagId, @NonNull Tag tag, @NonNull IsoDep tech) throws Exception {
        DesfireProtocol desfireProtocol = new DesfireProtocol(tech);
        List<RawDesfireApplication> apps = readApplications(desfireProtocol);
        RawDesfireManufacturingData manufData = desfireProtocol.getManufacturingData();
        return RawDesfireCard.create(tagId, new Date(), apps, manufData);
    }

    @NonNull
    private List<RawDesfireApplication> readApplications(@NonNull DesfireProtocol desfireProtocol) throws Exception {
        List<RawDesfireApplication> apps = new ArrayList<>();
        for (int appId : desfireProtocol.getAppList()) {
            desfireProtocol.selectApp(appId);
            apps.add(RawDesfireApplication.create(appId, readFiles(desfireProtocol)));
        }
        return apps;
    }

    @NonNull
    private List<RawDesfireFile> readFiles(@NonNull DesfireProtocol desfireProtocol) throws Exception {
        List<RawDesfireFile> files = new ArrayList<>();
        for (int fileId : desfireProtocol.getFileList()) {
            RawDesfireFileSettings settings = desfireProtocol.getFileSettings(fileId);
            files.add(readFile(desfireProtocol, fileId, settings));
        }
        return files;
    }

    @NonNull
    private RawDesfireFile readFile(
            @NonNull DesfireProtocol desfireProtocol,
            int fileId,
            @NonNull RawDesfireFileSettings fileSettings) throws Exception {
        try {
            byte[] fileData = readFileData(desfireProtocol, fileId, fileSettings);
            return RawDesfireFile.create(fileId, fileSettings, fileData);
        } catch (AccessControlException ex) {
            return RawDesfireFile.createUnauthorized(fileId, fileSettings, ex.getMessage());
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            return RawDesfireFile.createInvalid(fileId, fileSettings, ex.toString());
        }
    }

    @NonNull
    private byte[] readFileData(
            @NonNull DesfireProtocol desfireProtocol,
            int fileId,
            @NonNull RawDesfireFileSettings settings) throws Exception {
        switch (settings.fileType()) {
            case STANDARD_DATA_FILE:
            case BACKUP_DATA_FILE:
                return desfireProtocol.readFile(fileId);
            case VALUE_FILE:
                return desfireProtocol.getValue(fileId);
            case CYCLIC_RECORD_FILE:
            case LINEAR_RECORD_FILE:
                return desfireProtocol.readRecord(fileId);
        }
        throw new Exception("Unknown file type");
    }
}
