/*
 * FelicaTagReader.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.felica;

import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.nfc.tech.TagTechnology;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.codebutler.farebot.card.TagReader;
import com.codebutler.farebot.card.felica.raw.RawFelicaCard;
import com.codebutler.farebot.base.util.ArrayUtils;
import com.codebutler.farebot.base.util.ByteUtils;
import com.codebutler.farebot.key.CardKeys;

import net.kazzz.felica.FeliCaTag;
import net.kazzz.felica.command.ReadResponse;
import net.kazzz.felica.lib.FeliCaLib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class FelicaTagReader extends TagReader<FelicaTagReader.FelicaTech, RawFelicaCard, CardKeys> {

    private static final String TAG = "FelicaTagReader";

    public FelicaTagReader(@NonNull byte[] tagId, @NonNull Tag tag) {
        super(tagId, tag, null);
    }

    @NonNull
    @Override
    protected FelicaTech getTech(@NonNull Tag tag) {
        return new FelicaTech(tag);
    }

    // https://github.com/tmurakam/felicalib/blob/master/src/dump/dump.c
    // https://github.com/tmurakam/felica2money/blob/master/src/card/Suica.cs
    @NonNull
    @Override
    protected RawFelicaCard readTag(
            @NonNull byte[] tagId,
            @NonNull Tag tag,
            @NonNull FelicaTech tech,
            @Nullable CardKeys cardKeys) throws Exception {
        NfcF nfcF = NfcF.get(tag);
        Log.d(TAG, "Default system code: " + ByteUtils.getHexString(nfcF.getSystemCode()));

        boolean octopusMagic = false;
        boolean sztMagic = false;

        FeliCaTag ft = new FeliCaTag(tag);

        FeliCaLib.IDm idm = ft.pollingAndGetIDm(FeliCaLib.SYSTEMCODE_ANY);
        FeliCaLib.PMm pmm = ft.getPMm();

        if (idm == null) {
            throw new Exception("Failed to read IDm");
        }

        List<FelicaSystem> systems = new ArrayList<>();

        // FIXME: Enumerate "areas" inside of systems ???
        List<FeliCaLib.SystemCode> codes = Arrays.asList(ft.getSystemCodeList());

        // Check if we failed to get a System Code
        if (codes.size() == 0) {
            // Lets try to ping for an Octopus anyway
            FeliCaLib.IDm octopusSystem = ft.pollingAndGetIDm(FeliCaLib.SYSTEMCODE_OCTOPUS);
            if (octopusSystem != null) {
                Log.d(TAG, "Detected Octopus card");
                // Octopus has a special knocking sequence to allow unprotected reads, and does not
                // respond to the normal system code listing.
                codes.add(new FeliCaLib.SystemCode(FeliCaLib.SYSTEMCODE_OCTOPUS));
                octopusMagic = true;
            }

            FeliCaLib.IDm sztSystem = ft.pollingAndGetIDm(FeliCaLib.SYSTEMCODE_SZT);
            if (sztSystem != null) {
                Log.d(TAG, "Detected Shenzhen Tong card");
                // Because Octopus and SZT are similar systems, use the same knocking sequence in
                // case they have the same bugs with system code listing.
                codes.add(new FeliCaLib.SystemCode(FeliCaLib.SYSTEMCODE_SZT));
                sztMagic = true;
            }
        }

        for (FeliCaLib.SystemCode code : codes) {
            Log.d(TAG, "Got system code: " + ByteUtils.getHexString(code.getBytes()));

            int systemCode = code.getCode();

            FeliCaLib.IDm thisIdm = ft.pollingAndGetIDm(systemCode);

            Log.d(TAG, " - Got IDm: " + ByteUtils.getHexString(thisIdm.getBytes()) + "  compare: "
                    + ByteUtils.getHexString(idm.getBytes()));

            byte[] foo = idm.getBytes();
            ArrayUtils.reverse(foo);
            Log.d(TAG, " - Got Card ID? " + ByteUtils.byteArrayToInt(idm.getBytes(), 2, 6) + "  "
                    + ByteUtils.byteArrayToInt(foo, 2, 6));

            Log.d(TAG, " - Got PMm: " + ByteUtils.getHexString(ft.getPMm().getBytes()) + "  compare: "
                    + ByteUtils.getHexString(pmm.getBytes()));

            List<FelicaService> services = new ArrayList<>();
            FeliCaLib.ServiceCode[] serviceCodes;

            if (octopusMagic && code.getCode() == FeliCaLib.SYSTEMCODE_OCTOPUS) {
                Log.d(TAG, "Stuffing in Octopus magic service code");
                serviceCodes = new FeliCaLib.ServiceCode[]{new FeliCaLib.ServiceCode(FeliCaLib.SERVICE_OCTOPUS)};
            } else if (sztMagic && code.getCode() == FeliCaLib.SYSTEMCODE_SZT) {
                Log.d(TAG, "Stuffing in SZT magic service code");
                serviceCodes = new FeliCaLib.ServiceCode[]{new FeliCaLib.ServiceCode(FeliCaLib.SERVICE_SZT)};
            } else {
                serviceCodes = ft.getServiceCodeList();
            }

            for (FeliCaLib.ServiceCode serviceCode : serviceCodes) {
                byte[] bytes = serviceCode.getBytes();
                ArrayUtils.reverse(bytes);
                int serviceCodeInt = ByteUtils.byteArrayToInt(bytes);
                serviceCode = new FeliCaLib.ServiceCode(serviceCode.getBytes());

                List<FelicaBlock> blocks = new ArrayList<>();

                ft.polling(systemCode);

                byte addr = 0;
                ReadResponse result = ft.readWithoutEncryption(serviceCode, addr);
                while (result != null && result.getStatusFlag1() == 0) {
                    blocks.add(FelicaBlock.create(addr, result.getBlockData()));
                    addr++;
                    result = ft.readWithoutEncryption(serviceCode, addr);
                }

                if (blocks.size() > 0) { // Most service codes appear to be empty...
                    services.add(FelicaService.create(serviceCodeInt, blocks));
                    Log.d(TAG, "- Service code " + serviceCodeInt + " had " + blocks.size() + " blocks");
                }
            }

            systems.add(FelicaSystem.create(code.getCode(), services));
        }

        return RawFelicaCard.create(tagId, new Date(), idm, pmm, systems);
    }

    static class FelicaTech implements TagTechnology {

        @NonNull private final Tag mTag;

        FelicaTech(@NonNull Tag tag) {
            mTag = tag;
        }

        @NonNull
        @Override
        public Tag getTag() {
            return mTag;
        }

        @Override
        public void connect() throws IOException { }

        @Override
        public void close() throws IOException { }

        @Override
        public boolean isConnected() {
            return false;
        }
    }
}
