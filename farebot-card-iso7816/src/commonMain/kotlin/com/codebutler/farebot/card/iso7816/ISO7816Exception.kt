/*
 * ISO7816Exception.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.card.iso7816

open class ISO7816Exception internal constructor(s: String) : Exception(s)

class ISOEOFException : ISO7816Exception("End of file")
class ISOFileNotFoundException : ISO7816Exception("File not found")
class ISONoCurrentEF : ISO7816Exception("No current EF")
class ISOInstructionCodeNotSupported : ISO7816Exception("Instruction code not supported")
class ISOClassNotSupported : ISO7816Exception("Class not supported")
class ISOSecurityStatusNotSatisfied : ISO7816Exception("Security status not satisfied")
