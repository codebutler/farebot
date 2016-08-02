package com.codebutler.farebot.card;

import android.nfc.Tag;
import android.nfc.tech.TagTechnology;
import android.support.annotation.NonNull;

import org.apache.commons.io.IOUtils;

public abstract class TagReader<T extends TagTechnology, C extends RawCard> {

    @NonNull private final byte[] mTagId;
    @NonNull private final Tag mTag;

    public TagReader(@NonNull byte[] tagId, @NonNull Tag tag) {
        mTagId = tagId;
        mTag = tag;
    }

    @NonNull
    public C readTag() throws Exception {
        T tech = getTech(mTag);
        try {
            tech.connect();
            return readTag(mTagId, mTag, tech);
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
            @NonNull T tech)
    throws Exception;

    @NonNull
    protected abstract T getTech(@NonNull Tag tag);
}
