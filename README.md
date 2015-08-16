# Farebot M

View your remaining balance, recent trips, and other information from contactless public transit cards using your NFC Android phone!

This fork is maintained by Michael, which contains some support for additional cards.

**Version**: 2.9.22

## Principally Written By

* [Eric Butler][5] <eric@codebutler.com>

## About this fork

See [the Project Status page on the wiki][19].

## Thanks To

* [Karl Koscher][3] (ORCA)
* [Sean Cross][4] (CEPES/EZ-Link)
* Anonymous Contributor (Clipper)
* [nfc-felica][13] and [IC SFCard Fan][14] projects (Suica)
* [Wilbert Duijvenvoorde](https://github.com/wandcode) (MIFARE Classic/OV-chipkaart)
* [tbonang](https://github.com/tbonang) (NETS FlashPay)
* [Marcelo Liberato](https://github.com/mliberato) (Bilhete Único)
* [Lauri Andler](https://github.com/landler/) (HSL)
* [Michael](https://github.com/micolous/) (Opal, Manly Fast Ferry)
* [Rob O'Regan](http://www.robx1.net/nswtkt/private/manlyff/manlyff.htm) (Manly Fast Ferry card image)
* [The Noun Project][15] (Various icons)

## Supported Protocols

* [MIFARE DESFire][6]
* [CEPAS][2] (Not compatible with all devices)
* [FeliCa][8]
* [MIFARE Classic](http://en.wikipedia.org/wiki/MIFARE#MIFARE_Classic) (Not compatible with all devices)

## Supported Cards

* [Bilhete Único](http://www.sptrans.com.br/bilhete_unico/) - São Paulo, Brazil (Requires encryption keys, not compatible with all devices)
* [Clipper][1] - San Francisco, CA, USA
* [EZ-Link][7] - Singapore (Not compatible with all devices)
* [Manly Fast Ferry][20] - Sydney, Australia (new in M, requires encryption keys, not compatible with all devices)
* [Matkakortti][16], [HSL][17] - Finland
* [NETS FlashPay](http://www.netsflashpay.com.sg/) - Singapore
* [Opal][18] - Sydney, Australia (new in M)
* [ORCA][0] - Seattle, WA, USA
* [OV-chipkaart](http://www.ov-chipkaart.nl/) - Netherlands (Requires encryption keys, not compatible with all devices)
* [Suica][9], [ICOCA][10], [PASMO][11], [Edy][12] - Japan

## Supported Phones

FareBot requires an NFC Android phone running 4.0.3 or later.

Some newer devices do not support MIFARE Classic.  MIFARE Classic is not an NFC-compliant card format, so can only be read with phones with NXP chipsets.

[0]: http://www.orcacard.com/
[1]: https://www.clippercard.com/
[2]: http://en.wikipedia.org/wiki/CEPAS
[3]: https://twitter.com/#!/supersat
[4]: https://twitter.com/#!/xobs
[5]: https://twitter.com/#!/codebutler
[6]: http://en.wikipedia.org/wiki/MIFARE#MIFARE_DESFire
[7]: http://www.ezlink.com.sg/index.php
[8]: http://en.wikipedia.org/wiki/FeliCa
[9]: http://en.wikipedia.org/wiki/Suica
[10]: http://en.wikipedia.org/wiki/ICOCA
[11]: http://en.wikipedia.org/wiki/PASMO
[12]: http://en.wikipedia.org/wiki/Edy
[13]: http://code.google.com/p/nfc-felica/
[14]: http://www014.upp.so-net.ne.jp/SFCardFan/
[15]: http://www.thenounproject.com/
[16]: http://www.hsl.fi/EN/passengersguide/travelcard/Pages/default.aspx
[17]: http://www.hsl.fi/EN/
[18]: http://www.opal.com.au/
[19]: https://github.com/micolous/farebot/wiki/Project-Status
[20]: http://www.manlyfastferry.com.au/

