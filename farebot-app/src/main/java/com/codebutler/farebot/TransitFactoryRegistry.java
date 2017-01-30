package com.codebutler.farebot;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.cepas.CEPASCard;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.felica.FelicaCard;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.bilhete_unico.BilheteUnicoSPTransitFactory;
import com.codebutler.farebot.transit.clipper.ClipperTransitFactory;
import com.codebutler.farebot.transit.edy.EdyTransitFactory;
import com.codebutler.farebot.transit.ezlink.EZLinkTransitFactory;
import com.codebutler.farebot.transit.hsl.HSLTransitFactory;
import com.codebutler.farebot.transit.manly_fast_ferry.ManlyFastFerryTransitFactory;
import com.codebutler.farebot.transit.myki.MykiTransitFactory;
import com.codebutler.farebot.transit.octopus.OctopusTransitFactory;
import com.codebutler.farebot.transit.opal.OpalTransitFactory;
import com.codebutler.farebot.transit.orca.OrcaTransitFactory;
import com.codebutler.farebot.transit.ovc.OVChipTransitFactory;
import com.codebutler.farebot.transit.seq_go.SeqGoTransitFactory;
import com.codebutler.farebot.transit.stub.AdelaideMetrocardStubTransitFactory;
import com.codebutler.farebot.transit.stub.AtHopStubTransitFactory;
import com.codebutler.farebot.transit.stub.UnauthorizedClassicTransitFactory;
import com.codebutler.farebot.transit.suica.SuicaTransitFactory;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransitFactoryRegistry {

    @NonNull
    private final Map<Class<? extends Card>, List<TransitFactory>> mRegistry = new HashMap<>();

    public TransitFactoryRegistry(@NonNull Context context) {
        registerFactory(FelicaCard.class, new SuicaTransitFactory(context));
        registerFactory(FelicaCard.class, new EdyTransitFactory());
        registerFactory(FelicaCard.class, new OctopusTransitFactory());

        registerFactory(DesfireCard.class, new OrcaTransitFactory());
        registerFactory(DesfireCard.class, new ClipperTransitFactory());
        registerFactory(DesfireCard.class, new HSLTransitFactory());
        registerFactory(DesfireCard.class, new OpalTransitFactory());
        registerFactory(DesfireCard.class, new MykiTransitFactory());
        registerFactory(DesfireCard.class, new AdelaideMetrocardStubTransitFactory());
        registerFactory(DesfireCard.class, new AtHopStubTransitFactory());

        registerFactory(ClassicCard.class, new OVChipTransitFactory(context));
        registerFactory(ClassicCard.class, new BilheteUnicoSPTransitFactory());
        registerFactory(ClassicCard.class, new ManlyFastFerryTransitFactory());
        registerFactory(ClassicCard.class, new SeqGoTransitFactory(context));
        // This must be registered last to throw up a warning whenever there is a card with all locked sectors
        registerFactory(ClassicCard.class, new UnauthorizedClassicTransitFactory(context.getResources()));

        registerFactory(CEPASCard.class, new EZLinkTransitFactory());
    }

    @Nullable
    public TransitIdentity parseTransitIdentity(@NonNull Card card) {
        for (TransitFactory factory : getFactories(card.getParentClass())) {
            if (factory.check(card)) {
                return factory.parseIdentity(card);
            }
        }
        return null;
    }

    @Nullable
    public TransitData parseTransitData(@NonNull Card card) {
        for (TransitFactory factory : getFactories(card.getParentClass())) {
            if (factory.check(card)) {
                return factory.parseData(card);
            }
        }
        return null;
    }

    @NonNull
    private List<TransitFactory> getFactories(@NonNull Class<? extends Card> cardClass) {
        return Optional.fromNullable(mRegistry.get(cardClass))
                .or(ImmutableList.<TransitFactory>of());
    }

    private void registerFactory(@NonNull Class<? extends Card> cardClass, @NonNull TransitFactory factory) {
        List<TransitFactory> factories = mRegistry.get(cardClass);
        if (factories == null) {
            factories = new ArrayList<>();
            mRegistry.put(cardClass, factories);
        }
        factories.add(factory);
    }
}
