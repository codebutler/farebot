package com.codebutler.farebot.transit.stub;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicSector;
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector;
import com.codebutler.farebot.card.desfire.DesfireApplication;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.desfire.DesfireFile;
import com.codebutler.farebot.card.desfire.UnauthorizedDesfireFile;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitIdentity;

public class UnauthorizedTransitInfoFactory implements TransitFactory<Card,UnauthorizedTransitInfo> {

    @NonNull private final Resources mResources;

    public UnauthorizedTransitInfoFactory(@NonNull Resources resources) {
        mResources = resources;
    }

    @Override
    public boolean check(@NonNull Card card) {
        if (card instanceof ClassicCard) {
            boolean foundSector = false;
            for (ClassicSector s : ((ClassicCard) card).getSectors()) {
                foundSector = true;
                if (!(s instanceof UnauthorizedClassicSector)) {
                    return false;
                }
            }
            return foundSector;
        } else if (card instanceof DesfireCard) {
            boolean foundFile = false;
            for (DesfireApplication application : ((DesfireCard) card).getApplications()) {
                for (DesfireFile file : application.getFiles()) {
                    foundFile = true;
                    if (!(file instanceof UnauthorizedDesfireFile)) {
                        return false;
                    }
                }
            }
            return foundFile;
        }
        return false;
    }

    @NonNull
    @Override
    public TransitIdentity parseIdentity(@NonNull Card card) {
        return TransitIdentity.create(
                mResources.getString(R.string.locked_card),
                String.format("%s - %s", card.getCardType(), card.getTagId().hex()));
    }

    @NonNull
    @Override
    public UnauthorizedTransitInfo parseInfo(@NonNull Card card) {
        return UnauthorizedTransitInfo.create();
    }
}
