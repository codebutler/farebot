/*
 * CardStream.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.app.feature.home

import com.codebutler.farebot.app.core.app.FareBotApplication
import com.codebutler.farebot.app.core.kotlin.Optional
import com.codebutler.farebot.app.core.kotlin.filterAndGetOptional
import com.codebutler.farebot.app.core.nfc.NfcStream
import com.codebutler.farebot.app.core.nfc.TagReaderFactory
import com.codebutler.farebot.app.core.sample.RawSampleCard
import com.codebutler.farebot.app.core.serialize.CardKeysSerializer
import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.key.CardKeys
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.model.SavedCard
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class CardStream(
    private val application: FareBotApplication,
    private val cardPersister: CardPersister,
    private val cardSerializer: CardSerializer,
    private val cardKeysPersister: CardKeysPersister,
    private val cardKeysSerializer: CardKeysSerializer,
    private val nfcStream: NfcStream,
    private val tagReaderFactory: TagReaderFactory
) {

    private val loadingRelay: BehaviorRelay<Boolean> = BehaviorRelay.createDefault(false)
    private val errorRelay: PublishRelay<Throwable> = PublishRelay.create()
    private val sampleRelay: PublishRelay<RawCard<*>> = PublishRelay.create()

    fun observeCards(): Observable<RawCard<*>> {
        val realCards = nfcStream.observe()
                .observeOn(Schedulers.io())
                .doOnNext { loadingRelay.accept(true) }
                .map { tag -> Optional(
                        try {
                            val cardKeys = getCardKeys(ByteUtils.getHexString(tag.id))
                            val rawCard = tagReaderFactory.getTagReader(tag.id, tag, cardKeys).readTag()
                            if (rawCard.isUnauthorized) {
                                throw CardUnauthorizedException()
                            }
                            rawCard
                        } catch (error: Throwable) {
                            errorRelay.accept(error)
                            loadingRelay.accept(false)
                            null
                        })
                }
                .filterAndGetOptional()

        val sampleCards = sampleRelay
                .observeOn(Schedulers.io())
                .doOnNext { loadingRelay.accept(true) }
                .delay(3, TimeUnit.SECONDS)

        return Observable.merge(realCards, sampleCards)
                .doOnNext { card ->
                    application.updateTimestamp(card.tagId().hex())
                    cardPersister.insertCard(SavedCard(
                        type = card.cardType(),
                        serial = card.tagId().hex(),
                        data = cardSerializer.serialize(card)))
                }
                .doOnNext { loadingRelay.accept(false) }
    }

    fun observeLoading(): Observable<Boolean> = loadingRelay.hide()

    fun observeErrors(): Observable<Throwable> = errorRelay.hide()

    fun emitSample() {
        sampleRelay.accept(RawSampleCard())
    }

    private fun getCardKeys(tagId: String): CardKeys? {
        val savedKey = cardKeysPersister.getForTagId(tagId) ?: return null
        return cardKeysSerializer.deserialize(savedKey.keyData)
    }

    class CardUnauthorizedException : Throwable() {
        override val message: String
            get() = "Unauthorized"
    }
}
