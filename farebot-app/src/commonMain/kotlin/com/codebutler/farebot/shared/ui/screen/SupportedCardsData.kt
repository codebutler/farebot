package com.codebutler.farebot.shared.ui.screen

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_app.generated.resources.*
import farebot.farebot_app.generated.resources.Res

/** All supported cards across all platforms. */
val ALL_SUPPORTED_CARDS: List<CardInfo> = listOf(
    // North America - USA
    CardInfo(Res.string.card_name_orca, CardType.MifareDesfire, TransitRegion.USA, Res.string.card_location_seattle_wa, imageRes = Res.drawable.orca_card, latitude = 47.6062f, longitude = -122.3321f, sampleDumpFile = "ORCA.nfc"),
    CardInfo(Res.string.card_name_clipper, CardType.MifareDesfire, TransitRegion.USA, Res.string.card_location_san_francisco_ca, extraNoteRes = Res.string.card_note_clipper, imageRes = Res.drawable.clipper_card, latitude = 37.7749f, longitude = -122.4194f, sampleDumpFile = "Clipper.nfc"),
    CardInfo(Res.string.card_name_charlie_card, CardType.MifareClassic, TransitRegion.USA, Res.string.card_location_boston_ma, imageRes = Res.drawable.charlie_card, latitude = 42.3601f, longitude = -71.0589f),
    CardInfo(Res.string.card_name_lax_tap, CardType.MifareClassic, TransitRegion.USA, Res.string.card_location_los_angeles_ca, imageRes = Res.drawable.laxtap_card, latitude = 34.0522f, longitude = -118.2437f, sampleDumpFile = "LaxTap.json"),
    CardInfo(Res.string.card_name_msp_goto, CardType.MifareClassic, TransitRegion.USA, Res.string.card_location_minneapolis_mn, imageRes = Res.drawable.msp_goto_card, latitude = 44.9778f, longitude = -93.2650f, sampleDumpFile = "MspGoTo.json"),
    CardInfo(Res.string.card_name_ventra, CardType.MifareUltralight, TransitRegion.USA, Res.string.card_location_chicago_il, extraNoteRes = Res.string.card_note_ventra, imageRes = Res.drawable.ventra, latitude = 41.8781f, longitude = -87.6298f, sampleDumpFile = "Ventra.json"),
    CardInfo(Res.string.card_name_holo, CardType.MifareDesfire, TransitRegion.USA, Res.string.card_location_oahu_hawaii, serialOnly = true, imageRes = Res.drawable.holo_card, latitude = 21.3069f, longitude = -157.8583f, sampleDumpFile = "Holo.json"),
    CardInfo(Res.string.card_name_trimet_hop, CardType.MifareDesfire, TransitRegion.USA, Res.string.card_location_portland_or, serialOnly = true, imageRes = Res.drawable.trimethop_card, latitude = 45.5152f, longitude = -122.6784f),
    CardInfo(Res.string.card_name_sun_card, CardType.MifareClassic, TransitRegion.USA, Res.string.card_location_orlando_fl, serialOnly = true, imageRes = Res.drawable.suncard, latitude = 28.5383f, longitude = -81.3792f),

    // North America - Canada
    CardInfo(Res.string.card_name_compass, CardType.MifareUltralight, TransitRegion.CANADA, Res.string.card_location_vancouver_canada, extraNoteRes = Res.string.card_note_compass, imageRes = Res.drawable.yvr_compass_card, latitude = 49.2827f, longitude = -123.1207f, sampleDumpFile = "Compass.json"),
    CardInfo(Res.string.card_name_opus, CardType.ISO7816, TransitRegion.CANADA, Res.string.card_location_montreal_canada, imageRes = Res.drawable.opus_card, latitude = 45.5017f, longitude = -73.5673f),
    CardInfo(Res.string.card_name_presto, CardType.MifareDesfire, TransitRegion.CANADA, Res.string.card_location_ontario_canada, serialOnly = true, imageRes = Res.drawable.presto_card, latitude = 43.6532f, longitude = -79.3832f),

    // South America
    CardInfo(Res.string.card_name_bilhete_unico, CardType.MifareClassic, TransitRegion.BRAZIL, Res.string.card_location_sao_paulo_brazil, imageRes = Res.drawable.bilheteunicosp_card, latitude = -23.5505f, longitude = -46.6333f),
    CardInfo(Res.string.card_name_bip, CardType.MifareClassic, TransitRegion.CHILE, Res.string.card_location_santiago_chile, imageRes = Res.drawable.chilebip, latitude = -33.4489f, longitude = -70.6693f),

    // Europe - UK & Ireland
    CardInfo(Res.string.card_name_oyster, CardType.MifareClassic, TransitRegion.UK, Res.string.card_location_london_uk, extraNoteRes = Res.string.card_note_oyster, imageRes = Res.drawable.oyster_card, latitude = 51.5074f, longitude = -0.1278f),
    CardInfo(Res.string.card_name_leap, CardType.MifareDesfire, TransitRegion.IRELAND, Res.string.card_location_dublin_ireland, extraNoteRes = Res.string.card_note_leap, imageRes = Res.drawable.leap_card, latitude = 53.3498f, longitude = -6.2603f),

    // Europe - Benelux
    CardInfo(Res.string.card_name_ov_chipkaart, CardType.MifareClassic, TransitRegion.NETHERLANDS, Res.string.card_location_the_netherlands, keysRequired = true, imageRes = Res.drawable.ovchip_card, latitude = 52.3676f, longitude = 4.9041f),
    CardInfo(Res.string.card_name_mobib, CardType.ISO7816, TransitRegion.BELGIUM, Res.string.card_location_brussels_belgium, imageRes = Res.drawable.mobib_card, latitude = 50.8503f, longitude = 4.3517f, sampleDumpFile = "Mobib.json"),

    // Europe - France (Intercode)
    CardInfo(Res.string.card_name_navigo, CardType.ISO7816, TransitRegion.FRANCE, Res.string.card_location_paris_france, imageRes = Res.drawable.navigo, latitude = 48.8566f, longitude = 2.3522f),
    CardInfo(Res.string.card_name_oura, CardType.ISO7816, TransitRegion.FRANCE, Res.string.card_location_grenoble_france, imageRes = Res.drawable.oura, latitude = 45.1885f, longitude = 5.7245f),
    CardInfo(Res.string.card_name_pastel, CardType.ISO7816, TransitRegion.FRANCE, Res.string.card_location_toulouse_france, preview = true, imageRes = Res.drawable.pastel, latitude = 43.6047f, longitude = 1.4442f),
    CardInfo(Res.string.card_name_pass_pass, CardType.ISO7816, TransitRegion.FRANCE, Res.string.card_location_hauts_de_france, preview = true, imageRes = Res.drawable.passpass, latitude = 50.6292f, longitude = 3.0573f),
    CardInfo(Res.string.card_name_transgironde, CardType.ISO7816, TransitRegion.FRANCE, Res.string.card_location_gironde_france, preview = true, imageRes = Res.drawable.transgironde, latitude = 44.8378f, longitude = -0.5792f),
    CardInfo(Res.string.card_name_tam, CardType.ISO7816, TransitRegion.FRANCE, Res.string.card_location_montpellier_france, imageRes = Res.drawable.tam_montpellier, latitude = 43.6108f, longitude = 3.8767f),
    CardInfo(Res.string.card_name_korrigo, CardType.ISO7816, TransitRegion.FRANCE, Res.string.card_location_brittany_france, imageRes = Res.drawable.korrigo, latitude = 48.1173f, longitude = -1.6778f),
    CardInfo(Res.string.card_name_envibus, CardType.ISO7816, TransitRegion.FRANCE, Res.string.card_location_sophia_antipolis_france, imageRes = Res.drawable.envibus, latitude = 43.6163f, longitude = 7.0552f),

    // Europe - Iberia & Italy
    CardInfo(Res.string.card_name_bonobus, CardType.MifareClassic, TransitRegion.SPAIN, Res.string.card_location_cadiz_spain, imageRes = Res.drawable.cadizcard, latitude = 36.5271f, longitude = -6.2886f),
    CardInfo(Res.string.card_name_ricaricami, CardType.MifareClassic, TransitRegion.ITALY, Res.string.card_location_milan_italy, imageRes = Res.drawable.ricaricami, latitude = 45.4642f, longitude = 9.1900f),
    CardInfo(Res.string.card_name_venezia_unica, CardType.ISO7816, TransitRegion.ITALY, Res.string.card_location_venice_italy, imageRes = Res.drawable.veneziaunica, latitude = 45.4408f, longitude = 12.3155f),
    CardInfo(Res.string.card_name_carta_mobile, CardType.ISO7816, TransitRegion.ITALY, Res.string.card_location_pisa_italy, imageRes = Res.drawable.cartamobile, latitude = 43.7228f, longitude = 10.4017f),
    CardInfo(Res.string.card_name_lisboa_viva, CardType.ISO7816, TransitRegion.PORTUGAL, Res.string.card_location_lisbon_portugal, imageRes = Res.drawable.lisboaviva, latitude = 38.7223f, longitude = -9.1393f),

    // Europe - Scandinavia & Finland
    CardInfo(Res.string.card_name_hsl, CardType.MifareDesfire, TransitRegion.FINLAND, Res.string.card_location_helsinki_finland, extraNoteRes = Res.string.card_note_hsl, imageRes = Res.drawable.hsl_card, latitude = 60.1699f, longitude = 24.9384f, sampleDumpFile = "HSL.json"),
    CardInfo(Res.string.card_name_waltti, CardType.MifareDesfire, TransitRegion.FINLAND, Res.string.card_location_finland, imageRes = Res.drawable.waltti_logo, latitude = 61.4978f, longitude = 23.7610f),
    CardInfo(Res.string.card_name_tampere, CardType.MifareDesfire, TransitRegion.FINLAND, Res.string.card_location_tampere_finland, imageRes = Res.drawable.tampere, latitude = 61.4978f, longitude = 23.7610f),
    CardInfo(Res.string.card_name_slaccess, CardType.MifareClassic, TransitRegion.SWEDEN, Res.string.card_location_stockholm_sweden, keysRequired = true, keyBundle = "slaccess", preview = true, imageRes = Res.drawable.slaccess, latitude = 59.3293f, longitude = 18.0686f),
    CardInfo(Res.string.card_name_rejsekort, CardType.MifareClassic, TransitRegion.DENMARK, Res.string.card_location_denmark, keysRequired = true, keyBundle = "rejsekort", preview = true, imageRes = Res.drawable.rejsekort, latitude = 55.6761f, longitude = 12.5683f),
    CardInfo(Res.string.card_name_vasttrafik, CardType.MifareClassic, TransitRegion.SWEDEN, Res.string.card_location_gothenburg_sweden, keysRequired = true, keyBundle = "gothenburg", preview = true, imageRes = Res.drawable.vasttrafik, latitude = 57.7089f, longitude = 11.9746f),
    // Europe - Eastern Europe
    CardInfo(Res.string.card_name_warsaw, CardType.MifareClassic, TransitRegion.POLAND, Res.string.card_location_warsaw_poland, keysRequired = true, imageRes = Res.drawable.warsaw_card, latitude = 52.2297f, longitude = 21.0122f),
    CardInfo(Res.string.card_name_tartu_bus, CardType.MifareClassic, TransitRegion.ESTONIA, Res.string.card_location_tartu_estonia, imageRes = Res.drawable.tartu, latitude = 58.3780f, longitude = 26.7290f),

    // Europe - Russia & Former USSR
    CardInfo(Res.string.card_name_troika, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_moscow_russia, extraNoteRes = Res.string.card_note_russia, imageRes = Res.drawable.troika_card, latitude = 55.7558f, longitude = 37.6173f, sampleDumpFile = "Troika.json"),
    CardInfo(Res.string.card_name_podorozhnik, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_saint_petersburg_russia, extraNoteRes = Res.string.card_note_russia, imageRes = Res.drawable.podorozhnik_card, latitude = 59.9343f, longitude = 30.3351f),
    CardInfo(Res.string.card_name_strelka, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_moscow_region_russia, serialOnly = true, imageRes = Res.drawable.strelka_card, latitude = 55.7558f, longitude = 37.6173f),
    CardInfo(Res.string.card_name_kazan, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_kazan_russia, keysRequired = true, imageRes = Res.drawable.kazan, latitude = 55.7963f, longitude = 49.1089f),
    CardInfo(Res.string.card_name_yargor, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_yaroslavl_russia, imageRes = Res.drawable.yargor, latitude = 57.6261f, longitude = 39.8845f),
    // Umarsh variants
    CardInfo(Res.string.card_name_yoshkar_ola_transport_card, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_yoshkar_ola_russia, keysRequired = true, preview = true, imageRes = Res.drawable.yoshkar_ola, latitude = 56.6346f, longitude = 47.8998f),
    CardInfo(Res.string.card_name_strizh, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_izhevsk_russia, keysRequired = true, preview = true, imageRes = Res.drawable.strizh, latitude = 56.8519f, longitude = 53.2114f),
    CardInfo(Res.string.card_name_electronic_barnaul, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_barnaul_russia, keysRequired = true, preview = true, imageRes = Res.drawable.barnaul, latitude = 53.3548f, longitude = 83.7698f),
    CardInfo(Res.string.card_name_siticard_vladimir, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_vladimir_russia, keysRequired = true, preview = true, imageRes = Res.drawable.siticard_vladimir, latitude = 56.1290f, longitude = 40.4066f),
    CardInfo(Res.string.card_name_kirov_transport_card, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_kirov_russia, keysRequired = true, preview = true, imageRes = Res.drawable.kirov, latitude = 58.6036f, longitude = 49.6680f),
    CardInfo(Res.string.card_name_siticard, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_nizhniy_novgorod_russia, keysRequired = true, preview = true, imageRes = Res.drawable.siticard, latitude = 56.2965f, longitude = 43.9361f),
    CardInfo(Res.string.card_name_omka, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_omsk_russia, keysRequired = true, preview = true, imageRes = Res.drawable.omka, latitude = 54.9885f, longitude = 73.3242f),
    CardInfo(Res.string.card_name_penza_transport_card, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_penza_russia, keysRequired = true, preview = true, imageRes = Res.drawable.penza, latitude = 53.1959f, longitude = 45.0184f),
    CardInfo(Res.string.card_name_ekarta, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_yekaterinburg_russia, keysRequired = true, preview = true, imageRes = Res.drawable.ekarta, latitude = 56.8389f, longitude = 60.6057f),
    // Crimea
    CardInfo(Res.string.card_name_crimea_trolleybus_card, CardType.MifareClassic, TransitRegion.Crimea, Res.string.card_location_crimea, keysRequired = true, preview = true, imageRes = Res.drawable.crimea_trolley, latitude = 44.9521f, longitude = 34.1024f),
    CardInfo(Res.string.card_name_parus_school_card, CardType.MifareClassic, TransitRegion.Crimea, Res.string.card_location_crimea, keysRequired = true, preview = true, imageRes = Res.drawable.parus_school, latitude = 44.9521f, longitude = 34.1024f),
    // Zolotaya Korona variants
    CardInfo(Res.string.card_name_zolotaya_korona, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_russia, keysRequired = true, preview = true, imageRes = Res.drawable.zolotayakorona, latitude = 55.0084f, longitude = 82.9357f),
    CardInfo(Res.string.card_name_krasnodar_etk, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_krasnodar_russia, keysRequired = true, preview = true, imageRes = Res.drawable.krasnodar_etk, latitude = 45.0355f, longitude = 38.9753f),
    CardInfo(Res.string.card_name_orenburg_ekg, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_orenburg_russia, keysRequired = true, preview = true, imageRes = Res.drawable.orenburg_ekg, latitude = 51.7727f, longitude = 55.0988f),
    CardInfo(Res.string.card_name_samara_etk, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_samara_russia, keysRequired = true, preview = true, imageRes = Res.drawable.samara_etk, latitude = 53.1959f, longitude = 50.1001f),
    CardInfo(Res.string.card_name_yaroslavl_etk, CardType.MifareClassic, TransitRegion.RUSSIA, Res.string.card_location_yaroslavl_russia, keysRequired = true, preview = true, imageRes = Res.drawable.yaroslavl_etk, latitude = 57.6261f, longitude = 39.8845f),
    // Georgia
    CardInfo(Res.string.card_name_metromoney, CardType.MifareClassic, TransitRegion.GEORGIA, Res.string.card_location_tbilisi_georgia, imageRes = Res.drawable.metromoney, latitude = 41.7151f, longitude = 44.8271f),
    // Ukraine
    CardInfo(Res.string.card_name_kyiv_metro, CardType.MifareClassic, TransitRegion.UKRAINE, Res.string.card_location_kyiv_ukraine, extraNoteRes = Res.string.card_note_kiev, imageRes = Res.drawable.kiev, latitude = 50.4501f, longitude = 30.5234f),
    CardInfo(Res.string.card_name_kyiv_digital, CardType.MifareClassic, TransitRegion.UKRAINE, Res.string.card_location_kyiv_ukraine, imageRes = Res.drawable.kiev_digital, latitude = 50.4501f, longitude = 30.5234f),

    // Europe - Switzerland
    CardInfo(Res.string.card_name_tpf, CardType.MifareDesfire, TransitRegion.SWITZERLAND, Res.string.card_location_fribourg_switzerland, serialOnly = true, imageRes = Res.drawable.tpf_card, latitude = 46.8065f, longitude = 7.1620f),

    // Middle East & Africa
    CardInfo(Res.string.card_name_ravkav, CardType.ISO7816, TransitRegion.ISRAEL, Res.string.card_location_israel, imageRes = Res.drawable.ravkav_card, latitude = 32.0853f, longitude = 34.7818f),
    CardInfo(Res.string.card_name_metro_q, CardType.MifareClassic, TransitRegion.QATAR, Res.string.card_location_qatar, imageRes = Res.drawable.metroq, latitude = 25.2854f, longitude = 51.5310f),
    CardInfo(Res.string.card_name_nol, CardType.MifareDesfire, TransitRegion.UAE, Res.string.card_location_dubai_uae, serialOnly = true, imageRes = Res.drawable.nol, latitude = 25.2048f, longitude = 55.2708f),
    CardInfo(Res.string.card_name_hafilat, CardType.MifareDesfire, TransitRegion.UAE, Res.string.card_location_abu_dhabi_uae, extraNoteRes = Res.string.card_note_adelaide, imageRes = Res.drawable.hafilat, latitude = 24.4539f, longitude = 54.3773f),
    CardInfo(Res.string.card_name_istanbul_kart, CardType.MifareDesfire, TransitRegion.TURKEY, Res.string.card_location_istanbul_turkey, serialOnly = true, imageRes = Res.drawable.istanbulkart_card, latitude = 41.0082f, longitude = 28.9784f),
    CardInfo(Res.string.card_name_gautrain, CardType.MifareClassic, TransitRegion.SOUTH_AFRICA, Res.string.card_location_gauteng_south_africa, imageRes = Res.drawable.gautrain, latitude = -26.2041f, longitude = 28.0473f),

    // Asia - Japan
    CardInfo(Res.string.card_name_suica, CardType.FeliCa, TransitRegion.JAPAN, Res.string.card_location_tokyo_japan, imageRes = Res.drawable.suica_card, latitude = 35.6762f, longitude = 139.6503f, sampleDumpFile = "Suica.nfc"),
    CardInfo(Res.string.card_name_pasmo, CardType.FeliCa, TransitRegion.JAPAN, Res.string.card_location_tokyo_japan, imageRes = Res.drawable.pasmo_card, latitude = 35.6762f, longitude = 139.6503f, sampleDumpFile = "PASMO.nfc"),
    CardInfo(Res.string.card_name_icoca, CardType.FeliCa, TransitRegion.JAPAN, Res.string.card_location_kansai_japan, imageRes = Res.drawable.icoca_card, latitude = 34.6937f, longitude = 135.5023f, sampleDumpFile = "ICOCA.nfc"),
    CardInfo(Res.string.card_name_toica, CardType.FeliCa, TransitRegion.JAPAN, Res.string.card_location_nagoya_japan, imageRes = Res.drawable.toica, latitude = 35.1815f, longitude = 136.9066f),
    CardInfo(Res.string.card_name_manaca, CardType.FeliCa, TransitRegion.JAPAN, Res.string.card_location_nagoya_japan, imageRes = Res.drawable.manaca, latitude = 35.1815f, longitude = 136.9066f),
    CardInfo(Res.string.card_name_pitapa, CardType.FeliCa, TransitRegion.JAPAN, Res.string.card_location_kansai_japan, imageRes = Res.drawable.pitapa, latitude = 34.6937f, longitude = 135.5023f),
    CardInfo(Res.string.card_name_kitaca, CardType.FeliCa, TransitRegion.JAPAN, Res.string.card_location_hokkaido_japan, imageRes = Res.drawable.kitaca, latitude = 43.0618f, longitude = 141.3545f),
    CardInfo(Res.string.card_name_sugoca, CardType.FeliCa, TransitRegion.JAPAN, Res.string.card_location_fukuoka_japan, imageRes = Res.drawable.sugoca, latitude = 33.5904f, longitude = 130.4017f),
    CardInfo(Res.string.card_name_nimoca, CardType.FeliCa, TransitRegion.JAPAN, Res.string.card_location_fukuoka_japan, imageRes = Res.drawable.nimoca, latitude = 33.5904f, longitude = 130.4017f),
    CardInfo(Res.string.card_name_hayakaken, CardType.FeliCa, TransitRegion.JAPAN, Res.string.card_location_fukuoka_city_japan, imageRes = Res.drawable.hayakaken, latitude = 33.5904f, longitude = 130.4017f),
    CardInfo(Res.string.card_name_edy, CardType.FeliCa, TransitRegion.JAPAN, Res.string.card_location_tokyo_japan, imageRes = Res.drawable.edy_card, latitude = 35.6762f, longitude = 139.6503f),

    // Asia - Korea
    CardInfo(Res.string.card_name_t_money, CardType.ISO7816, TransitRegion.SOUTH_KOREA, Res.string.card_location_seoul_south_korea, imageRes = Res.drawable.tmoney_card, latitude = 37.5665f, longitude = 126.9780f, sampleDumpFile = "TMoney.json"),
    // Asia - China
    CardInfo(Res.string.card_name_beijing_municipal_card, CardType.ISO7816, TransitRegion.CHINA, Res.string.card_location_beijing_china, imageRes = Res.drawable.beijing, latitude = 39.9042f, longitude = 116.4074f),
    CardInfo(Res.string.card_name_shanghai_public_transportation_card, CardType.ISO7816, TransitRegion.CHINA, Res.string.card_location_shanghai_china, imageRes = Res.drawable.shanghai, latitude = 31.2304f, longitude = 121.4737f),
    CardInfo(Res.string.card_name_shenzhen_tong, CardType.ISO7816, TransitRegion.CHINA, Res.string.card_location_shenzhen_china, imageRes = Res.drawable.szt_card, latitude = 22.5431f, longitude = 114.0579f),
    CardInfo(Res.string.card_name_wuhan_tong, CardType.ISO7816, TransitRegion.CHINA, Res.string.card_location_wuhan_china, imageRes = Res.drawable.wuhantong, latitude = 30.5928f, longitude = 114.3055f),
    CardInfo(Res.string.card_name_t_union, CardType.ISO7816, TransitRegion.CHINA, Res.string.card_location_china, imageRes = Res.drawable.tunion, latitude = 39.9042f, longitude = 116.4074f),
    CardInfo(Res.string.card_name_city_union, CardType.ISO7816, TransitRegion.CHINA, Res.string.card_location_china, imageRes = Res.drawable.city_union, latitude = 39.9042f, longitude = 116.4074f),

    // Asia - Southeast Asia
    CardInfo(Res.string.card_name_octopus, CardType.FeliCa, TransitRegion.HONG_KONG, Res.string.card_location_hong_kong, imageRes = Res.drawable.octopus_card, latitude = 22.3193f, longitude = 114.1694f, sampleDumpFile = "Octopus.json"),
    CardInfo(Res.string.card_name_ez_link, CardType.CEPAS, TransitRegion.SINGAPORE, Res.string.card_location_singapore, imageRes = Res.drawable.ezlink_card, latitude = 1.3521f, longitude = 103.8198f, sampleDumpFile = "EZLink.json"),
    CardInfo(Res.string.card_name_nets_flashpay, CardType.CEPAS, TransitRegion.SINGAPORE, Res.string.card_location_singapore, imageRes = Res.drawable.nets_card, latitude = 1.3521f, longitude = 103.8198f),
    CardInfo(Res.string.card_name_touch_n_go, CardType.MifareClassic, TransitRegion.MALAYSIA, Res.string.card_location_malaysia, imageRes = Res.drawable.touchngo, latitude = 3.1390f, longitude = 101.6869f),
    CardInfo(Res.string.card_name_komuterlink, CardType.MifareClassic, TransitRegion.MALAYSIA, Res.string.card_location_malaysia, imageRes = Res.drawable.komuterlink, latitude = 3.1390f, longitude = 101.6869f),
    CardInfo(Res.string.card_name_kartu_multi_trip, CardType.FeliCa, TransitRegion.INDONESIA, Res.string.card_location_jakarta_indonesia, extraNoteRes = Res.string.card_note_kmt_felica, imageRes = Res.drawable.kmt_card, latitude = -6.2088f, longitude = 106.8456f),

    // Asia - Taiwan
    CardInfo(Res.string.card_name_easycard, CardType.MifareClassic, TransitRegion.TAIWAN, Res.string.card_location_taipei_taiwan, keysRequired = true, imageRes = Res.drawable.easycard, latitude = 25.0330f, longitude = 121.5654f, sampleDumpFile = "EasyCard.mfc"),

    // Oceania - Australia
    CardInfo(Res.string.card_name_opal, CardType.MifareDesfire, TransitRegion.AUSTRALIA, Res.string.card_location_sydney_australia, extraNoteRes = Res.string.card_note_opal, imageRes = Res.drawable.opal_card, latitude = -33.8688f, longitude = 151.2093f, sampleDumpFile = "Opal.json"),
    CardInfo(Res.string.card_name_myki, CardType.MifareDesfire, TransitRegion.AUSTRALIA, Res.string.card_location_victoria_australia, serialOnly = true, imageRes = Res.drawable.myki_card, latitude = -37.8136f, longitude = 144.9631f, sampleDumpFile = "Myki.json"),
    CardInfo(Res.string.card_name_seqgo, CardType.MifareClassic, TransitRegion.AUSTRALIA, Res.string.card_location_brisbane_and_seq_australia, keysRequired = true, imageRes = Res.drawable.seqgo_card, latitude = -27.4698f, longitude = 153.0251f, sampleDumpFile = "SeqGo.json"),
    CardInfo(Res.string.card_name_manly_fast_ferry, CardType.MifareClassic, TransitRegion.AUSTRALIA, Res.string.card_location_sydney_australia, keysRequired = true, imageRes = Res.drawable.manly_fast_ferry_card, latitude = -33.8688f, longitude = 151.2093f),
    CardInfo(Res.string.card_name_adelaide_metrocard, CardType.MifareDesfire, TransitRegion.AUSTRALIA, Res.string.card_location_adelaide_australia, extraNoteRes = Res.string.card_note_adelaide, imageRes = Res.drawable.adelaide, latitude = -34.9285f, longitude = 138.6007f),
    CardInfo(Res.string.card_name_smartrider, CardType.MifareClassic, TransitRegion.AUSTRALIA, Res.string.card_location_perth_australia, imageRes = Res.drawable.smartrider_card, latitude = -31.9505f, longitude = 115.8605f),

    // Oceania - New Zealand
    CardInfo(Res.string.card_name_at_hop, CardType.MifareDesfire, TransitRegion.NEW_ZEALAND, Res.string.card_location_auckland_new_zealand, serialOnly = true, imageRes = Res.drawable.athopcard, latitude = -36.8485f, longitude = 174.7633f),
    CardInfo(Res.string.card_name_snapper, CardType.ISO7816, TransitRegion.NEW_ZEALAND, Res.string.card_location_wellington_new_zealand, imageRes = Res.drawable.snapperplus, latitude = -41.2865f, longitude = 174.7762f),
    CardInfo(Res.string.card_name_busit, CardType.MifareClassic, TransitRegion.NEW_ZEALAND, Res.string.card_location_waikato_new_zealand, preview = true, imageRes = Res.drawable.busitcard, latitude = -37.7870f, longitude = 175.2793f),
    CardInfo(Res.string.card_name_smartride, CardType.MifareClassic, TransitRegion.NEW_ZEALAND, Res.string.card_location_rotorua_new_zealand, preview = true, imageRes = Res.drawable.rotorua, latitude = -38.1368f, longitude = 176.2497f),
    CardInfo(Res.string.card_name_metrocard, CardType.MifareClassic, TransitRegion.NEW_ZEALAND, Res.string.card_location_christchurch_new_zealand, keysRequired = true, extraNoteRes = Res.string.card_note_chc_metrocard, imageRes = Res.drawable.chc_metrocard, latitude = -43.5321f, longitude = 172.6362f),
    CardInfo(Res.string.card_name_otago_gocard, CardType.MifareClassic, TransitRegion.NEW_ZEALAND, Res.string.card_location_otago_new_zealand, imageRes = Res.drawable.otago_gocard, latitude = -45.8788f, longitude = 170.5028f),

)
