/*
 * TagReader.java
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

package com.codebutler.farebot.card;

import android.nfc.Tag;
import android.nfc.tech.TagTechnology;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.key.CardKeys;

import org.apache.commons.io.IOUtils;

public abstract class TagReader<
        T extends TagTechnology,
        C extends RawCard,
        K extends CardKeys> {

    @NonNull private final byte[] mTagId;
    @NonNull private final Tag mTag;
    @Nullable private final K mCardKeys;

    public TagReader(@NonNull byte[] tagId, @NonNull Tag tag, @Nullable K cardKeys) {
        mTagId = tagId;
        mTag = tag;
        mCardKeys = cardKeys;
    }

    @NonNull
    public C readTag() throws Exception {
        T tech = getTech(mTag);
        try {
            tech.connect();
            return readTag(mTagId, mTag, tech, mCardKeys);
        } finally {
            if (tech.isConnected()) {
                IOUtils.closeQuietly(tech);
            }
        }
    }

    @NonNull
    protected abstract C readTag(
            @NonNull byte[] tagId,
            @NonNull Tag tag,
            @NonNull T tech,
            @Nullable K cardKeys)
    throws Exception;

    @NonNull
    protected abstract T getTech(@NonNull Tag tag);
}
