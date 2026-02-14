package com.codebutler.farebot.shared.di

import com.codebutler.farebot.shared.core.NavDataHolder
import com.codebutler.farebot.shared.platform.Analytics
import com.codebutler.farebot.shared.platform.AppPreferences
import com.codebutler.farebot.shared.platform.InMemoryAppPreferences
import com.codebutler.farebot.shared.platform.NoOpAnalytics
import com.codebutler.farebot.shared.serialize.CardImporter
import com.codebutler.farebot.shared.viewmodel.AddKeyViewModel
import com.codebutler.farebot.shared.viewmodel.CardViewModel
import com.codebutler.farebot.shared.viewmodel.HistoryViewModel
import com.codebutler.farebot.shared.viewmodel.HomeViewModel
import com.codebutler.farebot.shared.viewmodel.KeysViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sharedModule =
    module {
        single { NavDataHolder() }
        single { CardImporter(get(), get()) }
        single<Analytics> { NoOpAnalytics() }
        single<AppPreferences> { InMemoryAppPreferences() }

        viewModel { HomeViewModel(getOrNull(), get(), get(), get(), get()) }
        viewModel { CardViewModel(get(), get(), get(), get(), get(), get()) }
        viewModel { HistoryViewModel(get(), get(), get(), get(), get()) }
        viewModel { KeysViewModel(get()) }
        viewModel { AddKeyViewModel(get(), getOrNull()) }
    }
