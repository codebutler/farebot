/*
 * FelicaCard.java
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

package com.codebutler.farebot.card.felica;

import android.nfc.Tag;
import android.os.Parcel;
import android.util.Base64;
import android.util.Log;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.transit.SuicaTransitData;
import com.codebutler.farebot.transit.EdyTransitData;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import net.kazzz.felica.FeliCaTag;
import net.kazzz.felica.command.ReadResponse;
import net.kazzz.felica.lib.FeliCaLib;
import org.apache.commons.lang3.ArrayUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FelicaCard extends Card {
    private FeliCaLib.IDm  mIDm;
    private FeliCaLib.PMm  mPMm;
    private FelicaSystem[] mSystems;

    public static Creator<FelicaCard> CREATOR = new Creator<FelicaCard>() {
        public FelicaCard createFromParcel(Parcel source) {
            int tagIdLength = source.readInt();
            byte[] tagId = new byte[tagIdLength];
            source.readByteArray(tagId);

            Date scannedAt = new Date(source.readLong());

            FeliCaLib.IDm idm = source.readParcelable(FeliCaLib.IDm.class.getClassLoader());
            FeliCaLib.PMm pmm = source.readParcelable(FeliCaLib.PMm.class.getClassLoader());

            FelicaSystem[] systems = new FelicaSystem[source.readInt()];
            source.readTypedArray(systems, FelicaSystem.CREATOR);
            
            return new FelicaCard(tagId, scannedAt, idm, pmm, systems);
        }

        public FelicaCard[] newArray(int size) {
            return new FelicaCard[size];
        }
    };

    // https://github.com/tmurakam/felicalib/blob/master/src/dump/dump.c
    // https://github.com/tmurakam/felica2money/blob/master/src/card/Suica.cs
    public static FelicaCard dumpTag(byte[] tagId, Tag tag) throws Exception {
        FeliCaTag ft = new FeliCaTag(tag);

        FeliCaLib.IDm idm = ft.pollingAndGetIDm(FeliCaLib.SYSTEMCODE_ANY);
        FeliCaLib.PMm pmm = ft.getPMm();

        if (idm == null)
            throw new Exception("Failed to read IDm");

        List<FelicaSystem> systems = new ArrayList<FelicaSystem>();

        // FIXME: Enumerate "areas" inside of systems ???
                
        for (FeliCaLib.SystemCode code : ft.getSystemCodeList()) {
            Log.d("FelicaCard", "Got system code: " + Utils.getHexString(code.getBytes()));

            int systemCode = Utils.byteArrayToInt(code.getBytes());
            //ft.polling(systemCode);

            FeliCaLib.IDm this_idm = ft.pollingAndGetIDm(systemCode);
            
            Log.d("FelicaCard", " - Got IDm: " + Utils.getHexString(this_idm.getBytes()) + "  compare: " + Utils.getHexString(idm.getBytes()));
            
            byte[] foo = idm.getBytes();
            ArrayUtils.reverse(foo);
            Log.d("FelicaCard", " - Got Card ID? " + Utils.byteArrayToInt(idm.getBytes(), 2, 6  ) + "  " + Utils.byteArrayToInt(foo, 2, 6));
            
            Log.d("FelicaCard", " - Got PMm: " + Utils.getHexString(ft.getPMm().getBytes()) + "  compare: " + Utils.getHexString(pmm.getBytes()));

            List<FelicaService> services = new ArrayList<FelicaService>();

            for (FeliCaLib.ServiceCode serviceCode : ft.getServiceCodeList()) {
                // There appears to be some disagreement over byte order.
                byte[] bytes = serviceCode.getBytes();
                ArrayUtils.reverse(bytes);
                int serviceCodeInt = Utils.byteArrayToInt(bytes);
                serviceCode = new FeliCaLib.ServiceCode(serviceCodeInt);

                List<FelicaBlock> blocks = new ArrayList<FelicaBlock>();

                ft.polling(systemCode);

                byte addr = 0;
                ReadResponse result = ft.readWithoutEncryption(serviceCode, addr);
                while (result != null && result.getStatusFlag1() == 0) {
                    blocks.add(new FelicaBlock(addr, result.getBlockData()));
                    addr++;
                    result = ft.readWithoutEncryption(serviceCode, addr);
                }

                if (blocks.size() > 0) { // Most service codes appear to be empty...
                    FelicaBlock[] blocksArray = blocks.toArray(new FelicaBlock[blocks.size()]);
                    services.add(new FelicaService(serviceCodeInt, blocksArray));
                }
            }

            FelicaService[] servicesArray = services.toArray(new FelicaService[services.size()]);
            systems.add(new FelicaSystem(Utils.byteArrayToInt(code.getBytes()), servicesArray));
        }

        FelicaSystem[] systemsArray = systems.toArray(new FelicaSystem[systems.size()]);
        return new FelicaCard(tagId, new Date(), idm, pmm, systemsArray);
    }

    public FelicaCard(byte[] tagId, Date scannedAt, FeliCaLib.IDm idm, FeliCaLib.PMm pmm, FelicaSystem[] systems) {
        super(tagId, scannedAt);
        mIDm     = idm;
        mPMm     = pmm;
        mSystems = systems;
    }

    public FeliCaLib.IDm getIDm() {
        return mIDm;
    }

    public FeliCaLib.PMm getPMm() {
        return mPMm;
    }

    // FIXME: Getters that parse IDm...

    // date ????
    /*
    public int getManufactureCode() {

    }

    public int getCardIdentification() {

    }

    public int getROMType() {

    }

    public int getICType() {

    }

    public int getTimeout() {

    }
    */

    public FelicaSystem[] getSystems() {
        return mSystems;
    }

    public FelicaSystem getSystem(int systemCode) {
        for (FelicaSystem system : mSystems) {
            if (system.getCode() == systemCode) {
                return system;
            }
        }
        return null;
    }

    public static FelicaCard fromXml(byte[] tagId, Date scannedAt, Element element) {
        Element systemsElement = (Element) element.getElementsByTagName("systems").item(0);

        NodeList systemElements = systemsElement.getElementsByTagName("system");

        FeliCaLib.IDm idm = new FeliCaLib.IDm(Base64.decode(element.getElementsByTagName("idm").item(0).getTextContent(), Base64.DEFAULT));
        FeliCaLib.PMm pmm = new FeliCaLib.PMm(Base64.decode(element.getElementsByTagName("pmm").item(0).getTextContent(), Base64.DEFAULT));

        FelicaSystem[] systems = new FelicaSystem[systemElements.getLength()];

        for (int x = 0; x < systemElements.getLength(); x++) {
            Element systemElement = (Element) systemElements.item(x);

            int systemCode = Integer.parseInt(systemElement.getAttribute("code"));

            Element servicesElement = (Element) systemElement.getElementsByTagName("services").item(0);

            NodeList serviceElements = servicesElement.getElementsByTagName("service");

            FelicaService[] services = new FelicaService[serviceElements.getLength()];

            for (int y = 0; y < serviceElements.getLength(); y++) {
                Element serviceElement = (Element) serviceElements.item(y);
                int serviceCode = Integer.parseInt(serviceElement.getAttribute("code"));

                Element blocksElement = (Element) serviceElement.getElementsByTagName("blocks").item(0);

                NodeList blockElements = blocksElement.getElementsByTagName("block");

                FelicaBlock[] blocks = new FelicaBlock[blockElements.getLength()];

                for (int z = 0; z < blockElements.getLength(); z++) {
                    Element blockElement = (Element) blockElements.item(z);
                    byte address = Byte.parseByte(blockElement.getAttribute("address"));
                    byte[] data = Base64.decode(blockElement.getTextContent(), Base64.DEFAULT);

                    blocks[z] = new FelicaBlock(address, data);
                }

                services[y] = new FelicaService(serviceCode, blocks);
            }

            systems[x] = new FelicaSystem(systemCode, services);
        }

        return new FelicaCard(tagId, scannedAt, idm, pmm, systems);
    }

    @Override
    public CardType getCardType() {
        return CardType.FeliCa;
    }

    @Override
    public TransitIdentity parseTransitIdentity() {
        if (SuicaTransitData.check(this))
            return SuicaTransitData.parseTransitIdentity(this);
        else if (EdyTransitData.check(this))
            return EdyTransitData.parseTransitIdentity(this);
        return null;
    }

    @Override
    public TransitData parseTransitData() {
        Log.d("FelicaCard", "parseTransitData() called!!");
        if (SuicaTransitData.check(this))
            return new SuicaTransitData(this);
        else if (EdyTransitData.check(this))
            return new EdyTransitData(this);
        return null;
    }

    @Override
    public Element toXML() throws Exception {
        Element root = super.toXML();

        Document doc = root.getOwnerDocument();

        Element idmElement = doc.createElement("idm");
        idmElement.setTextContent(Base64.encodeToString(mIDm.getBytes(), Base64.DEFAULT));
        root.appendChild(idmElement);

        Element pmmElement = doc.createElement("pmm");
        pmmElement.setTextContent(Base64.encodeToString(mPMm.getBytes(), Base64.DEFAULT));
        root.appendChild(pmmElement);

        Element systemsElement = doc.createElement("systems");

        for (FelicaSystem system : mSystems) {
            Element systemElement = doc.createElement("system");
            systemElement.setAttribute("code", String.valueOf(system.getCode()));

            Element servicesElement = doc.createElement("services");
            for (FelicaService service : system.getServices()) {
                Element serviceElement = doc.createElement("service");
                serviceElement.setAttribute("code", String.valueOf(service.getServiceCode()));

                Element blocksElement = doc.createElement("blocks");
                for (FelicaBlock block : service.getBlocks()) {
                    Element blockElement = doc.createElement("block");
                    blockElement.setAttribute("address", String.valueOf(block.getAddress()));
                    blockElement.setTextContent(Base64.encodeToString(block.getData(), Base64.DEFAULT));

                    blocksElement.appendChild(blockElement);
                }

                serviceElement.appendChild(blocksElement);

                servicesElement.appendChild(serviceElement);
            }

            systemElement.appendChild(servicesElement);

            systemsElement.appendChild(systemElement);
        }

        root.appendChild(systemsElement);

        return root;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeParcelable(mIDm, flags);
        parcel.writeParcelable(mPMm, flags);
        parcel.writeInt(mSystems.length);
        parcel.writeTypedArray(mSystems, flags);
    }
}
