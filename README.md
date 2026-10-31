# SafeSpot: Spy Scanner & VPN

SafeSpot is a free Android app (package `com.safespot.scanner`, minSdk 24) that checks a phone for stalkerware and malware, and includes a free VPN.

## Features
- **Spy scanner**:
  - matches installed apps against the [Echap stalkerware-indicators](https://github.com/AssoEchap/stalkerware-indicators) list by package name and signing certificate;
  - flags apps with enabled accessibility services, device-admin rights or notification-listener access;
  - flags hidden apps, the install source (Play, another store or sideloaded) and risky permission combinations, with a risk score.
- **Antivirus**:
  - checks the SHA-256 of installed APKs, and of APK files in a folder you pick, against a hash DB you can update;
  - heuristics for trojan-like apps;
  - a daily check of newly installed or updated apps;
  - an optional MalwareBazaar cloud lookup that you have to opt into.
- **Device health**: security patch age, screen lock, developer options and ADB, root signs, storage, battery and network.
- **VPN**: OpenVPN (through [ics-openvpn](https://github.com/schwabe/ics-openvpn)) using free [VPN Gate](https://www.vpngate.net/) volunteer relays. You can turn on the optional Quad9 malware-blocking DNS.
- **Dashboard**: security score, diagnostics charts, scan history, streaks and achievements.

## Build
```
JAVA_HOME=/path/to/jdk17 ./gradlew :app:assembleSkeletonOvpn2Debug
```
Requirements:
- Android SDK and NDK, CMake and swig. These are needed by the `:openvpn` module, which builds OpenVPN and OpenSSL natively.
- `keystore/keystore.properties` (storeFile, storePassword, keyAlias, keyPassword) for signed release builds. Keep it out of git.
- An optional abuse.ch key for the cloud lookup: `-PmbAuthKey=...`.

## Remote data (GitHub Pages)
The app loads its data from `https://micky9889900.github.io/vpn/`. The `pages/vpn/` folder holds:
- `servers.json`: the VPN servers. `tools/refresh_servers.py` regenerates it from VPN Gate. To add your own or paid servers, add entries with the same schema (`type, host, port, proto, country, countryCode, config, provider`).
- `malware_db.json`: built by `tools/build_malware_db.py`, which needs `MB_AUTH_KEY`.
- `ioc.json`: built by `tools/convert_ioc.py` from Echap's ioc.yaml.

If `servers.json` can't be reached, the app falls back to the live VPN Gate API.

## Licences
- SafeSpot is under the **GNU GPL v3** (see `LICENSE`).
- `openvpn/` is ics-openvpn © Arne Schwabe, under GPL-2.0 (see `openvpn/LICENSE-ics-openvpn.txt`). It includes OpenVPN, OpenSSL, LZ4 and LZO under their own licences.
- The stalkerware indicators are © Echap, under CC BY 4.0.
- The VPN servers are provided by the VPN Gate Academic Experiment, University of Tsukuba.
