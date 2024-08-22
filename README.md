# FareBot

View your remaining balance, recent trips, and other information from contactless public transit cards using your NFC Android phone!

[![Build Status](https://travis-ci.org/codebutler/farebot.svg?branch=master)](https://travis-ci.org/codebutler/farebot)

## Written By

* [Eric Butler][5] <eric@codebutler.com>

## Thanks To

* [Karl Koscher][3] (ORCA)
* [Sean Cross][4] (CEPAS/EZ-Link)
* Anonymous Contributor (Clipper)
* [nfc-felica][13] and [IC SFCard Fan][14] projects (Suica)
* [Wilbert Duijvenvoorde](https://github.com/wandcode) (MIFARE Classic/OV-chipkaart)
* [tbonang](https://github.com/tbonang) (NETS FlashPay)
* [Marcelo Liberato](https://github.com/mliberato) (Bilhete Único)
* [Lauri Andler](https://github.com/landler/) (HSL)
* [Michael Farrell](https://github.com/micolous/) (Opal, Manly Fast Ferry, Go card, Myki, Octopus)
* [Rob O'Regan](http://www.robx1.net/nswtkt/private/manlyff/manlyff.htm) (Manly Fast Ferry card image)
* [b33f](http://www.fuzzysecurity.com/tutorials/rfid/4.html) (EasyCard)
* [Bondan](https://github.com/sybond) [Sumbodo](http://sybond.web.id) (Kartu Multi Trip, COMMET)

## License

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

## Supported Protocols

* [CEPAS][2] (Not compatible with all devices)
* [FeliCa][8]
* [MIFARE Classic][23] (Not compatible with all devices)
* [MIFARE DESFire][6]
* [MIFARE Ultralight][24] (Not compatible with all devices)

## Supported Cards

* [Clipper][1] - San Francisco, CA, USA
* [EZ-Link][7] - Singapore (Not compatible with all devices)
* [Myki][21] - Melbourne (and surrounds), VIC, Australia (Only the card number can be read)
* [Matkakortti][16], [HSL][17] - Finland
* [NETS FlashPay](http://www.netsflashpay.com.sg/) - Singapore
* [Octopus][25] - Hong Kong
* [Opal][18] - Sydney (and surrounds), NSW, Australia
* [ORCA][0] - Seattle, WA, USA
* [Suica][9], [ICOCA][10], [PASMO][11], [Edy][12] - Japan
* [Kartu Multi Trip][26] - Jakarta, Indonesia (Only for new FeliCa cards)

## Supported Cards (Keys Required)

These cards require that you crack the encryption key (using a [proxmark3](https://github.com/Proxmark/proxmark3/wiki/Mifare-HowTo#how-can-i-break-a-card) 
or [mfcuk](https://github.com/nfc-tools/mfcuk)+[mfoc](https://github.com/nfc-tools/mfoc)) and are not compatible with all devices.

* [Bilhete Único](http://www.sptrans.com.br/bilhete_unico/) - São Paulo, Brazil
* [Go card][20] (Translink) - Brisbane and South East Queensland, Australia
* [Manly Fast Ferry][19] - Sydney, Australia 
* [OV-chipkaart](http://www.ov-chipkaart.nl/) - Netherlands
* [EasyCard](http://www.easycard.com.tw/english/index.asp) - Taipei (Older insecure cards only)

## Supported Phones

FareBot requires an NFC Android phone running 5.0 or later.

## Building

    $ git clone https://github.com/codebutler/farebot.git
    $ cd farebot
    $ git submodule update --init
    $ ./gradlew assembleDebug

## Open Source Libraries

FareBot uses the following open-source libraries:

* [AutoDispose](https://github.com/uber/AutoDispose)
* [AutoValue](https://github.com/google/auto/tree/master/value)
* [AutoValue Gson](https://github.com/rharter/auto-value-gson)
* [Dagger](https://google.github.io/dagger/)
* [Gson](https://github.com/google/gson)
* [Guava](https://github.com/google/guava)
* [Kotlin](https://kotlinlang.org/)
* [Magellan](https://github.com/wealthfront/magellan/)
* [RxJava](https://github.com/ReactiveX/RxJava)
* [RxRelay](https://github.com/JakeWharton/RxRelay)

[0]: http://www.orcacard.com/
[1]: https://www.clippercard.com/
[2]: https://en.wikipedia.org/wiki/CEPAS
[3]: https://twitter.com/#!/supersat
[4]: https://twitter.com/#!/xobs
[5]: https://twitter.com/#!/codebutler
[6]: https://en.wikipedia.org/wiki/MIFARE#MIFARE_DESFire
[7]: http://www.ezlink.com.sg/
[8]: https://en.wikipedia.org/wiki/FeliCa
[9]: https://en.wikipedia.org/wiki/Suica
[10]: https://en.wikipedia.org/wiki/ICOCA
[11]: https://en.wikipedia.org/wiki/PASMO
[12]: https://en.wikipedia.org/wiki/Edy
[13]: http://code.google.com/p/nfc-felica/
[14]: http://www014.upp.so-net.ne.jp/SFCardFan/
[16]: http://www.hsl.fi/EN/passengersguide/travelcard/Pages/default.aspx
[17]: http://www.hsl.fi/EN/
[18]: http://www.opal.com.au/
[19]: http://www.manlyfastferry.com.au/
[20]: http://translink.com.au/tickets-and-fares/go-card
[21]: http://ptv.vic.gov.au/
[23]: https://en.wikipedia.org/wiki/MIFARE#MIFARE_Classic
[24]: https://en.wikipedia.org/wiki/MIFARE#MIFARE_Ultralight_and_MIFARE_Ultralight_EV1
[25]: http://www.octopus.com.hk/home/en/index.html
[26]: https://en.wikipedia.org/wiki/Kereta_Commuter_Indonesia
