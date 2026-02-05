package com.codebutler.farebot.shared.serialize

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.cepas.raw.RawCEPASCard
import com.codebutler.farebot.card.classic.raw.RawClassicCard
import com.codebutler.farebot.card.desfire.raw.RawDesfireCard
import com.codebutler.farebot.card.felica.raw.RawFelicaCard
import com.codebutler.farebot.card.iso7816.raw.RawISO7816Card
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.card.ultralight.raw.RawUltralightCard
import com.codebutler.farebot.card.vicinity.raw.RawVicinityCard
import com.codebutler.farebot.shared.sample.RawSampleCard
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class KotlinxCardSerializer(private val json: Json) : CardSerializer {

    override fun serialize(card: RawCard<*>): String {
        val cardType = card.cardType()
        val jsonElement = when (cardType) {
            CardType.MifareDesfire -> json.encodeToJsonElement(RawDesfireCard.serializer(), card as RawDesfireCard)
            CardType.MifareClassic -> json.encodeToJsonElement(RawClassicCard.serializer(), card as RawClassicCard)
            CardType.MifareUltralight -> json.encodeToJsonElement(RawUltralightCard.serializer(), card as RawUltralightCard)
            CardType.CEPAS -> json.encodeToJsonElement(RawCEPASCard.serializer(), card as RawCEPASCard)
            CardType.FeliCa -> json.encodeToJsonElement(RawFelicaCard.serializer(), card as RawFelicaCard)
            CardType.ISO7816 -> json.encodeToJsonElement(RawISO7816Card.serializer(), card as RawISO7816Card)
            CardType.Vicinity -> json.encodeToJsonElement(RawVicinityCard.serializer(), card as RawVicinityCard)
            CardType.Sample -> json.encodeToJsonElement(RawSampleCard.serializer(), card as RawSampleCard)
        }
        val jsonObject = buildJsonObject {
            put("cardType", cardType.name)
            jsonElement.jsonObject.forEach { (key, value) -> put(key, value) }
        }
        return json.encodeToString(JsonObject.serializer(), jsonObject)
    }

    override fun deserialize(data: String): RawCard<*> {
        val jsonObject = json.decodeFromString(JsonObject.serializer(), data)
        val cardTypeName = jsonObject["cardType"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing cardType field")
        val cardType = CardType.valueOf(cardTypeName)
        val contentJson = JsonObject(jsonObject.filterKeys { it != "cardType" })
        val contentString = json.encodeToString(JsonObject.serializer(), contentJson)
        return when (cardType) {
            CardType.MifareDesfire -> json.decodeFromString(RawDesfireCard.serializer(), contentString)
            CardType.MifareClassic -> json.decodeFromString(RawClassicCard.serializer(), contentString)
            CardType.MifareUltralight -> json.decodeFromString(RawUltralightCard.serializer(), contentString)
            CardType.CEPAS -> json.decodeFromString(RawCEPASCard.serializer(), contentString)
            CardType.FeliCa -> json.decodeFromString(RawFelicaCard.serializer(), contentString)
            CardType.ISO7816 -> json.decodeFromString(RawISO7816Card.serializer(), contentString)
            CardType.Vicinity -> json.decodeFromString(RawVicinityCard.serializer(), contentString)
            CardType.Sample -> json.decodeFromString(RawSampleCard.serializer(), contentString)
        }
    }
}
