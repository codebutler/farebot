/*
 * HelpScreenView.kt
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

package com.codebutler.farebot.app.feature.help

import android.content.Context
import android.nfc.NfcAdapter
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.kotlin.bindView
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.transit.manly_fast_ferry.ManlyFastFerryTransitInfo
import com.codebutler.farebot.transit.myki.MykiTransitInfo
import com.codebutler.farebot.transit.octopus.OctopusTransitInfo
import com.codebutler.farebot.transit.opal.OpalTransitInfo
import com.codebutler.farebot.transit.seq_go.SeqGoTransitInfo
import com.wealthfront.magellan.BaseScreenView
import java.util.ArrayList

class HelpScreenView(context: Context) : BaseScreenView<HelpScreen>(context) {

    companion object {
        private val SUPPORTED_CARDS = listOf(
                SupportedCard(
                        imageResId = R.drawable.orca_card,
                        name = "ORCA",
                        locationResId = R.string.location_seattle,
                        cardType = CardType.MifareDesfire
                ),
                SupportedCard(
                        imageResId = R.drawable.clipper_card,
                        name = "Clipper",
                        locationResId = R.string.location_san_francisco,
                        cardType = CardType.MifareDesfire
                ),
                SupportedCard(
                        imageResId = R.drawable.suica_card,
                        name = "Suica",
                        locationResId = R.string.location_tokyo,
                        cardType = CardType.FeliCa
                ),
                SupportedCard(
                        imageResId = R.drawable.pasmo_card,
                        name = "PASMO",
                        locationResId = R.string.location_tokyo,
                        cardType = CardType.FeliCa
                ),
                SupportedCard(
                        imageResId = R.drawable.icoca_card,
                        name = "ICOCA",
                        locationResId = R.string.location_kansai,
                        cardType = CardType.FeliCa
                ),
                SupportedCard(
                        imageResId = R.drawable.edy_card,
                        name = "Edy",
                        locationResId = R.string.location_tokyo,
                        cardType = CardType.FeliCa
                ),
                SupportedCard(
                        imageResId = R.drawable.ezlink_card,
                        name = "EZ-Link",
                        locationResId = R.string.location_singapore,
                        cardType = CardType.CEPAS,
                        extraNoteResId = R.string.ezlink_card_note
                ),
                SupportedCard(
                        imageResId = R.drawable.octopus_card,
                        name = OctopusTransitInfo.OCTOPUS_NAME,
                        locationResId = R.string.location_hong_kong,
                        cardType = CardType.FeliCa
                ),
                SupportedCard(
                        imageResId = R.drawable.bilheteunicosp_card,
                        name = "Bilhete Único",
                        locationResId = R.string.location_sao_paulo,
                        cardType = CardType.MifareClassic
                ),
                SupportedCard(
                        imageResId = R.drawable.seqgo_card,
                        name = SeqGoTransitInfo.NAME,
                        locationResId = R.string.location_brisbane_seq_australia,
                        cardType = CardType.MifareClassic,
                        keysRequired = true,
                        preview = true,
                        extraNoteResId = R.string.seqgo_card_note
                ),
                SupportedCard(
                        imageResId = R.drawable.hsl_card,
                        name = "HSL",
                        locationResId = R.string.location_helsinki_finland,
                        cardType = CardType.MifareDesfire
                ),
                SupportedCard(
                        imageResId = R.drawable.manly_fast_ferry_card,
                        name = ManlyFastFerryTransitInfo.NAME,
                        locationResId = R.string.location_sydney_australia,
                        cardType = CardType.MifareClassic,
                        keysRequired = true
                ),
                SupportedCard(
                        imageResId = R.drawable.myki_card,
                        name = MykiTransitInfo.NAME,
                        locationResId = R.string.location_victoria_australia,
                        cardType = CardType.MifareDesfire,
                        keysRequired = false,
                        preview = false,
                        extraNoteResId = R.string.myki_card_note
                ),
                SupportedCard(
                        imageResId = R.drawable.nets_card,
                        name = "NETS FlashPay",
                        locationResId = R.string.location_singapore,
                        cardType = CardType.CEPAS
                ),
                SupportedCard(
                        imageResId = R.drawable.opal_card,
                        name = OpalTransitInfo.NAME,
                        locationResId = R.string.location_sydney_australia,
                        cardType = CardType.MifareDesfire
                ),
                SupportedCard(
                        imageResId = R.drawable.ovchip_card,
                        name = "OV-chipkaart",
                        locationResId = R.string.location_the_netherlands,
                        cardType = CardType.MifareClassic,
                        keysRequired = true
                ),
                SupportedCard(
                        imageResId = R.drawable.easycard,
                        name = "EasyCard",
                        locationResId = R.string.easycard_card_location,
                        cardType = CardType.MifareClassic,
                        keysRequired = true,
                        extraNoteResId = R.string.easycard_card_note
                ),
                SupportedCard(
                        imageResId = R.drawable.kmt_card,
                        name = "Kartu Multi Trip",
                        locationResId = R.string.location_jakarta,
                        cardType = CardType.FeliCa,
                        keysRequired = false,
                        extraNoteResId = R.string.kmt_notes
                )

        )
    }

    private val recyclerView: RecyclerView by bindView(R.id.recycler)

    init {
        inflate(context, R.layout.screen_help, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = SupportedCardsAdapter(context, SUPPORTED_CARDS)
    }

    internal class SupportedCardsAdapter(
        private val context: Context,
        private val supportedCards: List<SupportedCard>
    )
        : RecyclerView.Adapter<SupportedCardViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupportedCardViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            return SupportedCardViewHolder(layoutInflater.inflate(R.layout.item_supported_card, parent, false))
        }

        override fun onBindViewHolder(holder: SupportedCardViewHolder, position: Int) {
            holder.bind(context, supportedCards[position])
        }

        override fun getItemCount(): Int = supportedCards.size
    }

    internal class SupportedCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textViewName: TextView by bindView(R.id.card_name)
        private val textViewLocation: TextView by bindView(R.id.card_location)
        private val textViewNote: TextView by bindView(R.id.card_note)
        private val imageView: ImageView by bindView(R.id.card_image)
        private val imageViewSecure: ImageView by bindView(R.id.card_secure)
        private val viewNotSupported: View by bindView(R.id.card_not_supported)

        init {
            imageViewSecure.setOnClickListener {
                Toast.makeText(imageViewSecure.context, R.string.keys_required, Toast.LENGTH_SHORT).show()
            }
        }

        fun bind(context: Context, supportedCard: SupportedCard) {
            textViewName.text = supportedCard.name
            textViewLocation.setText(supportedCard.locationResId)
            imageView.setImageResource(supportedCard.imageResId)

            imageViewSecure.visibility = if (supportedCard.keysRequired) View.VISIBLE else View.GONE

            val notes = getNotes(context, supportedCard)
            if (notes != null) {
                textViewNote.text = notes
                textViewNote.visibility = View.VISIBLE
            } else {
                textViewNote.text = null
                textViewNote.visibility = View.GONE
            }

            viewNotSupported.visibility = if (isCardSupported(context, supportedCard)) View.GONE else View.VISIBLE
        }

        private fun getNotes(context: Context, supportedCard: SupportedCard): String? {
            val notes = ArrayList<String>()
            val extraNoteResId = supportedCard.extraNoteResId
            if (extraNoteResId != null) {
                notes.add(context.getString(extraNoteResId))
            }
            if (supportedCard.preview) {
                notes.add(context.getString(R.string.card_experimental))
            }
            if (supportedCard.cardType == CardType.CEPAS) {
                notes.add(context.getString(R.string.card_not_compatible))
            }
            if (!notes.isEmpty()) {
                return notes.joinToString(" ")
            }
            return null
        }

        private fun isCardSupported(context: Context, supportedCard: SupportedCard): Boolean {
            if (NfcAdapter.getDefaultAdapter(context) == null) {
                return true
            }
            val supportsMifareClassic = context.packageManager.hasSystemFeature("com.nxp.mifare")
            if (supportedCard.cardType == CardType.MifareClassic && !supportsMifareClassic) {
                return false
            }
            return true
        }
    }

    data class SupportedCard(
        @get:DrawableRes val imageResId: Int,
        val name: String,
        @get:StringRes val locationResId: Int,
        val cardType: CardType,
        val keysRequired: Boolean = false,
        val preview: Boolean = false,
        @get:StringRes val extraNoteResId: Int? = null
    )
}
