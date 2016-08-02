package com.codebutler.farebot.card.classic;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.TagReader;
import com.codebutler.farebot.card.classic.raw.RawClassicBlock;
import com.codebutler.farebot.card.classic.raw.RawClassicCard;
import com.codebutler.farebot.card.classic.raw.RawClassicSector;
import com.codebutler.farebot.key.CardKeys;
import com.codebutler.farebot.key.ClassicCardKeys;
import com.codebutler.farebot.key.ClassicSectorKey;
import com.codebutler.farebot.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClassicTagReader extends TagReader<MifareClassic, RawClassicCard> {

    private static final byte[] PREAMBLE_KEY = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00};

    public ClassicTagReader(@NonNull byte[] tagId, @NonNull Tag tag) {
        super(tagId, tag);
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
            @NonNull MifareClassic tech) throws Exception {
        ClassicCardKeys keys = (ClassicCardKeys) CardKeys.forTagId(tagId);

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
                            if (sectorKey.getType().equals(ClassicSectorKey.TYPE_KEYA)) {
                                authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, sectorKey.getKey());
                            } else {
                                authSuccess = tech.authenticateSectorWithKeyB(sectorIndex, sectorKey.getKey());
                            }
                        }
                    }

                    if (!authSuccess) {
                        // Be a little more forgiving on the key list.  Lets try all the keys!
                        //
                        // This takes longer, of course, but means that users aren't scratching
                        // their heads when we don't get the right key straight away.
                        ClassicSectorKey[] cardKeys = keys.keys();

                        for (int keyIndex = 0; keyIndex < cardKeys.length; keyIndex++) {
                            if (keyIndex == sectorIndex) {
                                // We tried this before
                                continue;
                            }

                            if (cardKeys[keyIndex].getType().equals(ClassicSectorKey.TYPE_KEYA)) {
                                authSuccess = tech.authenticateSectorWithKeyA(sectorIndex,
                                        cardKeys[keyIndex].getKey());
                            } else {
                                authSuccess = tech.authenticateSectorWithKeyB(sectorIndex,
                                        cardKeys[keyIndex].getKey());
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
                sectors.add(RawClassicSector.createInvalid(sectorIndex, Utils.getErrorMessage(ex)));
            }
        }

        return RawClassicCard.create(tagId, new Date(), sectors);
    }
}
