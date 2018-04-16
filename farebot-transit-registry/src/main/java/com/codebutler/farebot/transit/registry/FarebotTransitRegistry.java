package com.codebutler.farebot.transit.registry;

import android.content.Context;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.transit.registry.annotations.TransitCardRegistry;
import java.util.List;
import java.util.Map;

@TransitCardRegistry
public abstract class FarebotTransitRegistry {
  public static Map<Class<? extends Card>, List<TransitFactory<Card, TransitInfo>>> get(Context context) {
    return CardsRegistry_FarebotTransitRegistry.create();
  }
}
