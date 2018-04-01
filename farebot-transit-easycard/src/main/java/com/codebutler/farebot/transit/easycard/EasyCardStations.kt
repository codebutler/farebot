/*
 * EasyCardStations.kt
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * Based on code from http://www.fuzzysecurity.com/tutorials/rfid/4.html
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
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

package com.codebutler.farebot.transit.easycard

val EasyCardStations = mapOf(
        0x47 to "Metro Tamsui",
        0x46 to "Metro Hongshulin",
        0x45 to "Metro Zhuwei",
        0x44 to "Metro Guandu",
        0x43 to "Metro Zhongyi",
        0x41 to "Metro Xinbeitou",
        0x40 to "Metro Beitou",
        0x3f to "Metro Qiyan",
        0x3e to "Metro Qilian",
        0x3d to "Metro Shipai",
        0x3c to "Metro Mingde",
        0x3b to "Metro Zhishan",
        0x3a to "Metro Shilin",
        0x39 to "Metro Jiantan",
        0x38 to "Metro Yuanshan",
        0x37 to "Metro Minquan W. Rd.",
        0x36 to "Metro Shuanglian",
        0x35 to "Metro Zhongshan",
        0x53 to "Metro Xinpu",
        0x54 to "Metro Jiangzicui",
        0x55 to "Metro Longshan Temple",
        0x56 to "Metro Ximen",
        0x58 to "Metro Shandao Temple",
        0x59 to "Metro Zhongxiao Xinsheng",
        0x5b to "Metro Zhongxiao Dunhua",
        0x5c to "Metro Sun Yat-Sen Memorial Hall",
        0x5d to "Metro Taipei City Hall",
        0x5f to "Metro Houshanpi",
        0x5e to "Metro Yongchun",
        0x60 to "Metro Kunyang",
        0x61 to "Metro Nangang",
        0x13 to "Metro Taipei Zoo",
        0x12 to "Metro Muzha",
        0x11 to "Metro Wanfang Community",
        0x10 to "Metro Wanfang Hospital",
        0x0f to "Metro Xinhai",
        0x0e to "Metro Linguang",
        0x0d to "Metro Liuzhangli",
        0x0c to "Metro Technology Building",
        0x0b to "Metro Daan",
        0x09 to "Metro Nanjing E. Rd.",
        0x08 to "Metro Zhongshan Junior High School",
        0x0a to "Metro Zhongxiao Fuxing",
        0x21 to "Metro Xindian",
        0x30 to "Metro Nanshijiao",
        0x2f to "Metro Jingan",
        0x2e to "Metro Yongan Market",
        0x2d to "Metro Dingxi",
        0x22 to "Metro Xindian District Office",
        0x23 to "Metro Qizhang",
        0x24 to "Metro Dapinglin",
        0x25 to "Metro Jingmei",
        0x26 to "Metro Wanlong",
        0x27 to "Metro Gongguan",
        0x28 to "Metro Taipower Building",
        0x29 to "Metro Guting",
        0x2a to "Metro Chiang Kai-Shek Memorial Hall",
        0x2b to "Metro Xiaonanmen",
        0x32 to "Metro NTU Hospital",
        0x34 to "Metro Taipei Main Station",
        0x42 to "Metro Fuxinggang",
        0x4f to "Metro Haishan",
        0x4e to "Metro Tucheng",
        0x4d to "Metro Yongning",
        0x52 to "Metro Banqiao",
        0x51 to "Metro Fuzhong",
        0x50 to "Metro Far Eastern Hospital",
        0x20 to "Metro Xiaobitan",
        0x07 to "Metro Songshan Airport",
        0x15 to "Metro Dazhi",
        0x16 to "Metro Jiannan Rd.",
        0x17 to "Metro Xihu",
        0x18 to "Metro Gangqian",
        0x19 to "Metro Wende",
        0x1a to "Metro Neihu",
        0x1b to "Metro Dahu Park",
        0x1c to "Metro Huzhou",
        0x1d to "Metro Donghu",
        0x1e to "Metro Nangang Software Park",
        0x1f to "Metro Taipei Nangang Exhibition Center",
        0xae to "Metro Luzhou",
        0xaf to "Metro Sanmin Senior High School",
        0xb0 to "Metro St. Ignatius High School",
        0xb1 to "Metro Sanhe Junior High School",
        0xb2 to "Metro Sanchong Elementary School",
        0x80 to "Metro Daqiaotou",
        0x82 to "Metro Zhongshan Elementary School",
        0x83 to "Metro Xingtian Temple",
        0x84 to "Metro Songjiang Nanjing",
        0x86 to "Metro Dongmen",
        0x7f to "Metro Taipei Bridge",
        0x7e to "Metro Cailiao",
        0x7d to "Metro Sanchong",
        0x7c to "Metro Xianse Temple",
        0x7b to "Metro Touqianzhuang",
        0x7a to "Metro Xinzhuang",
        0x79 to "Metro Fu Jen University",
        0xb3 to "Metro Huilong",
        0xb4 to "Metro Danfeng",
        0x05 to "Bus Fare",
        0x01 to "Store purchase")
