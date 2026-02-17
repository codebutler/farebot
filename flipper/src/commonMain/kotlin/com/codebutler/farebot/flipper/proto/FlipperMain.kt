/*
 * FlipperMain.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.flipper.proto

/**
 * Flipper RPC command status codes.
 * Matches CommandStatus enum in flipper.proto.
 */
enum class CommandStatus(val value: Int) {
    OK(0),
    ERROR(1),
    ERROR_STORAGE_NOT_READY(2),
    ERROR_STORAGE_EXIST(3),
    ERROR_STORAGE_NOT_EXIST(4),
    ERROR_STORAGE_INVALID_PARAMETER(5),
    ERROR_STORAGE_DENIED(6),
    ERROR_STORAGE_INVALID_NAME(7),
    ERROR_STORAGE_INTERNAL(8),
    ERROR_STORAGE_NOT_IMPLEMENTED(9),
    ERROR_STORAGE_ALREADY_OPEN(10),
    ERROR_STORAGE_DIR_NOT_EMPTY(11),
    ERROR_APP_CANT_START(12),
    ERROR_APP_SYSTEM_LOCKED(13),
    ERROR_APP_NOT_RUNNING(14),
    ERROR_APP_CMD_ERROR(15),
    ERROR_VIRTUAL_DISPLAY_ALREADY_STARTED(16),
    ERROR_VIRTUAL_DISPLAY_NOT_STARTED(17),
    ERROR_GPIO_MODE_INCORRECT(18),
    ERROR_GPIO_UNKNOWN_PIN_MODE(19),
    ;

    companion object {
        fun fromValue(value: Int): CommandStatus =
            entries.firstOrNull { it.value == value } ?: ERROR
    }
}
