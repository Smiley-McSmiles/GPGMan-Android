# GPGMan 🛡️

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![OpenPGP](https://img.shields.io/badge/OpenPGP-Bouncy%20Castle%20v1.78.1-blue.svg)](https://www.bouncycastle.org)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20First-success.svg)](#privacy--security-architecture)

**GPGMan** is a modern, full-featured OpenPGP (GnuPG) Manager, Encrypted Cryptographic Vault, and Digital Signature Suite built natively for Android using Kotlin and Jetpack Compose (Material 3).

---

## 👥 Project Attributions

- **WOOSAH** — Lead Architect & Maintainer
- **Gemini 3.8 Flash** — Lead Engineer

---

## ✨ Features & Capabilities

### 🔑 1. Advanced Key Generation & Management
- **Multiple Modern & Classic Cryptographic Curves/Algorithms:**
  - **Ed25519 + Cv25519 (Recommended / Modern Standard):** 256-bit Edwards curve (`EdDSA`) for certification and digital signatures + 256-bit Montgomery curve (`X25519 / ECDH`) for secure encryption. Immune to side-channel timing attacks, fast, and compact.
  - **RSA:** 2048, 3072, and 4096-bit key sizes with positive self-certification.
  - **NIST Elliptic Curves:** NIST P-256 (`secp256r1`), P-384 (`secp384r1`), and P-521 (`secp521r1`) (ECDSA + ECDH).
  - **Brainpool Curves:** Brainpool P-256 (`brainpoolP256r1`).
- **Flexible Expiration Presets:** Choose from **30 Days**, **90 Days**, **1 Year**, **2 Years**, or **Never Expire** using responsive chip selectors.
- **Passphrase Protection:** Optional AES-256 passphrase protection for secret keys.
- **Formatted Fingerprints:** Standard 4-character spaced OpenPGP fingerprints (e.g. `XXXX XXXX XXXX ...`) for easy visual verification.
- **Web of Trust:** Configurable trust levels (Unknown, Marginal, Full, Ultimate).

### 🔒 2. Hardware-Backed Security
- **Android KeyStore AES-256-GCM:** All imported and generated private keys are envelope-encrypted using hardware-backed cryptographic keys inside the Android KeyStore (`AndroidKeyStore` provider). Private keys never touch storage in plaintext.
- **Master App Lock with 3 Authentic Lock Types:**
  - **Continuous Drag Pattern Lock:** Authentic Android 3x3 gesture unlock. Drag seamlessly across the 9 dots with real-time rubber-band tracing lines and auto-submission upon finger release.
  - **Numeric PIN Lock:** 4-8 digit numeric PIN with single-tap unlock directly from the keyboard checkmark (`ImeAction.Done`).
  - **Alphanumeric Password Lock:** Arbitrary length passphrases with toggleable password visibility.

### 🛡️ 3. Encryption & Decryption
- **Asymmetric PGP Encryption:** Encrypt text messages and binary files using one or multiple recipient public keys.
- **Symmetric Passphrase Encryption (PBE):** Standalone encryption using AES-256 symmetric cipher without needing public keys.
- **File Encryption:** Encrypt and decrypt arbitrary files (PDFs, images, documents, archives) with integrity check packets (`MDC`).
- **Clean Armor Output:** Produces standard, sanitized ASCII-armored blocks (`-----BEGIN PGP MESSAGE-----`) without vendor metadata or build version tags.

### ✍️ 4. Digital Signatures & Verification
- **Cleartext Signatures:** Create RFC 4880 compliant OpenPGP cleartext signed documents (`-----BEGIN PGP SIGNED MESSAGE-----`).
- **Cryptographic Verification:** Automatically validates signatures against all public keys in your local keyring, reporting key ID, signer user identity, and signature timestamp.

### 🗄️ 5. Secure Encrypted Vault
- Store confidential notes, recovery phrases, seeds, and credentials.
- Backed by Room Local Database and encrypted at rest with Android KeyStore master keys.
- Quick search, copy, and export capabilities.

### 🌐 6. Key Import, Export & Lookups
- **GitHub Public Keys:** Instant lookup and import of any GitHub developer's public GPG keys via `https://github.com/<username>.gpg`.
- **Keyserver Query:** Search and fetch verified keys directly from `keys.openpgp.org` by email or Key ID.
- **Public & Private Keys Backup:**
  - Export all public keys in an ASCII-armored bundle.
  - Export all private keys with an interactive security confirmation warning.

### 📋 7. Security Audit Logging
- Built-in audit trail recording cryptographic operations (key generation, encryption, decryption, signatures, deletions, backups) with status badges and timestamps.

---

## 🏗️ Architecture & Technology Stack

- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material Design 3 (M3)](https://m3.material.io)
- **Cryptography Engine:** [Bouncy Castle](https://www.bouncycastle.org) OpenPGP (`org.bouncycastle:bcpg-jdk18on:1.78.1`)
- **Key Store:** Android KeyStore Provider with AES-256-GCM encryption
- **Database:** [Android Room Database](https://developer.android.com/training/data-storage/room) with KSP
- **Architecture:** Clean Architecture / MVVM with Kotlin Coroutines and `StateFlow`
- **Testing:** Local JVM unit tests & Robolectric tests (`app/src/test/java/com/example/`)

---

## 🔒 Privacy & Security Policy

1. **100% Offline-First:** GPGMan stores everything locally on your device. There is no telemetry, analytics, tracking, or cloud sync.
2. **Zero Third-Party Servers:** Network calls are strictly made only when the user explicitly triggers an external lookup (e.g. GitHub key fetch or OpenPGP keyserver search).
3. **Hardware Storage:** Private keys are protected using Android KeyStore master keys.

---

## 🚀 Building & Running

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or newer
- **JDK:** OpenJDK 17 or 21
- **Android SDK:** Compile SDK 36, Minimum SDK 24 (Android 7.0+)

### Building from Command Line

```bash
# Clone the repository
git clone https://github.com/Smiley-McSmiles/GPGMan.git
cd GPGMan

# Build debug APK
gradle assembleDebug

# Run Unit and Robolectric Tests
gradle :app:testDebugUnitTest
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

```
Copyright (c) 2026 WOOSAH (Lead Architect & Maintainer), Gemini 3.8 Flash (Lead Engineer)
```
