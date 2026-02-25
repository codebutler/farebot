# MIFARE Classic Key Recovery — Card Compatibility

## Attack Types

| Attack          | Target                        | Speed        | Requires              |
|-----------------|-------------------------------|--------------|-----------------------|
| Default keys    | Any MIFARE Classic            | Instant      | Nothing               |
| Nested          | Weak PRNG (pre-2008 Classic)  | ~5 seconds   | 1 known sector key    |
| Hardnested      | True RNG (Classic EV1+, 2012) | 5-30 minutes | 1 known sector key    |

## MIFARE Classic Transit Systems

### Requires Key Recovery (custom keys, at least some sectors locked)

| Transit System          | Region            | Card Type   | PRNG Type  | Attack Needed  |
|-------------------------|-------------------|-------------|------------|----------------|
| OV-chipkaart            | Netherlands       | Classic 4K  | True RNG   | Hardnested     |
| SeqGo                   | Brisbane, AU      | Classic     | Varies     | Nested/Hard    |
| EasyCard                | Taiwan            | Classic     | Varies     | Nested/Hard    |
| Kazan                   | Russia            | Classic     | Varies     | Nested/Hard    |
| Warsaw                  | Poland            | Classic     | Varies     | Nested/Hard    |
| ChC Metrocard           | Wellington, NZ    | Classic     | Varies     | Nested/Hard    |
| Manly Fast Ferry        | Sydney, AU        | Classic     | Varies     | Nested/Hard    |
| RKF                     | Russia            | Classic     | Varies     | Nested/Hard    |
| Umarsh                  | Russia            | Classic     | Varies     | Nested/Hard    |
| Zolotaya Korona         | Russia            | Classic     | Varies     | Nested/Hard    |

### Uses Default Keys (no attack needed)

| Transit System   | Region              | Transit System   | Region              |
|------------------|---------------------|------------------|---------------------|
| Bilhete Unico SP | Sao Paulo, BR       | Oyster           | London, UK          |
| BIP              | Chile               | Podorozhnik      | St. Petersburg, RU  |
| Bonobus          | France              | Ricarica Mi      | Italy               |
| Charlie Card     | Boston, US          | Selecta          | France              |
| Cifial           | France              | Smart Rider      | Perth, AU           |
| ERG              | Various             | TouchnGo         | Malaysia            |
| Gautrain         | South Africa        | Troika           | Moscow, RU          |
| Kiev             | Ukraine             | Waikato          | New Zealand         |
| Komuterlink      | Malaysia            | YarGor           | Yaroslavl, RU       |
| LAX TAP          | Los Angeles, US     | MetroMoney       | Russia              |
| MSP Goto         | Malaysia            | MetroQ           | Russia              |
| Nextfare         | Various             | Otago            | Dunedin, NZ         |

## MIFARE Classic Chip Variants

| Chip                      | Year  | PRNG     | Crypto      | Attack               |
|---------------------------|-------|----------|-------------|----------------------|
| Classic (NXP MF1S50/70)   | 2001  | Weak     | Crypto1     | Nested               |
| Classic EV1 (MF1S7001)    | 2012  | True RNG | Crypto1     | Hardnested           |
| Classic EV2               | 2019  | True RNG | AES option  | Hardnested (Crypto1) |
| Plus SL1 (emulation mode) | 2009  | Varies   | Crypto1     | Nested/Hardnested    |

## Other Card Types (no key recovery needed)

| Card Type       | Encryption     | Systems (examples)                         |
|-----------------|----------------|--------------------------------------------|
| DESFire EV1/2/3 | AES / 3DES     | ORCA, Clipper, HSL, Opal, Myki, Leap       |
| Ultralight      | None / pwd     | Troika UL, Amiibo, Ventra UL               |
| FeliCa          | DES (internal) | Suica, Edy, Octopus                        |
| ISO 7816        | Varies         | Calypso (Opus, Mobib), TMoney, Snapper      |
| CEPAS           | Internal       | EZLink                                     |
