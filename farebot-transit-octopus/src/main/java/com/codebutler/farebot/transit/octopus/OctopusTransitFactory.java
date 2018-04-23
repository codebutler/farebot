/*
 * OctopusTransitFactory.java
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
 *
 * Portions based on FelicaCard.java from nfcard project
 * Copyright 2013 Sinpo Wei <sinpowei@gmail.com>
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

package com.codebutler.farebot.transit.octopus;

import android.support.annotation.NonNull;
import com.codebutler.farebot.base.util.ByteArray;
import com.codebutler.farebot.base.util.ByteUtils;
import com.codebutler.farebot.card.felica.FelicaCard;
import com.codebutler.farebot.card.felica.FelicaService;
import com.codebutler.farebot.card.felica.FelicaSystem;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.registry.annotations.TransitCard;
import net.kazzz.felica.lib.FeliCaLib;

import static com.codebutler.farebot.transit.octopus.OctopusTransitInfo.*;

@TransitCard
public class OctopusTransitFactory implements TransitFactory<FelicaCard, OctopusTransitInfo> {

    @Override
    public boolean check(@NonNull FelicaCard card) {
        return (card.getSystem(FeliCaLib.SYSTEMCODE_OCTOPUS) != null)
                || (card.getSystem(FeliCaLib.SYSTEMCODE_SZT) != null);
    }

    @NonNull
    @Override
    public TransitIdentity parseIdentity(@NonNull FelicaCard card) {
        if (card.getSystem(FeliCaLib.SYSTEMCODE_SZT) != null) {
            if (card.getSystem(FeliCaLib.SYSTEMCODE_OCTOPUS) != null) {
                // Dual-mode card.
                return TransitIdentity.create(DUAL_NAME, null);
            } else {
                // SZT-only card.
                return TransitIdentity.create(SZT_NAME, null);
            }
        } else {
            // Octopus-only card.
            return TransitIdentity.create(OCTOPUS_NAME, null);
        }
    }

    @NonNull
    @Override
    public OctopusTransitInfo parseInfo(@NonNull FelicaCard card) {
        int octopusBalance = 0;
        int shenzhenBalance = 0;
        boolean hasOctopus = false;
        boolean hasShenzhen = false;

        FelicaSystem octopusSystem = card.getSystem(FeliCaLib.SYSTEMCODE_OCTOPUS);
        if (octopusSystem != null) {
            FelicaService service = octopusSystem.getService(FeliCaLib.SERVICE_OCTOPUS);
            if (service != null) {
                ByteArray metadata = service.getBlocks().get(0).getData();
                octopusBalance = ByteUtils.byteArrayToInt(metadata.bytes(), 0, 4) - 350;
                hasOctopus = true;
            }
        }

        FelicaSystem sztSystem = card.getSystem(FeliCaLib.SYSTEMCODE_SZT);
        if (sztSystem != null) {
            FelicaService service = sztSystem.getService(FeliCaLib.SERVICE_SZT);
            if (service != null) {
                ByteArray metadata = service.getBlocks().get(0).getData();
                shenzhenBalance = ByteUtils.byteArrayToInt(metadata.bytes(), 0, 4) - 350;
                hasShenzhen = true;
            }
        }

        return OctopusTransitInfo.create(octopusBalance, shenzhenBalance, hasOctopus, hasShenzhen);
    }
}
