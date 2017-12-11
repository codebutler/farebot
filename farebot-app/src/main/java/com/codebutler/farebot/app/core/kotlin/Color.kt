/*
 * Color.java
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

package com.codebutler.farebot.app.core.kotlin

import android.content.Context
import android.graphics.Color
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.v4.content.res.ResourcesCompat

@ColorInt
fun Context.getColor(@ColorRes colorRes: Int?, @ColorInt defaultColor: Int) =
        if (colorRes == null) defaultColor else ResourcesCompat.getColor(resources, colorRes, theme)

@ColorInt
fun adjustAlpha(@ColorInt color: Int, alpha: Int = 0): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
