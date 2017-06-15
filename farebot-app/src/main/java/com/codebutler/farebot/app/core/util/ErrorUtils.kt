/*
 * ErrorUtils.kt
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

package com.codebutler.farebot.app.core.util

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.util.Log
import android.widget.Toast

object ErrorUtils {

    fun showErrorAlert(activity: Activity, ex: Throwable) {
        Log.e(activity.javaClass.name, ex.message, ex)
        AlertDialog.Builder(activity)
                .setMessage(getErrorMessage(ex))
                .show()
    }

    fun showErrorToast(activity: Activity, ex: Throwable) {
        Log.e(activity.javaClass.name, ex.message, ex)
        Toast.makeText(activity, getErrorMessage(ex), Toast.LENGTH_SHORT).show()
    }

    fun getErrorMessage(ex: Throwable): String {
        val ex1 = if (ex.cause != null) ex.cause as Throwable else ex
        var errorMessage = ex1.localizedMessage
        if (TextUtils.isEmpty(errorMessage)) {
            errorMessage = ex1.message
        }
        if (TextUtils.isEmpty(errorMessage)) {
            errorMessage = ex1.toString()
        }
        return errorMessage
    }
}
