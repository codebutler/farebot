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
* [Michael Farrell](https://github.com/micolous/) (Opal, Manly Fast Ferry, Go card, Myki)
* [Rob O'Regan](http://www.robx1.net/nswtkt/private/manlyff/manlyff.htm) (Manly Fast Ferry card image)
* [The Noun Project][15] (Various icons)

## Supported Protocols

* [CEPAS][2] (Not compatible with all devices)
* [FeliCa][8]
* [MIFARE Classic][23] (Not compatible with all devices)
* [MIFARE DESFire][6]
* [MIFARE Ultralight][24] (Not compatible with all devices)

## Supported Cards

* [Bilhete Único](http://www.sptrans.com.br/bilhete_unico/) - São Paulo, Brazil (Requires encryption keys, not compatible with all devices)
* [Clipper][1] - San Francisco, CA, USA
* [EZ-Link][7] - Singapore (Not compatible with all devices)
* [Go card][20] (Translink) - Brisbane and South East Queensland, Australia (requires encryption keys, not compatible with all devices, not all stations known)
* [Manly Fast Ferry][19] - Sydney, Australia (requires encryption keys, not compatible with all devices)
* [Myki][21] - Melbourne (and surrounds), VIC, Australia (Only the card number can be read)
* [Matkakortti][16], [HSL][17] - Finland
* [NETS FlashPay](http://www.netsflashpay.com.sg/) - Singapore
* [Opal][18] - Sydney (and surrounds), NSW, Australia
* [ORCA][0] - Seattle, WA, USA
* [OV-chipkaart](http://www.ov-chipkaart.nl/) - Netherlands (Requires encryption keys, not compatible with all devices)
* [Suica][9], [ICOCA][10], [PASMO][11], [Edy][12] - Japan


## Supported Phones

FareBot requires an NFC Android phone running 4.0.1 or later.

## Building

```
git clone https://github.com/codebutler/farebot.git
cd farebot
git submodule init
git submodule update
./gradlew assembleDebug
```

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
[15]: http://www.thenounproject.com/
[16]: http://www.hsl.fi/EN/passengersguide/travelcard/Pages/default.aspx
[17]: http://www.hsl.fi/EN/
[18]: http://www.opal.com.au/
[19]: http://www.manlyfastferry.com.au/
[20]: http://translink.com.au/tickets-and-fares/go-card
[21]: http://ptv.vic.gov.au/
[23]: https://en.wikipedia.org/wiki/MIFARE#MIFARE_Classic
[24]: https://en.wikipedia.org/wiki/MIFARE#MIFARE_Ultralight_and_MIFARE_Ultralight_EV1
