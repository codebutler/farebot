package com.codebutler.farebot.transit.registry.annotations;

import com.uber.crumb.annotations.CrumbProducer;

@CrumbProducer
public @interface TransitCard {
  CardType value();
}
