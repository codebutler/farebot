package com.codebutler.farebot.shared.serialize

import com.codebutler.farebot.base.util.decodeBase64
import com.codebutler.farebot.base.util.toBase64
import com.codebutler.farebot.card.felica.FeliCaIdm
import com.codebutler.farebot.card.felica.FeliCaPmm
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

object ByteArrayAsBase64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ByteArray", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ByteArray,
    ) {
        encoder.encodeString(value.toBase64())
    }

    override fun deserialize(decoder: Decoder): ByteArray = decoder.decodeString().decodeBase64()
}

object IDmSerializer : KSerializer<FeliCaIdm> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FeliCaLib.IDm", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: FeliCaIdm,
    ) {
        encoder.encodeString(value.getBytes().toBase64())
    }

    override fun deserialize(decoder: Decoder): FeliCaIdm = FeliCaIdm(decoder.decodeString().decodeBase64())
}

object PMmSerializer : KSerializer<FeliCaPmm> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FeliCaLib.PMm", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: FeliCaPmm,
    ) {
        encoder.encodeString(value.getBytes().toBase64())
    }

    override fun deserialize(decoder: Decoder): FeliCaPmm = FeliCaPmm(decoder.decodeString().decodeBase64())
}

val FareBotSerializersModule =
    SerializersModule {
        contextual(ByteArray::class, ByteArrayAsBase64Serializer)
        contextual(FeliCaIdm::class, IDmSerializer)
        contextual(FeliCaPmm::class, PMmSerializer)
    }
