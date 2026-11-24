# CXP Exporter

CXP Exporter is an open-source Android app that exports passkeys and passwords from Google Password Manager to KeePassXC and KeePassDX. It uses the Android Credential Exchange API (FIDO CXP/CXF) to receive your credentials, lets you review and select them, and writes them to an encrypted KeePass database (KDBX) or to plain files for import.

> **Disclaimer:** CXP Exporter is not affiliated with or endorsed by Google, the FIDO Alliance, the KeePassXC project or the KeePassDX project. Google, Google Password Manager, Android, FIDO, KeePassXC, KeePassDX and passkey names are trademarks of their respective owners. Exported files contain private keys and passwords. Treat them as secrets, and use at your own risk.

## Features

- **Import** passkeys and passwords from Google Password Manager through the Android Credential Exchange (Credential Manager) API.
- **Re-open** a CXF JSON file exported earlier, to export it again in another format.
- **Review** records with filters (All, Passkeys, Passwords), search by title, username, relying party ID or URL, and select or deselect records.
- **Save** password records into a provider of your choice (for example KeePassDX) through the system dialog.
- **Export** to an encrypted KeePass 4 database (KDBX), to a CXF JSON file, or to a KeePassXC-compatible CSV file (passwords only).
- **Clear** all records and raw data from memory at any time.

## Requirements

- Android 14 or newer.
- Up-to-date Google Play services with credential transfer support.
- Google Password Manager as the exporter. The credentials are read from it.
- Availability depends on Google's rollout. If the import reports that the device does not support credential transfer, or that no app offers credentials to export, the feature is not available on your device yet.

## Download

Signed APKs are published on the [Releases](https://github.com/sameaslooks/android-cxp-exporter/releases) page. The APK is named `cxp-exporter-<version>.apk`.

Verify the APK before installing:

- Check the signing certificate with `apksigner verify --print-certs cxp-exporter-<version>.apk`.
- Confirm the APK was built by GitHub Actions from this repository: `gh attestation verify cxp-exporter-<version>.apk -R sameaslooks/android-cxp-exporter`.

## Getting started

1. Install the APK. Allow installing unknown apps for the app you open it from.
2. Open CXP Exporter and tap **Import from Credential Manager**. Follow the system prompts to select Google Password Manager and approve the transfer.
3. Review the imported list. Use the filters and search, and select the records you want to keep.
4. Tap **Export**, choose **KDBX**, and enter a master password twice.
5. Import the file into KeePassXC or KeePassDX:
   - **KeePassXC:** open the `.kdbx` file, or merge it into an open database with **Database > Merge from database**.
   - **KeePassDX:** open the `.kdbx` file, or merge it into an existing database from the database menu.
6. Delete the exported file once you have verified the import. Clear the records in CXP Exporter.

## Export formats

| Format | File | Encrypted | Content |
|---|---|---|---|
| KDBX 4 | `cxp-export-<yyyyMMdd-HHmm>.kdbx` | Yes, with the master password you enter | Root group "CXP Import" with subgroups "Passkeys" and "Passwords" |
| CXF JSON | `cxp-export-<yyyyMMdd-HHmm>.json` | No | Raw CXF JSON as received, including passkey private keys |
| KeePassXC CSV | `cxp-export-<yyyyMMdd-HHmm>.csv` | No | Passwords only, group `Root/CXP Import` |

Use KDBX for passkeys. CXF JSON and CSV are unencrypted and should be deleted after use.

## Passkey storage format

Passkeys are written to KDBX entries using the attributes that KeePassXC and KeePassDX read for passkeys:

| Attribute | Value |
|---|---|
| `KPEX_PASSKEY_CREDENTIAL_ID` | Credential ID, base64url without padding |
| `KPEX_PASSKEY_PRIVATE_KEY_PEM` | PKCS#8 private key in PEM format (protected) |
| `KPEX_PASSKEY_RELYING_PARTY` | Relying party ID (rpId) |
| `KPEX_PASSKEY_USERNAME` | Username |
| `KPEX_PASSKEY_USER_HANDLE` | User handle, base64url without padding |

Each passkey entry also has URL `https://<rpId>` and the tag `Passkey`.

## Why there is no Save button for passkeys

Passwords can be saved to a provider of your choice with the **Save** button. The system dialog lets you pick KeePassDX or another provider.

Passkeys have no Save button. The Credential Manager API always creates a new key when a passkey is registered, so it cannot store the existing private key. The website would also not accept the new key, because it does not match the one already registered there. Use the KDBX export for passkeys instead.

## Permissions

CXP Exporter requests no Android permissions. In particular, it does not request `INTERNET`.

## Security

- **No network.** The app has no `INTERNET` permission and sends no telemetry.
- **Memory only.** Records and the raw CXF JSON are kept in memory. Nothing is written to disk unless you start an export.
- **Screen protection.** `FLAG_SECURE` blocks screenshots and screen recording.
- **No backup.** `android:allowBackup="false"`, and the data extraction rules exclude all data.
- **Clear.** The Clear action removes records and raw JSON from memory and zero-fills private key bytes. The same happens when the app is closed.
- **Exported files are plaintext unless they are KDBX.** Delete CXF JSON and CSV files after use. Store KDBX files on an encrypted device.

Report vulnerabilities privately through [GitHub Security Advisories](https://github.com/sameaslooks/android-cxp-exporter/security/advisories). See [SECURITY.md](SECURITY.md).

## Building from source

Requirements: Windows 10/11, Linux or macOS; enough free disk space for the JDK, Android SDK and Gradle; internet access for the first build.

The build scripts download JDK 17, the Android SDK (platform android-36, build-tools 36.0.0) and Gradle into `.tools` inside the repository. Nothing is installed system-wide. The build runs the unit tests and then assembles the APK.

**Windows (PowerShell):**

```
git clone https://github.com/sameaslooks/android-cxp-exporter.git
cd android-cxp-exporter
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

Options: `-Debug`, `-Clean`.

**Linux / macOS:**

```
git clone https://github.com/sameaslooks/android-cxp-exporter.git
cd android-cxp-exporter
./build.sh
```

Options: `--debug`, `--clean`.

The APK is written to `dist/cxp-exporter-<version>.apk`.

**Signing:** the first release build creates a local key in `.signing`. Back it up. Updates install only over an APK signed with the same key. If the key is lost, you must uninstall and reinstall the app. Keep `.signing` out of version control.

### Release workflow

The **Release APK** GitHub Actions workflow (started manually) builds the version set in the project, runs the tests, and publishes a GitHub release `vX.Y.Z` with the signed APK. The **CI** workflow runs on every push and pull request and builds the debug APK and runs the unit tests.

Required repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | Base64 of `.signing/release.jks` |
| `SIGNING_STORE_PASSWORD` | `storePassword` from `.signing/keystore.properties` |
| `SIGNING_KEY_ALIAS` | `keyAlias` from `.signing/keystore.properties` |
| `SIGNING_KEY_PASSWORD` | `keyPassword` from `.signing/keystore.properties` |

Create `SIGNING_KEYSTORE_BASE64` from the local key:

- Windows PowerShell (copies the value to the clipboard): `[Convert]::ToBase64String([IO.File]::ReadAllBytes(".signing\release.jks")) | Set-Clipboard`
- Linux (prints the value): `base64 -w0 .signing/release.jks`
- macOS (prints the value): `base64 -i .signing/release.jks | tr -d '\n'`

Paste the value into the `SIGNING_KEYSTORE_BASE64` secret, then clear the clipboard or terminal.

CI must sign with the same key as local builds. Otherwise, updates over a locally built install will fail.

## Credits

- [FIDO Alliance Credential Exchange Format (CXF) v1.0 specification](https://fidoalliance.org/specs/cx/cxf-v1.0-ps-20250814.html)
- [Android Credential Manager](https://developer.android.com/identity/sign-in/credential-manager) and the Credential Manager provider events API
- [kotpass](https://github.com/keemobile/kotpass) for KDBX writing
- The passkey attribute format (`KPEX_PASSKEY_*`) used by [KeePassXC](https://github.com/keepassxreboot/keepassxc) and KeePassDX

Third-party components and their licenses are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Author

Sergei K. ([@sameaslooks](https://github.com/sameaslooks))

## License

MIT, see [LICENSE](LICENSE).
