package com.codebutler.farebot.shared.platform

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

private lateinit var appContext: Context

fun initDeviceRegion(context: Context) {
    appContext = context.applicationContext
}

actual fun getDeviceRegion(): String? {
    if (!::appContext.isInitialized) return Locale.getDefault().country.uppercase(Locale.US)

    val tm = appContext.getSystemService(Context.TELEPHONY_SERVICE)
    if (tm is TelephonyManager && (
        tm.phoneType == TelephonyManager.PHONE_TYPE_GSM ||
        tm.phoneType == TelephonyManager.PHONE_TYPE_CDMA
    )) {
        val netCountry = tm.networkCountryIso
        if (netCountry != null && netCountry.length == 2) {
            return netCountry.uppercase(Locale.US)
        }

        val simCountry = tm.simCountryIso
        if (simCountry != null && simCountry.length == 2) {
            return simCountry.uppercase(Locale.US)
        }
    }

    return Locale.getDefault().country.uppercase(Locale.US)
}
