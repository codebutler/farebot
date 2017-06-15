/*
 * DesfireCard.java
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

package com.codebutler.farebot.card.desfire;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.base.ui.FareBotUiTree;
import com.codebutler.farebot.base.util.ByteArray;
import com.google.auto.value.AutoValue;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class DesfireCard extends Card {

    @NonNull
    public static DesfireCard create(
            @NonNull ByteArray tagId,
            @NonNull Date scannedAt,
            @NonNull List<DesfireApplication> applications,
            @NonNull DesfireManufacturingData manufacturingData) {
        return new AutoValue_DesfireCard(
                tagId,
                scannedAt,
                applications,
                manufacturingData);
    }

    @NonNull
    public CardType getCardType() {
        return CardType.MifareDesfire;
    }

    @NonNull
    public abstract List<DesfireApplication> getApplications();

    @NonNull
    public abstract DesfireManufacturingData getManufacturingData();

    @Nullable
    public DesfireApplication getApplication(int appId) {
        for (DesfireApplication app : getApplications()) {
            if (app.getId() == appId) {
                return app;
            }
        }
        return null;
    }

    @NonNull
    @Override
    public FareBotUiTree getAdvancedUi(Context context) {
        FareBotUiTree.Builder cardUiBuilder = FareBotUiTree.builder(context);
        FareBotUiTree.Item.Builder appsUiBuilder = cardUiBuilder.item().title("Applications");
        for (DesfireApplication app : getApplications()) {
            FareBotUiTree.Item.Builder appUiBuilder = appsUiBuilder.item()
                    .title(String.format("Application: 0x%s", Integer.toHexString(app.getId())));
            FareBotUiTree.Item.Builder filesUiBuilder = appUiBuilder.item().title("Files");
            for (DesfireFile file : app.getFiles()) {
                FareBotUiTree.Item.Builder fileUiBuilder = filesUiBuilder.item()
                        .title(String.format("File: 0x%s", Integer.toHexString(file.getId())));
                DesfireFileSettings fileSettings = file.getFileSettings();
                FareBotUiTree.Item.Builder settingsUiBuilder = fileUiBuilder.item().title("Settings");
                    settingsUiBuilder.item()
                            .title("Type")
                            .value(fileSettings.getFileTypeName());
                if (fileSettings instanceof StandardDesfireFileSettings) {
                    StandardDesfireFileSettings standardFileSettings = (StandardDesfireFileSettings) fileSettings;
                    settingsUiBuilder.item()
                            .title("Size")
                            .value(standardFileSettings.getFileSize());
                } else if (fileSettings instanceof RecordDesfireFileSettings) {
                    RecordDesfireFileSettings recordFileSettings = (RecordDesfireFileSettings) fileSettings;
                    settingsUiBuilder.item()
                            .title("Cur Records")
                            .value(recordFileSettings.getCurRecords());
                    settingsUiBuilder.item()
                            .title("Max Records")
                            .value(recordFileSettings.getMaxRecords());
                    settingsUiBuilder.item()
                            .title("Record Size")
                            .value(recordFileSettings.getRecordSize());
                } else if (fileSettings instanceof ValueDesfireFileSettings) {
                    ValueDesfireFileSettings valueFileSettings = (ValueDesfireFileSettings) fileSettings;
                    settingsUiBuilder.item()
                            .title("Range")
                            .value(String.format(
                                    "%s - %s",
                                    valueFileSettings.getLowerLimit(),
                                    valueFileSettings.getUpperLimit()));
                    settingsUiBuilder.item()
                            .title("Limited Credit")
                            .value(String.format(
                                    "%s (%s)",
                                    valueFileSettings.getLimitedCreditValue(),
                                    valueFileSettings.getLimitedCreditEnabled() ? "enabled" : "disabled"));
                }
                if (file instanceof StandardDesfireFile) {
                    fileUiBuilder.item()
                            .title("Data")
                            .value(((StandardDesfireFile) file).getData());
                } else if (file instanceof RecordDesfireFile) {
                    FareBotUiTree.Item.Builder recordsUiBuilder = fileUiBuilder.item()
                            .title("Records");
                    List<DesfireRecord> records = ((RecordDesfireFile) file).getRecords();
                    for (int i = 0, recordsSize = records.size(); i < recordsSize; i++) {
                        DesfireRecord record = records.get(i);
                        recordsUiBuilder.item()
                                .title(String.format("Record %s", i))
                                .value(record.getData());
                    }
                } else if (file instanceof ValueDesfireFile) {
                    fileUiBuilder.item()
                            .title("Value")
                            .value(((ValueDesfireFile) file).getValue());
                } else if (file instanceof InvalidDesfireFile) {
                    fileUiBuilder.item()
                            .title("Error")
                            .value(((InvalidDesfireFile) file).getErrorMessage());
                } else if (file instanceof UnauthorizedDesfireFile) {
                    fileUiBuilder.item()
                            .title("Error")
                            .value(((UnauthorizedDesfireFile) file).getErrorMessage());
                }
            }
        }

        DesfireManufacturingData manufacturingData = getManufacturingData();

        FareBotUiTree.Item.Builder manufacturingDataUiBuilder = cardUiBuilder.item().title("Manufacturing Data");

        FareBotUiTree.Item.Builder hwInfoUiBuilder = manufacturingDataUiBuilder.item().title("Hardware Information");
        hwInfoUiBuilder.item().title("Vendor ID").value(manufacturingData.getHwVendorID());
        hwInfoUiBuilder.item().title("Type").value(manufacturingData.getHwType());
        hwInfoUiBuilder.item().title("Subtype").value(manufacturingData.getHwSubType());
        hwInfoUiBuilder.item().title("Major Version").value(manufacturingData.getHwMajorVersion());
        hwInfoUiBuilder.item().title("Minor Version").value(manufacturingData.getHwMinorVersion());
        hwInfoUiBuilder.item().title("Storage Size").value(manufacturingData.getHwStorageSize());
        hwInfoUiBuilder.item().title("Protocol").value(manufacturingData.getHwProtocol());

        FareBotUiTree.Item.Builder swInfoUiBuilder = manufacturingDataUiBuilder.item().title("Software Information");
        swInfoUiBuilder.item().title("Vendor ID").value(manufacturingData.getSwVendorID());
        swInfoUiBuilder.item().title("Type").value(manufacturingData.getSwType());
        swInfoUiBuilder.item().title("Subtype").value(manufacturingData.getSwSubType());
        swInfoUiBuilder.item().title("Major Version").value(manufacturingData.getSwMajorVersion());
        swInfoUiBuilder.item().title("Minor Version").value(manufacturingData.getSwMinorVersion());
        swInfoUiBuilder.item().title("Storage Size").value(manufacturingData.getSwStorageSize());
        swInfoUiBuilder.item().title("Protocol").value(manufacturingData.getSwProtocol());

        FareBotUiTree.Item.Builder generalInfoUiBuilder
                = manufacturingDataUiBuilder.item().title("General Information");
        generalInfoUiBuilder.item().title("Serial Number").value(manufacturingData.getUid());
        generalInfoUiBuilder.item().title("Batch Number").value(manufacturingData.getBatchNo());
        generalInfoUiBuilder.item().title("Week of Production").value(manufacturingData.getWeekProd());
        generalInfoUiBuilder.item().title("Year of Production").value(manufacturingData.getYearProd());

        return cardUiBuilder.build();
    }
}
