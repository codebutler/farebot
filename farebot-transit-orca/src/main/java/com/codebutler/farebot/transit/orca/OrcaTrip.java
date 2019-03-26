/*
 * OrcaTrip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2014 Kramer Campbell <kramer@kramerc.com>
 * Copyright (C) 2015 Sean CyberKitsune McClenaghan <cyberkitsune09@gmail.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright (C) 2018 Karl Koscher <supersat@cs.washington.edu>
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

package com.codebutler.farebot.transit.orca;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.desfire.DesfireRecord;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

@AutoValue
public abstract class OrcaTrip extends Trip {

    private static final Map<Long, Station> LINK_STATIONS = ImmutableMap.<Long, Station>builder()
            .put(10352L, Station.create("Capitol Hill Station", "Capitol Hill", "47.6192", "-122.3202"))
            .put(10351L, Station.create("University of Washington Station", "UW Station", "47.6496", "-122.3037"))
            .put(13193L, Station.create("Westlake Station", "Westlake", "47.6113968", "-122.337502"))
            .put(13194L, Station.create("University Street Station", "University Street", "47.6072502", "-122.335754"))
            .put(13195L, Station.create("Pioneer Square Station", "Pioneer Sq", "47.6021461", "-122.33107"))
            .put(13196L, Station.create("International District Station", "ID", "47.5976601", "-122.328217"))
            .put(13197L, Station.create("Stadium Station", "Stadium", "47.5918121", "-122.327354"))
            .put(13198L, Station.create("SODO Station", "SODO", "47.5799484", "-122.327515"))
            .put(13199L, Station.create("Beacon Hill Station", "Beacon Hill", "47.5791245", "-122.311287"))
            .put(13200L, Station.create("Mount Baker Station", "Mount Baker", "47.5764389", "-122.297737"))
            .put(13201L, Station.create("Columbia City Station", "Columbia City", "47.5589523", "-122.292343"))
            .put(13202L, Station.create("Othello Station", "Othello", "47.5375366", "-122.281471"))
            .put(13203L, Station.create("Rainier Beach Station", "Rainier Beach", "47.5222626", "-122.279579"))
            .put(13204L, Station.create("Tukwila International Blvd Station", "Tukwila", "47.4642754", "-122.288391"))
            .put(13205L, Station.create("Seatac Airport Station", "Sea-Tac", "47.4445305", "-122.297012"))
            .put(10353L, Station.create("Angle Lake Station", "Angle Lake", "47.4227143", "-122.2978669"))
            .build();

    private static Map<Integer, Station> sSounderStations = ImmutableMap.<Integer, Station>builder()
            .put(1, Station.create("Everett Station", "Everett", "47.9747155", "-122.1996922"))
            .put(2, Station.create("Edmonds Station", "Edmonds", "47.8109946","-122.3864407"))
            .put(3, Station.create("King Street Station", "King Street", "47.598445", "-122.330161"))
            .put(4, Station.create("Tuwkila Station", "Tukwila", "47.4603283", "-122.2421456"))
            .put(5, Station.create("Kent Station", "Kent", "47.384257", "-122.233151"))
            .put(6, Station.create("Auburn Station", "Auburn", "47.3065191", "-122.2343063"))
            .put(7, Station.create("Sumner Station", "Sumner", "47.2016577", "-122.2467547"))
            .put(8, Station.create("Puyallup Station", "Puyallup", "47.1926213", "-122.2977392"))
            .put(9, Station.create("Tacoma Dome Station", "Tacoma Dome", "47.2408695", "-122.4278904"))
            .put(0x1e01, Station.create("Mukilteo Station", "Mukilteo", "47.9491683", "-122.3010919"))
            .put(0x1e02, Station.create("Lakewood Station", "Lakewood", "47.1529884", "-122.5015344"))
            .put(0x37e5, Station.create("South Tacoma Station", "South Tacoma", "47.2038608", "-122.4877278"))
            .build();

    private static Map<Integer, Station> sWSFTerminals = ImmutableMap.<Integer, Station>builder()
            .put(10101, Station.create("Seattle Terminal", "Seattle", "47.602722", "-122.338512"))
            .put(10103, Station.create("Bainbridge Island Terminal", "Bainbridge", "47.62362", "-122.51082"))
            .put(10104, Station.create("Fauntleroy Terminal", "Seattle", "47.5231", "-122.39602"))
            .put(10115, Station.create("Anacortes Terminal", "Anacortes", "48.5065077", "-122.680434"))
            .build();

    private static final Map<Integer, Station> BRT_STATIONS = ImmutableMap.<Integer, Station>builder()
            .put(8101, Station.create("Bellevue Transit Ctr", "Bell TC", "47.6155475013397", "-122.195381419512"))
            .put(8102, Station.create("NE 8th & 124th NE EB", "8th & 124th EB", "47.6174257912468", "-122.1759606331"))
            .put(8103, Station.create("NE 8th & 140th NE EB", "8th & 140th EB", "47.617073832799",
                "-122.153093218803")) //Renamed from 104th
            .put(8104, Station.create("156th NE & NE 10th NB", "156th & 10th NB", "47.6183213583588",
                "-122.132316827774"))
            .put(8105, Station.create("156th NE & NE 15th NB", "156th & 15th NB", "47.6241102105962",
                "-122.132300734519"))
            .put(8106, Station.create("NE 24th & Bel-Red WB", "24th & Bel-Red WB", "47.631649", "-122.134445"))
            .put(8107, Station.create("156th NE at OTC NB", "156th @ OTC NB", "47.643988463442", "-122.132188081741"))
            .put(8108, Station.create("148th NE & NE 51st NB", "148th & 51st NB", "47.6557368980797",
                "-122.143093943595"))
            .put(8109, Station.create("Redmond Transit Ctr", "Redmond TC", "47.6766509387843", "-122.124935003843"))
            .put(8110, Station.create("148th NE & NE 87th SB", "148th & 87th SB", "47.6807680373983",
                "-122.145234346389"))
            .put(8111, Station.create("148th NE & NE Old Redmond Rd SB", "148th & Old Redmond SB", "47.667126850434",
                "-122.143295109272"))
            .put(8112, Station.create("148th NE & NE 51st SB", "148th & 51st SB", "47.6546637118338",
                "-122.143464088439"))
            .put(8113, Station.create("156th NE at OTC SB", "156th @ OTC SB", "47.6447799666965", "-122.132397294044"))
            .put(8114, Station.create("156th NE & NE 24th SB", "156th & 24th SB", "47.630863663919",
                "-122.132450938224"))
            .put(8115, Station.create("156th NE & NE 15th SB", "156th & 15th SB", "47.6241752908626",
                "-122.132493853569"))
            .put(8116, Station.create("156th NE & NE 10th SB", "156th & 10th SB", "47.618153215345",
                "-122.132515311241"))
            .put(8117, Station.create("NE 8th & 140th NE WB", "8th & 140th WB", "47.6172835638151",
                "-122.154402136802"))
            .put(8118, Station.create("NE 8th & 124th NE WB", "8th & 124th WB", "47.6172732395513", "-122.17461282307"))
            .put(8130, Station.create("SW Avalon & SW Bradford SB", "Avalon & Bradford SB", "47.5688667", "-122.37085"))
            .put(8131, Station.create("SW Avalon & SW Yancy NB", "Avalon & Yancy NB", "47.5678825", "-122.370674"))
            .put(8132, Station.create("35th SW & SW Avalon SB", "35th & Avalon SB", "47.56354143475",
                "-122.376263737678"))
            .put(8133, Station.create("35th SW & SW Avalon NB", "35th & Avalon NB", "47.5637097533135",
                "-122.376100122928"))
            .put(8134, Station.create("SW Alaska & Fauntleroy SW WB", "Alaska & Fauntleroy WB", "47.5611541487228",
                "-122.380509674549"))
            .put(8135, Station.create("SW Alaska & Fauntleroy SW EB", "Alaska & Fauntleroy EB", "47.5610165913989",
                "-122.380869090557"))
            .put(8136, Station.create("SW Alaska & California Ave WB", "Alaska & California WB", "47.5612030176851",
                "-122.387078404426"))
            .put(8137, Station.create("SW Alaska & California Ave EB", "Alaska & California EB", "47.5610546006955",
                "-122.386871874332"))
            .put(8138, Station.create("California SW & SW Findlay SB", "California & Findlay SB", "47.5518645981645",
                "-122.38714814186"))
            .put(8139, Station.create("California SW & SW Findlay NB", "California & Findlay NB", "47.5523696647784",
                "-122.386933565139"))
            .put(8140, Station.create("Fauntleroy SW & California SW WB", "Fauntleroy & California WB",
                "47.5448655755196", "-122.387681901454"))
            .put(8141, Station.create("California SW & Fauntleroy SW NB", "California & Fauntleroy NB",
                "47.545310962797", "-122.387107908725"))
            .put(8142, Station.create("Fauntleroy Ferry SB", "Fauntleroy Ferry SB", "47.5230070769314",
                "-122.393074482679"))
            .put(8143, Station.create("Fauntleroy Ferry NB", "Fauntleroy Ferry NB", "47.5231782426048",
                "-122.392898797988"))
            .put(8144, Station.create("SW Barton & 35th Ave SW WB", "SW Barton & 35th WB", "47.5211387084209",
                "-122.377245426177"))
            .put(8145, Station.create("SW Barton & 26th Ave SW EB", "SW Barton & 26th EB", "47.5209648190673",
                "-122.367358803749"))
            .put(8146, Station.create("3rd & Pike SB", "3rd & Pike SB", "47.609081", "-122.337327399999"))
            .put(8147, Station.create("3rd & Pike NB", "3rd & Pike NB", "47.6102046770867", "-122.33808517456"))
            .put(8148, Station.create("3rd & Seneca SB", "3rd & Seneca SB", "47.6064306874019", "-122.334909439086"))
            .put(8149, Station.create("3rd & Seneca NB", "3rd & Seneca NB", "47.6070220300827", "-122.335150837898"))
            .put(8150, Station.create("Seneca & 2nd EB", "Seneca & 2nd EB", "47.6068773597082", "-122.335641682147"))
            .put(8151, Station.create("3rd & Columbia SB", "3rd & Columbia SB", "47.6037994026769", "-122.3325034976"))
            .put(8152, Station.create("3rd & Columbia NB", "3rd & Columbia NB", "47.60419184323", "-122.332589328289"))
            .put(8153, Station.create("3rd & Marion SB", "3rd & Marion SB", "47.6044233275213", "-122.333064079284"))
            .put(8154, Station.create("Prefontaine & Yesler NB", "Prefontaine & Yesler NB", "47.6015893829318",
                "-122.329794466495"))
            .put(8160, Station.create("15th NW & NW 85 NB", "15th & 85 NB", "47.6911774", "-122.376709"))
            .put(8161, Station.create("15th NW & NW 85 SB", "15th & 85 SB", "47.6901610986643", "-122.376902103424"))
            .put(8162, Station.create("15th NW & NW 70 SB", "15th & 70 SB", "47.6790380273793", "-122.376880645751"))
            .put(8163, Station.create("15th NW & NW 65 NB", "15th & 65 NB", "47.6798759517131", "-122.376671433448"))
            .put(8164, Station.create("15th NW & NW 65 SB", "15th & 65 SB", "47.6761413059686", "-122.376821637153"))
            .put(8165, Station.create("15th NW & NW 60 SB", "15th & 60 SB", "47.6724389029691", "-122.37631201744"))
            .put(8166, Station.create("15th NW & NW Market NB", "15th & Market NB", "47.6684725252118",
                "-122.376118898391"))
            .put(8167, Station.create("15th NW & NW Market SB", "15th & Market SB", "47.6689096362649",
                "-122.37631201744"))
            .put(8168, Station.create("15th NW & NW Leary NB", "15th & Leary NB", "47.6634183885893",
                "-122.376043796539"))
            .put(8169, Station.create("15th NW & NW Leary SB", "15th & Leary SB", "47.663143811041",
                "-122.376483678817"))
            .put(8170, Station.create("15th W & W Dravus NB", "15th & Dravus NB", "47.6491601240274",
                "-122.376097440719"))
            .put(8171, Station.create("15th W & W Dravus SB", "15th & Dravus SB", "47.6481373973892",
                "-122.376875281333"))
            .put(8172, Station.create("Elliot W & W Prospect NB", "Elliot & Prospect NB", "47.6290163123735",
                "-122.371414303779"))
            .put(8173, Station.create("3rd Ave & Vine NB", "3rd & Vine NB", "47.6168767571982", "-122.348282933235"))
            .put(8174, Station.create("3rd Ave & Cedar SB", "3rd & Cedar SB", "47.6168821813073", "-122.348701357841"))
            .put(8175, Station.create("3rd Ave & Bell NB", "3rd & Bell NB", "47.6149855168781", "-122.34508305788"))
            .put(8176, Station.create("3rd Ave & Bell SB", "3rd & Bell SB", "47.614444891634", "-122.344514429569"))
            .put(8177, Station.create("3rd Ave & Virginia NB", "3rd & Virginia NB", "47.6128229823651",
                "-122.341424524784"))
            .put(8178, Station.create("3rd Ave & Virginia SB", "3rd & Virginia SB", "47.6124450720877",
                "-122.341145575046"))
            .put(8179, Station.create("W Mercer & 3rd W EB", "Mercer & 3rd EB", "47.6245684242042",
                "-122.360917254804"))
            .put(8180, Station.create("Mercer & Queen Anne WB", "Mercer & Queen Anne WB", "47.6246489281384",
                "-122.356552183628"))
            .put(8181, Station.create("Queen Anne & Mercer SB", "Queen Anne & Mercer SB", "47.6243063550421",
                "-122.356819063425"))
            .put(8182, Station.create("1st N & Republican NB", "1st & Republican NB", "47.6231882314514",
                "-122.355315685272"))
            .put(8183, Station.create("Queen Anne & W John SB", "Queen Anne & John SB", "47.6192144960556",
                "-122.356849908828"))
            .put(8184, Station.create("1st N & Denny NB", "1st & Denny NB", "47.6188766663707", "-122.355371862061"))
            .put(8185, Station.create("15 NW & NW 80 SB", "15 & 80 SB", "47.6860624506418", "-122.376896739006"))
            .put(8186, Station.create("15 NW & NW 75 SB", "15 & 75 SB", "47.683072220525", "-122.376928925514"))
            .put(8187, Station.create("15 W & W Emerson SB", "15 & Emerson SB", "47.653473", "-122.376328"))
            .put(8188, Station.create("15 W & W Armory SB", "15 & Armory SB", "47.637283696861", "-122.376408576965"))
            .put(8189, Station.create("W Mercer & 3rd WB", "Mercer & 3rd WB", "47.6246679096956", "-122.361153513193"))
            .put(8190, Station.create("Burien TC Bay 6 SB", "Burien TC Bay 6", "47.4694099", "-122.337318"))
            .put(8191, Station.create("Tukwila International Blvd Station", "TIBS", "47.464098",
            "-122.288176099999")) //Renamed from Tukwila LLR/S154 WB
            .put(8192, Station.create("Southcenter & 62nd Ave S EB", "Southcenter & 62nd EB", "47.462814",
                "-122.257118"))
            .put(8193, Station.create("Andover & Baker SB", "Andover & Baker SB", "47.457881071895",
                "-122.254480719566"))
            .put(8194, Station.create("Tukwila Sounder Station", "Tukwila Sounder Station", "47.4604308295467",
            "-122.241101861")) //Renamed from Tukwila Comm Rail/Bay EB, TODO: Compare to 8200
            .put(8195, Station.create("Rainier & S 7th NB", "Rainier & 7th NB", "47.4741712774955",
                "-122.215502858161"))
            .put(8196, Station.create("Renton TC Bay 2", "Renton TC Bay 2", "47.480546", "-122.2085766"))
            .put(8197, Station.create("Park Ave N & Garden Ave N WB", "Park & Garden WB", "47.5005871",
            "-122.2012435")) //Renamed from N 10/Garden Ave N WB
            .put(8198, Station.create("Renton TC Bay 3", "Renton TC Bay 3", "47.480546", "-122.2085766"))
            .put(8199, Station.create("S 7th & Rainier WB", "7th & Rainier WB", "47.4738730438368",
                "-122.217086702585"))
            .put(8200, Station.create("Tukwila Sounder Station", "Tukwila Sounder Station", "47.4607898893259",
            "-122.24082827568")) //Renamed from Tukwila Rail/Bat WB, TODO: Compare to 8194
            .put(8201, Station.create("Andover & Baker NB", "Andover & Baker NB", "47.4587152878659",
                "-122.254276871681"))
            .put(8220, Station.create("Aurora Village TC", "Aurora Village TC", "47.7745224987306",
                "-122.341010123491"))
            .put(8221, Station.create("Aurora N & N 192 SB", "Aurora & 192 SB", "47.7672855442468",
            "-122.346104979515")) //Renamed from Shoreline P&R
            .put(8222, Station.create("Aurora N & N 185 SB", "Aurora & 185 SB", "47.7628503952128",
                "-122.346185445785"))
            .put(8223, Station.create("Aurora N & N 175 SB", "Aurora & 175 SB", "47.7552989544991",
                "-122.345858216285"))
            .put(8224, Station.create("Aurora N & N 160 SB", "Aurora & 160 SB", "47.7484425665415",
                "-122.345691919326"))
            .put(8225, Station.create("Aurora N & N 145 SB", "Aurora & 145 SB", "47.7335725349705",
                "-122.345268130302"))
            .put(8226, Station.create("Aurora N & N 135 SB", "Aurora & 135 SB", "47.7264318233991",
                "-122.345117926597"))
            .put(8227, Station.create("Aurora N & N 130 SB", "Aurora & 130 SB", "47.7229639493678",
                "-122.345080375671"))
            .put(8228, Station.create("Aurora N & N 125 SB", "Aurora & 125 SB", "47.7199036564968",
                "-122.345048189163"))
            .put(8229, Station.create("Aurora N & N 115 SB", "Aurora & 115 SB", "47.7118803608749",
                "-122.344914078712"))
            .put(8230, Station.create("Aurora N & N 105 SB", "Aurora & 105 SB", "47.7047303996993",
                "-122.344817118649"))
            .put(8231, Station.create("Aurora N & N 100 SB", "Aurora & 100 SB", "47.7010904555273",
                "-122.344812154769"))
            .put(8232, Station.create("Aurora N & N 95 SB", "Aurora & 95 SB", "47.6974945275512", "-122.344704866409"))
            .put(8233, Station.create("Aurora N & N 90 SB", "Aurora & 90 SB", "47.6939886200535", "-122.345026731491"))
            .put(8234, Station.create("Aurora N & N 85 SB", "Aurora & 85 SB", "47.6904969207454", "-122.344737052917"))
            .put(8235, Station.create("Aurora N & N 76 SB", "Aurora & 76 SB", "47.6841520456182", "-122.344651222229"))
            .put(8236, Station.create("Aurora N & N 65 SB", "Aurora & 65 SB", "47.6763363521107", "-122.346984744071"))
            .put(8237, Station.create("Aurora N & N 46 SB", "Aurora & 46 SB", "47.6616733514361", "-122.347488999366"))
            //.put(8238, Station.create("Aurora N & Harrison SB", "Aurora & Harrison SB", "", ""))
            .put(8239, Station.create("Wall St & 5th Ave WB", "Wall & 5th WB", "47.6175626", "-122.3451438"))
            .put(8240, Station.create("Aurora N & Denny NB", "Aurora & Denny NB", "47.6180899355761",
                "-122.343460321426"))
            //.put(8241, Station.create("Aurora N & Harrison NB", "Aurora & Harrison NB", "", ""))
            .put(8242, Station.create("Aurora N & N 46 NB", "Aurora & 46 NB", "47.6616878033291", "-122.347161769866"))
            .put(8243, Station.create("Aurora N & N 85 NB", "Aurora & 85 NB", "47.6908941268671", "-122.344361543655"))
            .put(8244, Station.create("Aurora N & N 91 NB", "Aurora & 91 NB", "47.6946927089615", "-122.344409823417"))
            .put(8245, Station.create("Aurora N & N 100 NB", "Aurora & 100 NB", "47.7016753119282",
                "-122.344490289688"))
            .put(8246, Station.create("Aurora N & N Northgate Way NB", "Aurora & NGate NB", "47.7053539804587",
                "-122.344543933868"))
            .put(8247, Station.create("Aurora N & N 130 NB", "Aurora & 130 NB", "47.7234619517184",
                "-122.344881892204"))
            .put(8248, Station.create("Aurora N & N 135 NB", "Aurora & 135 NB", "47.7273014621554",
                "-122.344924807548"))
            .put(8249, Station.create("Aurora N & N 145 NB", "Aurora & 145 NB", "47.7349255155068",
                "-122.344914078712"))
            .put(8250, Station.create("Aurora N & N 160 NB", "Aurora & 160 NB", "47.7490665676696",
                "-122.345337867736"))
            .put(8251, Station.create("Aurora N & N 185 NB", "Aurora & 185 NB", "47.7636761548606",
                "-122.345804572105"))
            .put(8301, Station.create("Blanchard & 6th EB", "Blanchard & 6th EB", "47.615844358198",
                "-122.340904176235"))
            .put(8302, Station.create("Westlake & 9th NB", "Westlake & 9th NB", "47.6180953595593", "-122.33830243349"))
            .put(8303, Station.create("Valley & Fairview SB", "Valley & Fairview SB", "47.6260390829929",
                "-122.333831191062"))
            .put(8304, Station.create("Westlake & Mercer SB", "Westlake & Mercer SB", "47.624065919813",
                "-122.33858205378"))
            .put(8305, Station.create("Westlake & Harrison SB", "Westlake & Harrison SB", "47.6214526971832",
                "-122.338578701019"))
            .put(8306, Station.create("Westlake & 9th SB", "Westlake & 9th SB", "47.6179615678097", "-122.33851969242"))
            //.put(8307, Station.create("Lenora & 7th WB", "Lenora & 7th WB", "", ""))
            .put(13161, Station.create("238th St NB", "238th St NB", "47.7835264994018", "-122.343159914016"))
            .put(13162, Station.create("238th St SB", "238th St SB", "47.7828416337193", "-122.344018220901"))
            .put(13163, Station.create("Gateway at 216th NB", "Gateway at 216th NB", "47.8035350116184",
                "-122.328729629516"))
            .put(13164, Station.create("216th St SB", "216th St SB", "47.8028468063936", "-122.329609394073"))
            .put(13165, Station.create("Heron at 200th NB", "Heron at 200th NB", "47.8178339250236",
                "-122.317839860916"))
            .put(13166, Station.create("Crossroads at 196 SB", "Crossroads at 196 SB", "47.8208344317007",
                "-122.315602898597"))
            .put(13167, Station.create("Cherry Hill & 176 NB", "Cherry Hill & 176 NB", "47.8396277775522",
                "-122.298973202705"))
            .put(13168, Station.create("International 174 SB", "International 174 SB", "47.8408231764694",
                "-122.298310697078"))
            .put(13169, Station.create("148th St NB", "148th St NB", "47.8647739557922", "-122.281265258789"))
            .put(13170, Station.create("148th St SB", "148th St SB", "47.8638022450146", "-122.282488346099"))
            .put(13171, Station.create("Lincoln Way NB", "Lincoln Way NB", "47.8733566098851", "-122.273218631744"))
            .put(13172, Station.create("Lincoln Way SB", "Lincoln Way SB", "47.8739467275835", "-122.273030877113"))
            .put(13173, Station.create("Airport Rd NB", "Airport Rd NB", "", ""))
            .put(13174, Station.create("Airport Rd SB", "Airport Rd SB", "", ""))
            .put(13175, Station.create("4th Ave NB", "4th Ave NB", "47.9097046662584", "-122.239256501197"))
            .put(13176, Station.create("4th Ave SB", "4th Ave SB", "47.9099815401481", "-122.239401340484"))
            .put(13177, Station.create("Casino Rd NB", "Casino Rd NB", "47.9211703205365", "-122.22852230072"))
            .put(13178, Station.create("Casino Rd SB", "Casino Rd SB", "47.9212997395726", "-122.228833436965"))
            .put(13179, Station.create("50th St NB", "50th St NB", "47.9520886693373", "-122.213426828384"))
            .put(13180, Station.create("50th St SB", "50th St SB", "47.9516790852515", "-122.213920354843"))
            .put(13181, Station.create("40th St NB", "40th St NB", "47.9649134757321", "-122.210680246353"))
            .put(13182, Station.create("41st St SB", "41st St SB", "47.9628803994514", "-122.210948467254"))
            .put(13183, Station.create("Colby Ave EB", "Colby Ave EB", "47.9764710352963", "-122.207987308502"))
            .put(13184, Station.create("Wetmore Ave WB", "Wetmore Ave WB", "47.9766793205954", "-122.206785678863"))
            .put(13185, Station.create("Aurora Village TC", "Aurora Village TC", "47.7742926694779",
                "-122.34112009406"))
            .put(13186, Station.create("Everett Station Bay 1", "Everett Station", "47.9747867778208",
                "-122.197498146052"))
            .put(13187, Station.create("Peck Drive SB", "Peck Drive SB", "47.9414096794754", "-122.217804193496"))
            .put(13188, Station.create("Merrill Creek Test & Training Station", "Merrill Creek", "47.9339669026849",
                "-122.251898540424"))
            .put(13189, Station.create("Madison Drive NB", "Madison Drive NB", "47.9365293796851", "-122.218716144561"))
            .put(13190, Station.create("112th SB", "112th SB", "47.8961108540724", "-122.252415418624"))
            .put(13191, Station.create("112th NB", "112th NB", "47.8968301928371", "-122.251262068748"))
            .put(13206, Station.create("Tukwila Light Rail Station Bay 1", "TIBS Bay 1", "47.464098",
                "-122.288176099999"))
            .put(13207, Station.create("S 176th St SB", "176th St SB", "47.4453208950317", "-122.296457290649"))
            .put(13208, Station.create("S 176th St NB", "176th St NB", "47.4456165697685", "-122.296097874641"))
            .put(13209, Station.create("S 182nd St SB", "182nd St SB", "47.439793466891", "-122.296183705329"))
            .put(13210, Station.create("S 180nd St NB", "180nd St NB", "47.442456573361", "-122.295926213264"))
            .put(13211, Station.create("S 188th St SB", "188th St SB", "47.4340205212797", "-122.295679450035"))
            .put(13212, Station.create("S 188th St NB", "188th St NB", "47.4349821180936", "-122.295405864715"))
            .put(13213, Station.create("S 200th St SB", "200th St SB", "47.4223783629138", "-122.296580672264"))
            .put(13214, Station.create("S 200th St NB", "200th St NB", "47.423274842546", "-122.296092510223"))
            .put(13215, Station.create("S 208th ST SB", "208th ST SB", "47.4148066750236", "-122.297546267509"))
            .put(13216, Station.create("S 208th St NB", "208th St NB", "47.4157722528427", "-122.297047376632"))
            .put(13217, Station.create("S 216th St SB", "216th St SB", "47.4077566944469", "-122.2984957695"))
            .put(13218, Station.create("S 216th St NB", "216th St NB", "47.4088458364992", "-122.297943234443"))
            .put(13219, Station.create("Kent Des Moines SB", "Kent Des Moines SB", "47.3926225507716",
                "-122.29517519474"))
            .put(13220, Station.create("Kent Des Moines NB", "Kent Des Moines NB", "47.3945399764362",
                "-122.294644117355"))
            .put(13221, Station.create("S 240th St SB", "240th St SB", "47.3857911548532", "-122.296838164329"))
            .put(13222, Station.create("S 240th St NB", "240th St NB", "47.3862742119375", "-122.296333909034"))
            .put(13223, Station.create("S 260th St SB", "260th St SB", "47.3680166617334", "-122.304321527481"))
            .put(13224, Station.create("S 260th St NB", "260th St NB", "47.3694735745929", "-122.303146719932"))
            .put(13225, Station.create("S 272 ST SB", "272 ST SB", "47.3573265591089", "-122.309975624084"))
            .put(13226, Station.create("S 272nd St NB", "272nd St NB", "47.3588746142789", "-122.308688163757"))
            .put(13227, Station.create("S 288th St SB", "288th St SB", "47.3429776333081", "-122.312496900558"))
            .put(13228, Station.create("S 288th St NB", "288th St NB", "47.3437736865565", "-122.312081158161"))
            .put(13229, Station.create("S 312th St SB", "312th St SB", "47.3220905786881", "-122.313564419746"))
            .put(13230, Station.create("S 312th St NB", "312th St NB", "47.322810583296", "-122.313183546066"))
            //.put(13251, Station.create("Hwy 99 at 204th Street SB", "Hwy 99 at 204th Street SB", "", ""))
            .put(13300, Station.create("Canyon Park P&R", "Canyon Park", "47.794477", "-122.21132"))
            .put(13301, Station.create("220th St NB", "220th St NB", "47.798884", "-122.211686"))
            .put(13302, Station.create("208th St NB", "208th St NB", "47.810137", "-122.207465"))
            .put(13303, Station.create("208th St SB", "208th St SB", "47.808963", "-122.207691"))
            .put(13304, Station.create("196th St NB", "196th St NB", "47.820552", "-122.207352"))
            .put(13305, Station.create("196th St SB", "196th St SB", "47.820017", "-122.20756"))
            .put(13306, Station.create("180th St NB", "180th St NB", "47.834921", "-122.210835"))
            .put(13308, Station.create("164th St NB", "164th St NB", "47.84995", "-122.217593"))
            .put(13309, Station.create("164th St SB", "164th St SB", "47.849244", "-122.217507"))
            .put(13310, Station.create("153rd St SE NB", "153rd St SE", "47.859515", "-122.21885"))
            .put(13311, Station.create("153rd St SE SB", "153rd St SE", "47.858997", "-122.218977"))
            .put(13314, Station.create("16th Ave NB", "16th Ave NB", "47.878283", "-122.211737"))
            .put(13315, Station.create("16th Ave SB", "16th Ave SB", "47.878031", "-122.211423"))
            .put(13317, Station.create("Dumas Rd SB", "Dumas Rd SB", "47.858997", "-122.218977"))
            .put(13318, Station.create("3rd Ave E NB", "3rd Ave E NB", "47.882058", "-122.228468"))
            .put(13319, Station.create("3rd Ave E SB", "3rd Ave E SB", "47.881837", "-122.227572"))
            .put(13320, Station.create("4th Ave W NB", "4th Ave W NB", "47.882002", "-122.239239"))
            .put(13321, Station.create("4th Ave W SB", "4th Ave W SB", "47.881767", "-122.239202"))
            .put(13322, Station.create("Gibson Road NB", "Gibson Road NB", "47.881869", "-122.249814"))
            .put(13324, Station.create("Hwy 99 NB", "Hwy 99 NB", "47.890108", "-122.258833"))
            .put(13325, Station.create("Hwy 99 SB", "Hwy 99 SB", "47.888753", "-122.257978"))
            .put(13326, Station.create("112th Street NB", "112th Street NB", "47.896822", "-122.263692"))
            .put(13327, Station.create("112th Street SB", "112th Street SB", "47.896243", "-122.263481"))
            .put(13330, Station.create("Kasch Park NB", "Kasch Park NB", "47.918626", "-122.271586"))
            .put(13331, Station.create("Kasch Park SB", "Kasch Park SB", "47.917958", "-122.271781"))
            .put(13332, Station.create("Seaway Transit Center", "Seaway TC", "47.930117", "-122.259936")) //#1
            .put(13333, Station.create("Seaway Transit Center", "Seaway TC", "47.930117", "-122.259936")) //#2
            .put(13838, Station.create("Federal Way TC Bay 7", "FWTC Bay 7", "47.3175545", "-122.304787"))
            .put(13839, Station.create("Z Line (Test Location)", "Z Line", "47.4955968",
            "-122.285024799999")) //No idea where this actually is
            .build();

    private static final Map<Integer, Station> SEATTLE_STREETCAR_STATIONS = ImmutableMap.<Integer, Station>builder()
            .put(9056, Station.create("14th Ave S & Washington", "14th & Washington", "47.6005639165279",
                "-122.314181327819"))
            .put(9065, Station.create("Broadway & E Denny NB", "Broadway & Denny NB", "47.6181568313295",
                "-122.320747375488"))
            .put(9066, Station.create("Broadway & E Denny SB", "Broadway & Denny SB", "47.6180158077495",
                "-122.320980727672"))
            //.put(9067, Station.create("Broadway & Harrison NB", "Broadway & Harrison NB", "", ""))
            //.put(9068, Station.create("Broadway & Harrison SB", "Broadway & Harrison SB", "", ""))
            .put(9061, Station.create("Broadway & Marion NB", "Broadway & Marion NB", "47.6098502555259",
                "-122.320717871189"))
            .put(9062, Station.create("Broadway & Marion SB", "Broadway & Marion SB", "47.6098086650832",
                "-122.320913672447"))
            .put(9064, Station.create("Broadway & Pine NB", "Broadway & Pine NB", "47.6150723056943",
                "-122.320758104324"))
            .put(9063, Station.create("Broadway & Pine SB", "Broadway & Pine SB", "47.6161933149567",
            "-122.320924401283")) //Renamed from Pike
            //.put(9070, Station.create("Broadway & Prospect", "Broadway & Prospect", "", ""))
            //.put(9069, Station.create("Broadway & Roy", "Broadway & Roy", "", ""))
            .put(9059, Station.create("Broadway & Terrace NB", "Broadway & Terrace NB", "47.6054649941559",
                "-122.320712506771"))
            .put(9060, Station.create("Broadway & Terrace SB", "Broadway & Terrace SB", "47.6049785239516",
                "-122.320937812328"))
            .put(9057, Station.create("E Yesler Wy & Broadway EB", "Yesler & Broadway EB", "47.6016327884738",
                "-122.32006072998"))
            .put(9058, Station.create("E Yesler Wy & Broadway WB", "Yesler & Broadway WB", "47.6017431107308",
                "-122.320216298103"))
            .put(9007, Station.create("Fairview & Aloha", "Fairview & Aloha", "47.627573814398",
            "-122.332401573657")) //Renamed from Fairview & Campus
            .put(9006, Station.create("Lake Union Park", "Lake Union Park", "47.6259030928503", "-122.336444975041"))
            .put(9001, Station.create("McGraw Square", "McGraw Square", "47.6129802934938", "-122.337363660335"))
            .put(9051, Station.create("Occidental S & S Jackson", "Occidental & Jackson", "47.5991973", "-122.3332461"))
            .put(9055, Station.create("S Jackson & 12th Ave S", "Jackson & 12th Ave", "47.599210161578",
            "-122.315737009048")) //Renamed from 13th
            .put(9052, Station.create("S Jackson & 5th Ave S EB", "Jackson & 5th Ave EB", "47.5992065442833",
                "-122.326776981353"))
            .put(9053, Station.create("S Jackson & 5th Ave S WB", "Jackson & 5th Ave WB", "47.5992065442833",
                "-122.326776981353"))
            .put(9054, Station.create("S Jackson & 7th Ave S", "Jackson & 7th Ave", "47.5992002140171",
                "-122.323275357484"))
            .put(9005, Station.create("Terry & Republican NB", "Terry & Republican NB", "47.6234169199518",
            "-122.337044477462")) //Renamed from Terry & Mercer NB
            .put(9004, Station.create("Terry & Thomas NB", "Terry & Thomas NB", "47.6213677268386",
                "-122.337264418601"))
            .put(9002, Station.create("Westlake & 7th NB", "Westlake & 7th NB", "47.6154700859254",
                "-122.337763309478"))
            .put(9003, Station.create("Westlake & Denny NB", "Westlake & Denny NB", "47.6180953595593",
                "-122.33830243349"))
            .put(9008, Station.create("Westlake & Mercer SB", "Westlake & Mercer SB", "47.624065919813",
                "-122.33858205378"))
            .put(9010, Station.create("Westlake & 9th SB", "Westlake & 9th SB", "47.6179615678097", "-122.33851969242"))
            .put(9009, Station.create("Westlake & Thomas SB", "Westlake & Thomas SB", "47.6214526971832",
                "-122.338578701019"))
            .build();

    @NonNull
    static OrcaTrip create(@NonNull DesfireRecord record) {
        byte[] useData = record.getData().bytes();
        long[] usefulData = new long[useData.length];

        for (int i = 0; i < useData.length; i++) {
            usefulData[i] = ((long) useData[i]) & 0xFF;
        }

        long timestamp = ((0x0F & usefulData[3]) << 28)
                | (usefulData[4] << 20)
                | (usefulData[5] << 12)
                | (usefulData[6] << 4)
                | (usefulData[7] >> 4);

        long ftpType = ((usefulData[7] & 0xf) << 4) | ((usefulData[8] & 0xf0) >> 4);
        long coachNumber = ((usefulData[8] & 0xf) << 20) | (usefulData[9] << 12)
                | (usefulData[10] << 4) | ((usefulData[11] & 0xf0) >> 4);

        long fare;
        if (usefulData[15] == 0xFF || usefulData[16] == 0x02) {
            // FIXME: This appears to be some sort of special case for transfers and passes.
            fare = 0;
        } else {
            fare = (usefulData[15] << 7) | (usefulData[16] >> 1);
        }

        long newBalance = (usefulData[34] << 8) | usefulData[35];
        long agency = usefulData[3] >> 4;
        long transType = (usefulData[17]);

        // For tap outs, fare is the amount refunded to the card
        if (transType == OrcaData.TRANS_TYPE_TAP_OUT) {
            fare = -fare;
        }

        // Check to see if a pass use is also a tap off so that the trips can be combined
        if (transType == OrcaData.TRANS_TYPE_PASS_USE && usefulData[25] == 0x0F) {
            transType = OrcaData.TRANS_TYPE_TAP_OUT;
        }

        return new AutoValue_OrcaTrip(timestamp, agency, transType, ftpType, coachNumber, fare, newBalance);
    }

    @Override
    public long getExitTimestamp() {
        return 0;
    }

    @Override
    public String getAgencyName(@NonNull Resources resources) {
        switch ((int) getAgency()) {
            case OrcaTransitInfo.AGENCY_CT:
                return resources.getString(R.string.transit_orca_agency_ct);
            case OrcaTransitInfo.AGENCY_KCM:
                // The King County Water Taxi is now a separate agency but uses KCM's agency ID
                if (getFTPType() == OrcaTransitInfo.FTP_TYPE_WATER_TAXI) {
                    return resources.getString(R.string.transit_orca_agency_kcwt);
                } else {
                    return resources.getString(R.string.transit_orca_agency_kcm);
                }
            case OrcaTransitInfo.AGENCY_PT:
                return resources.getString(R.string.transit_orca_agency_pt);
            case OrcaTransitInfo.AGENCY_ST:
                return resources.getString(R.string.transit_orca_agency_st);
            case OrcaTransitInfo.AGENCY_WSF:
                return resources.getString(R.string.transit_orca_agency_wsf);
            case OrcaTransitInfo.AGENCY_ET:
                return resources.getString(R.string.transit_orca_agency_et);
            case OrcaTransitInfo.AGENCY_KT:
                return resources.getString(R.string.transit_orca_agency_kt);
        }
        return resources.getString(R.string.transit_orca_agency_unknown, Long.toString(getAgency()));
    }

    @Override
    public String getShortAgencyName(@NonNull Resources resources) {
        switch ((int) getAgency()) {
            case OrcaTransitInfo.AGENCY_CT:
                return "CT";
            case OrcaTransitInfo.AGENCY_KCM:
                if (getFTPType() == OrcaTransitInfo.FTP_TYPE_WATER_TAXI) {
                    return "KCWT";
                } else {
                    return "KCM";
                }
            case OrcaTransitInfo.AGENCY_PT:
                return "PT";
            case OrcaTransitInfo.AGENCY_ST:
                return "ST";
            case OrcaTransitInfo.AGENCY_WSF:
                return "WSF";
            case OrcaTransitInfo.AGENCY_ET:
                return "ET";
            case OrcaTransitInfo.AGENCY_KT:
                return "KT";
        }
        return resources.getString(R.string.transit_orca_agency_unknown, Long.toString(getAgency()));
    }

    @Override
    public String getRouteName(@NonNull Resources resources) {
        if (isLink()) {
            return resources.getString(R.string.transit_orca_route_link);
        } else if (isSounder()) {
            return resources.getString(R.string.transit_orca_route_sounder);
        } else {
            // FIXME: Need to find bus route #s
            if (getAgency() == OrcaTransitInfo.AGENCY_ST) {
                return resources.getString(R.string.transit_orca_route_express_bus);
            } else if (getAgency() == OrcaTransitInfo.AGENCY_KCM) {
                switch ((int)getFTPType()) {
                    case OrcaTransitInfo.FTP_TYPE_BUS:
                        return resources.getString(R.string.transit_orca_route_bus);
                    case OrcaTransitInfo.FTP_TYPE_WATER_TAXI:
                        return resources.getString(R.string.transit_orca_route_water_taxi);
                    case OrcaTransitInfo.FTP_TYPE_BRT:
                        return resources.getString(R.string.transit_orca_route_brt);
                }
            }
            return null;
        }
    }

    @Override
    public String getFareString(@NonNull Resources resources) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(getFare() / 100.0);
    }

    @Override
    public boolean hasFare() {
        return true;
    }

    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format(getNewBalance() / 100);
    }

    @Override
    public Station getStartStation() {
        if (isLink()) {
            return LINK_STATIONS.get(getCoachNumber());
        } else if (isSounder()) {
            return sSounderStations.get((int) getCoachNumber());
        } else if (getAgency() == OrcaTransitInfo.AGENCY_WSF) {
            return sWSFTerminals.get((int) getCoachNumber());
        } else if (isSeattleStreetcar()) {
            return SEATTLE_STREETCAR_STATIONS.get((int) getCoachNumber());
        } else if (isRapidRide() || isSwift()) {
            return BRT_STATIONS.get((int) getCoachNumber());
        }
        return null;
    }

    @Override
    public String getStartStationName(@NonNull Resources resources) {
        if (isLink()) {
            if (LINK_STATIONS.containsKey(getCoachNumber())) {
                return LINK_STATIONS.get(getCoachNumber()).getStationName();
            } else {
                return resources.getString(R.string.transit_orca_station_unknown_station,
                        Long.toString(getCoachNumber()));
            }
        } else if (isSounder()) {
            int stationNumber = (int) getCoachNumber();
            if (sSounderStations.containsKey(stationNumber)) {
                return sSounderStations.get(stationNumber).getStationName();
            } else {
                return resources.getString(R.string.transit_orca_station_unknown_station,
                        Integer.toString(stationNumber));
            }
        } else if (getAgency() == OrcaTransitInfo.AGENCY_WSF) {
            int terminalNumber = (int) getCoachNumber();
            if (sWSFTerminals.containsKey(terminalNumber)) {
                return sWSFTerminals.get(terminalNumber).getStationName();
            } else {
                return resources.getString(R.string.transit_orca_station_unknown_terminal,
                        Integer.toString(terminalNumber));
            }
        } else if (isRapidRide() || isSwift()) {
            int stationNumber = (int) getCoachNumber();
            if (BRT_STATIONS.containsKey(stationNumber)) {
                return BRT_STATIONS.get(stationNumber).getStationName();
            } else {
                return resources.getString(R.string.transit_orca_station_unknown_station,
                        Integer.toString(stationNumber));
            }
        } else if (isSeattleStreetcar()) {
            int stationNumber = (int) getCoachNumber();
            if (SEATTLE_STREETCAR_STATIONS.containsKey(stationNumber)) {
                return SEATTLE_STREETCAR_STATIONS.get(stationNumber).getStationName();
            } else {
                return resources.getString(R.string.transit_orca_station_unknown_station,
                        Integer.toString(stationNumber));
            }
        } else if (getFTPType() == OrcaTransitInfo.FTP_TYPE_BUS) {
            return resources.getString(R.string.transit_orca_station_coach,
                    Long.toString(getCoachNumber()));
        } else {
            return resources.getString(R.string.transit_orca_station_unknown_location,
                    Long.toString(getCoachNumber()));
        }
    }

    @Override
    public String getEndStationName(@NonNull Resources resources) {
        // ORCA tracks destination in a separate record
        return null;
    }

    @Override
    public Station getEndStation() {
        // ORCA tracks destination in a separate record
        return null;
    }

    @Override
    public Mode getMode() {
        if (isLink()) {
            return Mode.METRO;
        } else if (isSounder()) {
            return Mode.TRAIN;
        } else if (getFTPType() == OrcaTransitInfo.FTP_TYPE_FERRY
                || getFTPType() == OrcaTransitInfo.FTP_TYPE_WATER_TAXI) {
            return Mode.FERRY;
        } else if (isSeattleStreetcar()) {
            return Mode.TRAM;
        } else {
            return Mode.BUS;
        }
    }

    @Override
    public boolean hasTime() {
        return true;
    }

    private boolean isLink() {
        return (getAgency() == OrcaTransitInfo.AGENCY_ST
                && getFTPType() == OrcaTransitInfo.FTP_TYPE_LINK);
    }

    private boolean isSounder() {
        return (getAgency() == OrcaTransitInfo.AGENCY_ST
                && getFTPType() == OrcaTransitInfo.FTP_TYPE_SOUNDER);
    }

    private boolean isRapidRide() {
        return (getAgency() == OrcaTransitInfo.AGENCY_KCM
                && getFTPType() == OrcaTransitInfo.FTP_TYPE_BRT);
    }

    private boolean isSeattleStreetcar() {
        return getFTPType() == OrcaTransitInfo.FTP_TYPE_STREETCAR; //TODO: Find agency ID
    }

    private boolean isSwift() {
        return (getAgency() == OrcaTransitInfo.AGENCY_CT
                && getFTPType() == OrcaTransitInfo.FTP_TYPE_BRT);
    }

    abstract long getAgency();

    abstract long getTransType();

    abstract long getFTPType();

    abstract long getCoachNumber();

    abstract long getFare();

    abstract long getNewBalance();
}
