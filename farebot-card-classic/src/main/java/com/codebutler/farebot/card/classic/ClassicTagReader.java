/*
 * ClassicTagReader.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.card.classic;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.codebutler.farebot.card.TagReader;
import com.codebutler.farebot.card.classic.key.ClassicCardKeys;
import com.codebutler.farebot.card.classic.key.ClassicSectorKey;
import com.codebutler.farebot.card.classic.raw.RawClassicBlock;
import com.codebutler.farebot.card.classic.raw.RawClassicCard;
import com.codebutler.farebot.card.classic.raw.RawClassicSector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClassicTagReader extends TagReader<MifareClassic, RawClassicCard, ClassicCardKeys> {

    private static final byte[] PREAMBLE_KEY = {
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x00
    };

    public ClassicTagReader(@NonNull byte[] tagId, @NonNull Tag tag, @Nullable ClassicCardKeys cardKeys) {
        super(tagId, tag, cardKeys);
    }

    @NonNull
    @Override
    protected MifareClassic getTech(@NonNull Tag tag) {
        return MifareClassic.get(tag);
    }

    @NonNull
    @Override
    protected RawClassicCard readTag(
            @NonNull byte[] tagId,
            @NonNull Tag tag,
            @NonNull MifareClassic tech,
            @Nullable ClassicCardKeys keys) throws Exception {
        List<RawClassicSector> sectors = new ArrayList<>();

        for (int sectorIndex = 0; sectorIndex < tech.getSectorCount(); sectorIndex++) {
            try {
                boolean authSuccess = false;

                // Try the default keys first
                if (!authSuccess && sectorIndex == 0) {
                    authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, PREAMBLE_KEY);
                }

                if (!authSuccess) {
                    authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, MifareClassic.KEY_DEFAULT);
                }

                if (keys != null) {
                    // Try with a 1:1 sector mapping on our key list first
                    if (!authSuccess) {
                        ClassicSectorKey sectorKey = keys.keyForSector(sectorIndex);
                        if (sectorKey != null) {
                            authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, sectorKey.getKeyA().bytes());
                            if (!authSuccess) {
                                authSuccess = tech.authenticateSectorWithKeyB(sectorIndex, sectorKey.getKeyB().bytes());
                            }
                        }
                    }

                    if (!authSuccess) {
                        // Be a little more forgiving on the key list.  Lets try all the keys!
                        //
                        // This takes longer, of course, but means that users aren't scratching
                        // their heads when we don't get the right key straight away.
                        List<ClassicSectorKey> cardKeys = keys.keys();

                        for (int keyIndex = 0; keyIndex < cardKeys.size(); keyIndex++) {
                            if (keyIndex == sectorIndex) {
                                // We tried this before
                                continue;
                            }

                            authSuccess = tech.authenticateSectorWithKeyA(sectorIndex,
                                    cardKeys.get(keyIndex).getKeyA().bytes());

                            if (!authSuccess) {
                                authSuccess = tech.authenticateSectorWithKeyB(sectorIndex,
                                        cardKeys.get(keyIndex).getKeyB().bytes());
                            }

                            if (authSuccess) {
                                // Jump out if we have the key
                                break;
                            }
                        }
                    }
                }

                if (authSuccess) {
                    List<RawClassicBlock> blocks = new ArrayList<>();
                    // FIXME: First read trailer block to get type of other blocks.
                    int firstBlockIndex = tech.sectorToBlock(sectorIndex);
                    for (int blockIndex = 0; blockIndex < tech.getBlockCountInSector(sectorIndex); blockIndex++) {
                        byte[] data = tech.readBlock(firstBlockIndex + blockIndex);
                        blocks.add(RawClassicBlock.create(blockIndex, data));
                    }
                    sectors.add(RawClassicSector.createData(sectorIndex, blocks));
                } else {
                    sectors.add(RawClassicSector.createUnauthorized(sectorIndex));
                }
            } catch (IOException ex) {
                throw ex;
            } catch (Exception ex) {
                sectors.add(RawClassicSector.createInvalid(sectorIndex, ex.getMessage()));
            }
        }

        return RawClassicCard.create(tagId, new Date(), sectors);
    }
}
