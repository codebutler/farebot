package com.codebutler.farebot.card.felica;

import android.nfc.Tag;
import android.nfc.tech.TagTechnology;
import android.support.annotation.NonNull;
import android.util.Log;

import com.codebutler.farebot.card.TagReader;
import com.codebutler.farebot.card.felica.raw.RawFelicaCard;
import com.codebutler.farebot.util.Utils;

import net.kazzz.felica.FeliCaTag;
import net.kazzz.felica.command.ReadResponse;
import net.kazzz.felica.lib.FeliCaLib;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FelicaTagReader extends TagReader<FelicaTagReader.FelicaTech, RawFelicaCard> {

    public FelicaTagReader(@NonNull byte[] tagId, @NonNull Tag tag) {
        super(tagId, tag);
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
            @NonNull FelicaTech tech) throws Exception {
        FeliCaTag ft = new FeliCaTag(tag);

        FeliCaLib.IDm idm = ft.pollingAndGetIDm(FeliCaLib.SYSTEMCODE_ANY);
        FeliCaLib.PMm pmm = ft.getPMm();

        if (idm == null) {
            throw new Exception("Failed to read IDm");
        }

        List<FelicaSystem> systems = new ArrayList<>();

        // FIXME: Enumerate "areas" inside of systems ???

        for (FeliCaLib.SystemCode code : ft.getSystemCodeList()) {
            Log.d("FelicaCard", "Got system code: " + Utils.getHexString(code.getBytes()));

            int systemCode = Utils.byteArrayToInt(code.getBytes());
            //ft.polling(systemCode);

            FeliCaLib.IDm thisIdm = ft.pollingAndGetIDm(systemCode);

            Log.d("FelicaCard", " - Got IDm: " + Utils.getHexString(thisIdm.getBytes()) + "  compare: "
                    + Utils.getHexString(idm.getBytes()));

            byte[] foo = idm.getBytes();
            ArrayUtils.reverse(foo);
            Log.d("FelicaCard", " - Got Card ID? " + Utils.byteArrayToInt(idm.getBytes(), 2, 6) + "  "
                    + Utils.byteArrayToInt(foo, 2, 6));

            Log.d("FelicaCard", " - Got PMm: " + Utils.getHexString(ft.getPMm().getBytes()) + "  compare: "
                    + Utils.getHexString(pmm.getBytes()));

            List<FelicaService> services = new ArrayList<>();

            for (FeliCaLib.ServiceCode serviceCode : ft.getServiceCodeList()) {
                // There appears to be some disagreement over byte order.
                byte[] bytes = serviceCode.getBytes();
                ArrayUtils.reverse(bytes);
                int serviceCodeInt = Utils.byteArrayToInt(bytes);
                serviceCode = new FeliCaLib.ServiceCode(serviceCodeInt);

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
                }
            }

            systems.add(FelicaSystem.create(Utils.byteArrayToInt(code.getBytes()), services));
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
