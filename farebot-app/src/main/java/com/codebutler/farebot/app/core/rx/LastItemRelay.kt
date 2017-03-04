/*
 * LastItemRelay.kt
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

package com.codebutler.farebot.app.core.rx

import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import io.reactivex.Observer
import java.util.concurrent.atomic.AtomicReference

class LastValueRelay<T> private constructor() : Relay<T>() {

    private val relay = PublishRelay.create<T>()
    private val lastValue: AtomicReference<T?> = AtomicReference()

    companion object {
        fun <T> create(): LastValueRelay<T> = LastValueRelay()
    }

    override fun accept(value: T) {
        if (hasObservers()) {
            relay.accept(value)
        } else {
            lastValue.set(value)
        }
    }

    override fun hasObservers(): Boolean = relay.hasObservers()

    override fun subscribeActual(observer: Observer<in T>) {
        lastValue.getAndSet(null)?.let(observer::onNext)
        relay.subscribeActual(observer)
    }
}
