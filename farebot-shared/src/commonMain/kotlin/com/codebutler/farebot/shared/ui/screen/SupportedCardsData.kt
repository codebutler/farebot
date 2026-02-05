package com.codebutler.farebot.shared.ui.screen

import com.codebutler.farebot.card.CardType
import farebot.farebot_shared.generated.resources.Res
import farebot.farebot_shared.generated.resources.bilheteunicosp_card
import farebot.farebot_shared.generated.resources.clipper_card
import farebot.farebot_shared.generated.resources.easycard
import farebot.farebot_shared.generated.resources.edy_card
import farebot.farebot_shared.generated.resources.ezlink_card
import farebot.farebot_shared.generated.resources.hsl_card
import farebot.farebot_shared.generated.resources.icoca_card
import farebot.farebot_shared.generated.resources.kmt_card
import farebot.farebot_shared.generated.resources.manly_fast_ferry_card
import farebot.farebot_shared.generated.resources.myki_card
import farebot.farebot_shared.generated.resources.nets_card
import farebot.farebot_shared.generated.resources.octopus_card
import farebot.farebot_shared.generated.resources.opal_card
import farebot.farebot_shared.generated.resources.orca_card
import farebot.farebot_shared.generated.resources.ovchip_card
import farebot.farebot_shared.generated.resources.pasmo_card
import farebot.farebot_shared.generated.resources.seqgo_card
import farebot.farebot_shared.generated.resources.suica_card
import org.jetbrains.compose.resources.DrawableResource

data class SupportedCardInfo(
    val name: String,
    val location: String,
    val cardType: CardType,
    val keysRequired: Boolean = false,
    val preview: Boolean = false,
    val extraNote: String? = null,
    val imageRes: DrawableResource? = null,
)

/** All supported cards across all platforms. */
val ALL_SUPPORTED_CARDS = listOf(
    SupportedCardInfo("ORCA", "Seattle, WA", CardType.MifareDesfire, imageRes = Res.drawable.orca_card),
    SupportedCardInfo("Clipper", "San Francisco, CA", CardType.MifareDesfire, imageRes = Res.drawable.clipper_card),
    // Japan IC cards - all interoperable, listed in order of popularity/usage
    SupportedCardInfo("Suica", "Tokyo, Japan", CardType.FeliCa, imageRes = Res.drawable.suica_card),
    SupportedCardInfo("PASMO", "Tokyo, Japan", CardType.FeliCa, imageRes = Res.drawable.pasmo_card),
    SupportedCardInfo("ICOCA", "Kansai, Japan", CardType.FeliCa, imageRes = Res.drawable.icoca_card),
    SupportedCardInfo("TOICA", "Nagoya, Japan", CardType.FeliCa),
    SupportedCardInfo("manaca", "Nagoya, Japan", CardType.FeliCa),
    SupportedCardInfo("PiTaPa", "Kansai, Japan", CardType.FeliCa),
    SupportedCardInfo("Kitaca", "Hokkaido, Japan", CardType.FeliCa),
    SupportedCardInfo("SUGOCA", "Fukuoka, Japan", CardType.FeliCa),
    SupportedCardInfo("nimoca", "Fukuoka, Japan", CardType.FeliCa),
    SupportedCardInfo("Hayakaken", "Fukuoka City, Japan", CardType.FeliCa),
    SupportedCardInfo("Edy", "Tokyo, Japan", CardType.FeliCa, imageRes = Res.drawable.edy_card),
    SupportedCardInfo("EZ-Link", "Singapore", CardType.CEPAS, extraNote = "Not compatible with some devices.", imageRes = Res.drawable.ezlink_card),
    SupportedCardInfo("Octopus", "Hong Kong", CardType.FeliCa, imageRes = Res.drawable.octopus_card),
    SupportedCardInfo("Bilhete Único", "São Paulo, Brazil", CardType.MifareClassic, imageRes = Res.drawable.bilheteunicosp_card),
    SupportedCardInfo("SeqGo", "Brisbane and SEQ, Australia", CardType.MifareClassic, keysRequired = true, preview = true, extraNote = "Limited support. Keys required.", imageRes = Res.drawable.seqgo_card),
    SupportedCardInfo("HSL", "Helsinki, Finland", CardType.MifareDesfire, imageRes = Res.drawable.hsl_card),
    SupportedCardInfo("Manly Fast Ferry", "Sydney, Australia", CardType.MifareClassic, keysRequired = true, imageRes = Res.drawable.manly_fast_ferry_card),
    SupportedCardInfo("Myki", "Victoria, Australia", CardType.MifareDesfire, extraNote = "Serial number only.", imageRes = Res.drawable.myki_card),
    SupportedCardInfo("NETS FlashPay", "Singapore", CardType.CEPAS, extraNote = "Not compatible with some devices.", imageRes = Res.drawable.nets_card),
    SupportedCardInfo("Opal", "Sydney, Australia", CardType.MifareDesfire, imageRes = Res.drawable.opal_card),
    SupportedCardInfo("OV-chipkaart", "The Netherlands", CardType.MifareClassic, keysRequired = true, imageRes = Res.drawable.ovchip_card),
    SupportedCardInfo("EasyCard", "Taipei, Taiwan", CardType.MifareClassic, keysRequired = true, extraNote = "EasyCard is not yet fully supported.", imageRes = Res.drawable.easycard),
    SupportedCardInfo("Kartu Multi Trip", "Jakarta, Indonesia", CardType.FeliCa, extraNote = "Experimental support.", imageRes = Res.drawable.kmt_card),
)
