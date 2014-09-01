/*
 * FelicaSystem.java
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

import com.codebutler.farebot.util.Utils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name="system")
public class FelicaSystem {
    @Attribute(name="code") private int mCode;
    @ElementList(name="services") private List<FelicaService> mServices;

    private FelicaSystem() { /* For XML Serializer */ }

    public FelicaSystem(int code, FelicaService[] services) {
        mCode = code;
        mServices = Utils.arrayAsList(services);
    }

    public int getCode() {
        return mCode;
    }

    public List<FelicaService> getServices() {
        return mServices;
    }

    public FelicaService getService(int serviceCode) {
        for (FelicaService service : mServices) {
            if (service.getServiceCode() == serviceCode) {
                return service;
            }
        }
        return null;
    }
}
