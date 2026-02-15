/*
 * DesfireExceptions.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.desfire

/**
 * Thrown when an operation requires authentication that has not been performed.
 * This is the base class for all access-control related exceptions.
 *
 * Ported from Metrodroid's UnauthorizedException.
 */
open class UnauthorizedException(
    override val message: String = "Unauthorized",
) : IllegalStateException()

/**
 * Thrown when a requested resource (application, file, etc.) is not found on the card.
 *
 * Ported from Metrodroid's NotFoundException.
 */
open class NotFoundException(
    override val message: String = "Not found",
) : IllegalStateException()

/**
 * Thrown when a specific permission is denied (as opposed to general authentication failure).
 * This is a subclass of [UnauthorizedException].
 *
 * Ported from Metrodroid's DesfireProtocol.PermissionDeniedException.
 */
class PermissionDeniedException : UnauthorizedException("Permission denied")
