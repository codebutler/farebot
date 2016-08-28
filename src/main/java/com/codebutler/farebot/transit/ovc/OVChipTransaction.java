/*
 * OVChipTransaction.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2015 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ovc;

import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.codebutler.farebot.util.Utils;
import com.google.auto.value.AutoValue;

import java.util.Comparator;

@AutoValue
abstract class OVChipTransaction implements Parcelable {

    static final Comparator<OVChipTransaction> ID_ORDER = new Comparator<OVChipTransaction>() {
        @Override
        public int compare(OVChipTransaction t1, OVChipTransaction t2) {
            return (t1.getId() < t2.getId() ? -1 : (t1.getId() == t2.getId() ? 0 : 1));
        }
    };

    @NonNull
    static OVChipTransaction create(int transactionSlot, byte[] data) {
        if (data == null) {
            data = new byte[32];
        }

        int valid = 1;
        int date = 0;
        int time = 0;
        int unknownConstant = 0;
        int transfer = -3; // Default: No-data
        int company = 0;
        int id = 0;
        int station = 0;
        int machineId = 0;
        int vehicleId = 0;
        int productId = 0;
        int unknownConstant2 = 0;
        int amount = 0;
        int subscriptionId = -1; // Default: No valid subscriptionId
        String errorMessage = "";

        if (data[0] == (byte) 0x00
                && data[1] == (byte) 0x00
                && data[2] == (byte) 0x00
                && (data[3] & (byte) 0xF0) == (byte) 0x00) {
            valid = 0;
        }
        if ((data[3] & (byte) 0x10) != (byte) 0x00) {
            valid = 0;
        }
        if ((data[3] & (byte) 0x80) != (byte) 0x00) {
            valid = 0;
        }
        if ((data[2] & (byte) 0x02) != (byte) 0x00) {
            valid = 0;
        }
        if ((data[2] & (byte) 0x08) != (byte) 0x00) {
            valid = 0;
        }
        if ((data[2] & (byte) 0x20) != (byte) 0x00) {
            valid = 0;
        }
        if ((data[2] & (byte) 0x80) != (byte) 0x00) {
            valid = 0;
        }
        if ((data[1] & (byte) 0x01) != (byte) 0x00) {
            valid = 0;
        }
        if ((data[1] & (byte) 0x02) != (byte) 0x00) {
            valid = 0;
        }
        if ((data[1] & (byte) 0x08) != (byte) 0x00) {
            valid = 0;
        }
        if ((data[1] & (byte) 0x20) != (byte) 0x00) {
            valid = 0;
        }
        if ((data[1] & (byte) 0x40) != (byte) 0x00) {
            valid = 0;
        }
        if ((data[1] & (byte) 0x80) != (byte) 0x00) {
            valid = 0;
        }
        if ((data[0] & (byte) 0x02) != (byte) 0x00) {
            valid = 0;
        }
        if ((data[0] & (byte) 0x04) != (byte) 0x00) {
            valid = 0;
        }

        if (valid == 0) {
            errorMessage = "No transaction";
        } else {
            int iBitOffset = 53; // Ident, Date, Time

            date = (((char) data[3] & (char) 0x0F) << 10) | (((char) data[4] & (char) 0xFF) << 2)
                    | (((char) data[5] >> 6) & (char) 0x03);
            time = (((char) data[5] & (char) 0x3F) << 5) | (((char) data[6] >> 3) & (char) 0x1F);

            if ((data[3] & (byte) 0x20) != (byte) 0x00) {
                unknownConstant = Utils.getBitsFromBuffer(data, iBitOffset, 24);
                iBitOffset += 24;
            }

            if ((data[3] & (byte) 0x40) != (byte) 0x00) {
                transfer = Utils.getBitsFromBuffer(data, iBitOffset, 7);
                iBitOffset += 7;
            }

            if ((data[2] & (byte) 0x01) != (byte) 0x00) {
                company = Utils.getBitsFromBuffer(data, iBitOffset, 16);
                iBitOffset += 16;
            }

            if ((data[2] & (byte) 0x04) != (byte) 0x00) {
                id = Utils.getBitsFromBuffer(data, iBitOffset, 24);
                iBitOffset += 24;
            }

            if ((data[2] & (byte) 0x10) != (byte) 0x00) {
                station = Utils.getBitsFromBuffer(data, iBitOffset, 16);
                iBitOffset += 16;
            }

            if ((data[2] & (byte) 0x40) != (byte) 0x00) {
                machineId = Utils.getBitsFromBuffer(data, iBitOffset, 24);
                iBitOffset += 24;
            }

            if ((data[1] & (byte) 0x04) != (byte) 0x00) {
                vehicleId = Utils.getBitsFromBuffer(data, iBitOffset, 16);
                iBitOffset += 16;
            }

            if ((data[1] & (byte) 0x10) != (byte) 0x00) {
                productId = Utils.getBitsFromBuffer(data, iBitOffset, 5);
                iBitOffset += 5;
            }

            if ((data[0] & (byte) 0x01) != (byte) 0x00) {
                unknownConstant2 = Utils.getBitsFromBuffer(data, iBitOffset, 16);
                iBitOffset += 16;
            }

            if ((data[0] & (byte) 0x08) != (byte) 0x00) {
                amount = Utils.getBitsFromBuffer(data, iBitOffset, 16);
                iBitOffset += 16;
            }

            if ((data[1] & (byte) 0x10) == (byte) 0x00) {
                subscriptionId = Utils.getBitsFromBuffer(data, iBitOffset, 13);
            }
        }

        return new AutoValue_OVChipTransaction.Builder()
                .date(date)
                .time(time)
                .transfer(transfer)
                .company(company)
                .id(id)
                .station(station)
                .machineId(machineId)
                .vehicleId(vehicleId)
                .productId(productId)
                .amount(amount)
                .subscriptionId(subscriptionId)
                .valid(valid)
                .unknownConstant(unknownConstant)
                .unknownConstant2(unknownConstant2)
                .transactionSlot(transactionSlot)
                .errorMessage(errorMessage)
                .build();
    }

    public abstract int getTransactionSlot();

    public abstract int getDate();

    public abstract int getTime();

    public abstract int getTransfer();

    public abstract int getCompany();

    public abstract int getId();

    public abstract int getStation();

    public abstract int getMachineId();

    public abstract int getVehicleId();

    public abstract int getProductId();

    public abstract int getAmount();

    public abstract int getSubscriptionId();

    public abstract int getValid();

    public abstract int getUnknownConstant();

    public abstract int getUnknownConstant2();

    public abstract String getErrorMessage();

    public boolean isSameTrip(OVChipTransaction nextTransaction) {
        /*
         * Information about checking in and out:
         * http://www.chipinfo.nl/inchecken/
         */

        if (getCompany() == nextTransaction.getCompany() && getTransfer() == OVChipTransitData.PROCESS_CHECKIN
                && nextTransaction.getTransfer() == OVChipTransitData.PROCESS_CHECKOUT) {
            if (getDate() == nextTransaction.getDate()) {
                return true;
            } else if (getDate() == nextTransaction.getDate() - 1) {
                // All NS trips get reset at 4 AM (except if it's a night train, but that's out of our scope).
                if (getCompany() == OVChipTransitData.AGENCY_NS && nextTransaction.getTime() < 240) {
                    return true;
                }

                /*
                 * Some companies expect a checkout at the maximum of 15 minutes after the estimated arrival at the
                 * endstation of the line.
                 * But it's hard to determine the length of every single trip there is, so for now let's just assume a
                 * checkout at the next day is still from the same trip. Better solutions are always welcome ;)
                 */
                if (getCompany() != OVChipTransitData.AGENCY_NS) {
                    return true;
                }
            }
        }

        return false;
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder transactionSlot(int transactionSlot);

        abstract Builder date(int date);

        abstract Builder time(int time);

        abstract Builder transfer(int transfer);

        abstract Builder company(int company);

        abstract Builder id(int id);

        abstract Builder station(int station);

        abstract Builder machineId(int machineId);

        abstract Builder vehicleId(int vehicleId);

        abstract Builder productId(int productId);

        abstract Builder amount(int amount);

        abstract Builder subscriptionId(int subscriptionId);

        abstract Builder valid(int valid);

        abstract Builder unknownConstant(int unknownConstant);

        abstract Builder unknownConstant2(int unknownConstant2);

        abstract Builder errorMessage(String errorMessage);

        abstract OVChipTransaction build();
    }
}
