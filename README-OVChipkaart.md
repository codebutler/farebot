# OV-chipkaart support for FareBot

This contains the implementation of support for the OV-chipkaart used in the Netherlands.

By [Wilbert Duijvenvoorde](https://github.com/wandcode)

## Database

The database is created with [ovc-tools][0], specifically my [fork][1] of it (nothing special just updated data and an option to discard machines data).

## Keys

To fully read an OV-chipkaart you will need the keys for all the sectors. These keys can be obtained with [mfocGUI][2] and can be found in the Keys folder after dumping. If you want to save some time, you could dump only the so called 'A' keys as those are the only ones that are used ;).

For use in FareBot, save the key dump with a `.farebotkeys` extension, email the file to yourself, and open it on your device.

## TODO / FIXME

* Normally every trip has an end time (ExitTimeStamp) and it would be nice if it would be displayed. 
* Most trips have a start and an end station, but some of the names are too long to display them (both) on the same line. Split them into two lines and maybe display the start or end time behind each one?
* Display the subscriptions somewhere (its own tab or Advanced Info for example).
* The whole keys part could use a serious rewrite...
* Maybe move the database outside of the app to save space for those who don't need it?
* See all the TODOs and FIXMEs throughout the code for everything that needs fixing.

* (Not OV-chipkaart related and for the future): display public transit lanes on the Google map (if available)?

## Thanks To

* [PC Active][3] for hosting the [wiki][4].
* [OV-Chipkaart Forum][5] for all the research and information.
* [Huuf][2] for [mfocGUI][2].
* [Nexus-s-ovc][6] which got me started.
* [Eric Butler][7] for [FareBot][8] of course ;)

[0]: https://github.com/wvengen/ovc-tools
[1]: https://github.com/wandcode/ovc-tools
[2]: http://www.huuf.info/OV/
[3]: http://www.pc-active.nl/
[4]: http://ov-chipkaart.pc-active.nl/Main_Page
[5]: http://www.ov-chipkaart.me/forum/
[6]: https://code.google.com/p/nexus-s-ovc/
[7]: http://codebutler.com/
[8]: https://github.com/codebutler/farebot
