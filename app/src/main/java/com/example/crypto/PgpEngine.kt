package com.example.crypto

import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.CompressionAlgorithmTags
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
import org.bouncycastle.bcpg.sig.Features
import org.bouncycastle.bcpg.sig.KeyFlags
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPCompressedData
import org.bouncycastle.openpgp.PGPCompressedDataGenerator
import org.bouncycastle.openpgp.PGPEncryptedData
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator
import org.bouncycastle.openpgp.PGPEncryptedDataList
import org.bouncycastle.openpgp.PGPKeyPair
import org.bouncycastle.openpgp.PGPKeyRingGenerator
import org.bouncycastle.openpgp.PGPLiteralData
import org.bouncycastle.openpgp.PGPLiteralDataGenerator
import org.bouncycastle.openpgp.PGPOnePassSignature
import org.bouncycastle.openpgp.PGPOnePassSignatureList
import org.bouncycastle.openpgp.PGPPBEEncryptedData
import org.bouncycastle.openpgp.PGPPrivateKey
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureGenerator
import org.bouncycastle.openpgp.PGPSignatureList
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyConverter
import org.bouncycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePBEKeyEncryptionMethodGenerator
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator
import org.bouncycastle.util.encoders.Hex
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.spec.ECGenParameterSpec
import java.util.Date

object PgpEngine {

    init {
        // Register or replace Bouncy Castle provider safely on Android
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) != null) {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        }
        Security.addProvider(BouncyCastleProvider())
    }

    /**
     * Formats a raw 40-char hex fingerprint into standard 4-char spaced blocks.
     * e.g. "ABCD 1234 EF56 ..."
     */
    fun formatFingerprint(fingerprint: String): String {
        val clean = fingerprint.replace(" ", "").uppercase()
        return clean.chunked(4).joinToString(" ")
    }

    /**
     * Generates a new OpenPGP RSA Key Pair (convenience delegate).
     */
    fun generateRsaKeyPair(
        name: String,
        email: String,
        comment: String = "",
        keyBits: Int = 3072,
        passphrase: String = "",
        expiryDays: Int? = null
    ): Pair<String, String> {
        return generateKeyPair(
            algorithmType = "RSA",
            name = name,
            email = email,
            comment = comment,
            keyBits = keyBits,
            passphrase = passphrase,
            expiryDays = expiryDays
        )
    }

    /**
     * Generates an OpenPGP Key Pair with support for:
     * - Ed25519 (Signing/Certify) + Curve25519 / Cv25519 / X25519 (ECDH Encryption)
     * - RSA (2048, 3072, 4096-bit)
     * - NIST P-256, P-384, P-521 (ECDSA + ECDH)
     * - Brainpool P-256
     */
    fun generateKeyPair(
        algorithmType: String = "RSA",
        name: String,
        email: String,
        comment: String = "",
        keyBits: Int = 3072,
        passphrase: String = "",
        expiryDays: Int? = null
    ): Pair<String, String> {
        val (masterPgpKp, encSubkeyPgpKp) = when (algorithmType.uppercase()) {
            "ED25519", "CV25519", "ED25519_CV25519" -> {
                val masterKpg = KeyPairGenerator.getInstance("Ed25519", BouncyCastleProvider.PROVIDER_NAME)
                val masterKp = masterKpg.generateKeyPair()
                val encKpg = KeyPairGenerator.getInstance("X25519", BouncyCastleProvider.PROVIDER_NAME)
                val encSubkeyKp = encKpg.generateKeyPair()
                Pair(
                    JcaPGPKeyPair(PublicKeyAlgorithmTags.EDDSA_LEGACY, masterKp, Date()),
                    JcaPGPKeyPair(PGPPublicKey.ECDH, encSubkeyKp, Date())
                )
            }
            "NIST_P256", "ECDSA_P256" -> {
                val masterKpg = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME)
                masterKpg.initialize(ECGenParameterSpec("secp256r1"))
                val masterKp = masterKpg.generateKeyPair()
                val encKpg = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
                encKpg.initialize(ECGenParameterSpec("secp256r1"))
                val encSubkeyKp = encKpg.generateKeyPair()
                Pair(
                    JcaPGPKeyPair(PGPPublicKey.ECDSA, masterKp, Date()),
                    JcaPGPKeyPair(PGPPublicKey.ECDH, encSubkeyKp, Date())
                )
            }
            "NIST_P384", "ECDSA_P384" -> {
                val masterKpg = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME)
                masterKpg.initialize(ECGenParameterSpec("secp384r1"))
                val masterKp = masterKpg.generateKeyPair()
                val encKpg = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
                encKpg.initialize(ECGenParameterSpec("secp384r1"))
                val encSubkeyKp = encKpg.generateKeyPair()
                Pair(
                    JcaPGPKeyPair(PGPPublicKey.ECDSA, masterKp, Date()),
                    JcaPGPKeyPair(PGPPublicKey.ECDH, encSubkeyKp, Date())
                )
            }
            "NIST_P521", "ECDSA_P521" -> {
                val masterKpg = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME)
                masterKpg.initialize(ECGenParameterSpec("secp521r1"))
                val masterKp = masterKpg.generateKeyPair()
                val encKpg = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
                encKpg.initialize(ECGenParameterSpec("secp521r1"))
                val encSubkeyKp = encKpg.generateKeyPair()
                Pair(
                    JcaPGPKeyPair(PGPPublicKey.ECDSA, masterKp, Date()),
                    JcaPGPKeyPair(PGPPublicKey.ECDH, encSubkeyKp, Date())
                )
            }
            "BRAINPOOL_P256" -> {
                val masterKpg = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME)
                masterKpg.initialize(ECGenParameterSpec("brainpoolP256r1"))
                val masterKp = masterKpg.generateKeyPair()
                val encKpg = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
                encKpg.initialize(ECGenParameterSpec("brainpoolP256r1"))
                val encSubkeyKp = encKpg.generateKeyPair()
                Pair(
                    JcaPGPKeyPair(PGPPublicKey.ECDSA, masterKp, Date()),
                    JcaPGPKeyPair(PGPPublicKey.ECDH, encSubkeyKp, Date())
                )
            }
            else -> { // RSA
                val kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
                kpg.initialize(if (keyBits in listOf(2048, 3072, 4096)) keyBits else 3072)
                val masterKp = kpg.generateKeyPair()
                val encSubkeyKp = kpg.generateKeyPair()
                Pair(
                    JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, masterKp, Date()),
                    JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, encSubkeyKp, Date())
                )
            }
        }

        val userId = buildString {
            append(name.trim())
            if (comment.isNotBlank()) append(" (${comment.trim()})")
            if (email.isNotBlank()) append(" <${email.trim()}>")
        }

        // Master key signature subpackets
        val masterSubpacketGen = PGPSignatureSubpacketGenerator().apply {
            setKeyFlags(false, KeyFlags.SIGN_DATA or KeyFlags.CERTIFY_OTHER)
            setPreferredSymmetricAlgorithms(
                false,
                intArrayOf(
                    SymmetricKeyAlgorithmTags.AES_256,
                    SymmetricKeyAlgorithmTags.AES_192,
                    SymmetricKeyAlgorithmTags.AES_128
                )
            )
            setPreferredHashAlgorithms(
                false,
                intArrayOf(
                    HashAlgorithmTags.SHA512,
                    HashAlgorithmTags.SHA384,
                    HashAlgorithmTags.SHA256
                )
            )
            setPreferredCompressionAlgorithms(
                false,
                intArrayOf(
                    CompressionAlgorithmTags.ZLIB,
                    CompressionAlgorithmTags.BZIP2,
                    CompressionAlgorithmTags.ZIP
                )
            )
            setFeature(false, Features.FEATURE_MODIFICATION_DETECTION)
            if (expiryDays != null && expiryDays > 0) {
                setKeyExpirationTime(false, expiryDays.toLong() * 24L * 3600L)
            }
        }

        // Subkey signature subpackets
        val subkeySubpacketGen = PGPSignatureSubpacketGenerator().apply {
            setKeyFlags(false, KeyFlags.ENCRYPT_COMMS or KeyFlags.ENCRYPT_STORAGE)
            if (expiryDays != null && expiryDays > 0) {
                setKeyExpirationTime(false, expiryDays.toLong() * 24L * 3600L)
            }
        }

        val sha1Calc = JcaPGPDigestCalculatorProviderBuilder()
            .build()
            .get(HashAlgorithmTags.SHA1)

        val hashAlgo = when (masterPgpKp.publicKey.algorithm) {
            PublicKeyAlgorithmTags.EDDSA_LEGACY, PublicKeyAlgorithmTags.Ed25519 -> HashAlgorithmTags.SHA512
            PGPPublicKey.ECDSA -> {
                when {
                    masterPgpKp.publicKey.bitStrength > 384 -> HashAlgorithmTags.SHA512
                    masterPgpKp.publicKey.bitStrength > 256 -> HashAlgorithmTags.SHA384
                    else -> HashAlgorithmTags.SHA256
                }
            }
            else -> HashAlgorithmTags.SHA512
        }

        val keyRingGen = PGPKeyRingGenerator(
            PGPSignature.POSITIVE_CERTIFICATION,
            masterPgpKp,
            userId,
            sha1Calc,
            masterSubpacketGen.generate(),
            null,
            JcaPGPContentSignerBuilder(
                masterPgpKp.publicKey.algorithm,
                hashAlgo
            ).setProvider(BouncyCastleProvider.PROVIDER_NAME),
            if (passphrase.isNotEmpty()) {
                JcePBESecretKeyEncryptorBuilder(
                    PGPEncryptedData.AES_256,
                    sha1Calc
                ).setProvider(BouncyCastleProvider.PROVIDER_NAME).build(passphrase.toCharArray())
            } else {
                null
            }
        )

        keyRingGen.addSubKey(
            encSubkeyPgpKp,
            subkeySubpacketGen.generate(),
            null
        )

        val secretKeyRing = keyRingGen.generateSecretKeyRing()
        val publicKeyRing = keyRingGen.generatePublicKeyRing()

        val armoredPub = keyRingToArmoredString { out -> publicKeyRing.encode(out) }
        val armoredPriv = keyRingToArmoredString { out -> secretKeyRing.encode(out) }

        return Pair(armoredPub, armoredPriv)
    }

    /**
     * Sanitizes ASCII-armored output by removing any "Version: ..." headers
     * (such as "Version: BCPG v@RELEASE_NAME@") per user specifications.
     */
    fun sanitizeArmoredOutput(armoredText: String): String {
        return armoredText.replace(Regex("(?m)^Version:.*\\r?\\n"), "")
    }

    private fun keyRingToArmoredString(writer: (OutputStream) -> Unit): String {
        val baos = ByteArrayOutputStream()
        ArmoredOutputStream.builder().clearHeaders().build(baos).use { armoredOut ->
            writer(armoredOut)
            armoredOut.flush()
        }
        return sanitizeArmoredOutput(baos.toString(StandardCharsets.UTF_8.name()))
    }

    /**
     * Parses an OpenPGP public or private key block from text or file bytes.
     */
    fun parseKeyBlock(keyBlock: String): List<ParsedKeyInfo> {
        val input = PGPUtil.getDecoderStream(ByteArrayInputStream(keyBlock.toByteArray(StandardCharsets.UTF_8)))
        val results = mutableListOf<ParsedKeyInfo>()

        val pgpFact = JcaPGPObjectFactory(input)
        var obj = pgpFact.nextObject()

        while (obj != null) {
            when (obj) {
                is PGPPublicKeyRing -> {
                    val masterKey = obj.publicKey
                    val info = extractParsedKeyInfo(
                        publicKey = masterKey,
                        isSecret = false,
                        hasPassphrase = false,
                        armoredPublic = exportPublicKeyToArmored(obj),
                        armoredPrivate = null
                    )
                    results.add(info)
                }
                is PGPSecretKeyRing -> {
                    val masterSecret = obj.secretKey
                    val masterPub = masterSecret.publicKey
                    val hasPass = masterSecret.keyEncryptionAlgorithm != SymmetricKeyAlgorithmTags.NULL
                    val info = extractParsedKeyInfo(
                        publicKey = masterPub,
                        isSecret = true,
                        hasPassphrase = hasPass,
                        armoredPublic = exportPublicKeyToArmored(obj.publicKey),
                        armoredPrivate = exportSecretKeyToArmored(obj)
                    )
                    results.add(info)
                }
                is PGPPublicKeyRingCollection -> {
                    for (ring in obj) {
                        val masterKey = ring.publicKey
                        results.add(
                            extractParsedKeyInfo(
                                publicKey = masterKey,
                                isSecret = false,
                                hasPassphrase = false,
                                armoredPublic = exportPublicKeyToArmored(ring),
                                armoredPrivate = null
                            )
                        )
                    }
                }
                is PGPSecretKeyRingCollection -> {
                    for (ring in obj) {
                        val masterSecret = ring.secretKey
                        val masterPub = masterSecret.publicKey
                        val hasPass = masterSecret.keyEncryptionAlgorithm != SymmetricKeyAlgorithmTags.NULL
                        results.add(
                            extractParsedKeyInfo(
                                publicKey = masterPub,
                                isSecret = true,
                                hasPassphrase = hasPass,
                                armoredPublic = exportPublicKeyToArmored(ring.publicKey),
                                armoredPrivate = exportSecretKeyToArmored(ring)
                            )
                        )
                    }
                }
            }
            obj = pgpFact.nextObject()
        }

        return results
    }

    private fun extractParsedKeyInfo(
        publicKey: PGPPublicKey,
        isSecret: Boolean,
        hasPassphrase: Boolean,
        armoredPublic: String,
        armoredPrivate: String?
    ): ParsedKeyInfo {
        val fpBytes = publicKey.fingerprint
        val fp = Hex.toHexString(fpBytes).uppercase()
        val keyIdHex = String.format("0x%016X", publicKey.keyID)

        val uids = publicKey.userIDs.asSequence().map { it.toString() }.toList()
        val primaryUid = uids.firstOrNull() ?: "Unknown UID"

        // Parse Name, Comment, Email from "Name (Comment) <email>"
        var name = primaryUid
        var comment = ""
        var email = ""

        val emailMatch = Regex("<([^>]+)>").find(primaryUid)
        if (emailMatch != null) {
            email = emailMatch.groupValues[1]
            name = primaryUid.substring(0, emailMatch.range.first).trim()
        }
        val commentMatch = Regex("\\(([^)]+)\\)").find(name)
        if (commentMatch != null) {
            comment = commentMatch.groupValues[1]
            name = name.removeRange(commentMatch.range).trim()
        }

        val algoName = when (publicKey.algorithm) {
            PGPPublicKey.RSA_GENERAL, PGPPublicKey.RSA_ENCRYPT, PGPPublicKey.RSA_SIGN -> "RSA"
            PGPPublicKey.DSA -> "DSA"
            PGPPublicKey.ECDSA -> "ECDSA (P-${publicKey.bitStrength})"
            PublicKeyAlgorithmTags.EDDSA_LEGACY, PublicKeyAlgorithmTags.Ed25519 -> "Ed25519 (EdDSA)"
            PGPPublicKey.ECDH -> "Cv25519 (ECDH)"
            PGPPublicKey.ELGAMAL_ENCRYPT, PGPPublicKey.ELGAMAL_GENERAL -> "ElGamal"
            else -> "Algo-${publicKey.algorithm}"
        }

        val validSecs = publicKey.validSeconds
        val expiryDate = if (validSecs > 0) {
            publicKey.creationTime.time + (validSecs * 1000L)
        } else {
            null
        }

        return ParsedKeyInfo(
            fingerprint = fp,
            keyIdHex = keyIdHex,
            userId = primaryUid,
            name = name.ifEmpty { "GPG Key" },
            email = email,
            comment = comment,
            isSecretKey = isSecret,
            hasPassphrase = hasPassphrase,
            algorithm = algoName,
            bitStrength = publicKey.bitStrength,
            creationDate = publicKey.creationTime.time,
            expiryDate = expiryDate,
            armoredPublicKey = armoredPublic,
            armoredPrivateKey = armoredPrivate
        )
    }

    private fun exportPublicKeyToArmored(ring: PGPPublicKeyRing): String {
        return keyRingToArmoredString { out -> ring.encode(out) }
    }

    private fun exportPublicKeyToArmored(key: PGPPublicKey): String {
        return keyRingToArmoredString { out -> key.encode(out) }
    }

    private fun exportSecretKeyToArmored(ring: PGPSecretKeyRing): String {
        return keyRingToArmoredString { out -> ring.encode(out) }
    }

    /**
     * Reads a PGPPublicKey from armored text.
     */
    fun readPublicKey(armoredText: String): PGPPublicKey? {
        val stream = PGPUtil.getDecoderStream(ByteArrayInputStream(armoredText.toByteArray(StandardCharsets.UTF_8)))
        val pgpFact = JcaPGPObjectFactory(stream)
        var obj = pgpFact.nextObject()
        while (obj != null) {
            when (obj) {
                is PGPPublicKeyRing -> {
                    // Try to find encryption subkey first if exists, otherwise master
                    var encKey: PGPPublicKey? = null
                    for (k in obj.publicKeys) {
                        if (k.isEncryptionKey) {
                            encKey = k
                            break
                        }
                    }
                    return encKey ?: obj.publicKey
                }
                is PGPPublicKeyRingCollection -> {
                    val ring = obj.keyRings.asSequence().firstOrNull()
                    if (ring != null) {
                        var encKey: PGPPublicKey? = null
                        for (k in ring.publicKeys) {
                            if (k.isEncryptionKey) {
                                encKey = k
                                break
                            }
                        }
                        return encKey ?: ring.publicKey
                    }
                }
                is PGPPublicKey -> return obj
            }
            obj = pgpFact.nextObject()
        }
        return null
    }

    /**
     * Reads all public keys in a keyring (for signature verification).
     */
    fun readAllPublicKeys(armoredText: String): List<PGPPublicKey> {
        val list = mutableListOf<PGPPublicKey>()
        try {
            val stream = PGPUtil.getDecoderStream(ByteArrayInputStream(armoredText.toByteArray(StandardCharsets.UTF_8)))
            val pgpFact = JcaPGPObjectFactory(stream)
            var obj = pgpFact.nextObject()
            while (obj != null) {
                when (obj) {
                    is PGPPublicKeyRing -> {
                        for (k in obj.publicKeys) list.add(k)
                    }
                    is PGPPublicKeyRingCollection -> {
                        for (ring in obj) {
                            for (k in ring.publicKeys) list.add(k)
                        }
                    }
                    is PGPPublicKey -> list.add(obj)
                }
                obj = pgpFact.nextObject()
            }
        } catch (_: Exception) { }
        return list
    }

    /**
     * Reads a PGPSecretKeyRing from armored text.
     */
    fun readSecretKeyRing(armoredText: String): PGPSecretKeyRing? {
        val stream = PGPUtil.getDecoderStream(ByteArrayInputStream(armoredText.toByteArray(StandardCharsets.UTF_8)))
        val pgpFact = JcaPGPObjectFactory(stream)
        var obj = pgpFact.nextObject()
        while (obj != null) {
            when (obj) {
                is PGPSecretKeyRing -> return obj
                is PGPSecretKeyRingCollection -> return obj.keyRings.asSequence().firstOrNull()
            }
            obj = pgpFact.nextObject()
        }
        return null
    }

    /**
     * Encrypts plaintext message or raw data using recipient public keys
     * and/or symmetric passphrase.
     */
    fun encryptData(
        plainBytes: ByteArray,
        filename: String = "message.txt",
        recipientPublicKeys: List<PGPPublicKey> = emptyList(),
        symmetricPassphrase: String = "",
        armorOutput: Boolean = true
    ): ByteArray {
        val outStream = ByteArrayOutputStream()
        val finalStream: OutputStream = if (armorOutput) {
            ArmoredOutputStream.builder().clearHeaders().build(outStream)
        } else {
            outStream
        }

        val encGen = PGPEncryptedDataGenerator(
            JcePGPDataEncryptorBuilder(PGPEncryptedData.AES_256)
                .setWithIntegrityPacket(true)
                .setSecureRandom(SecureRandom())
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
        )

        // Add symmetric passphrase method if provided
        if (symmetricPassphrase.isNotEmpty()) {
            encGen.addMethod(
                JcePBEKeyEncryptionMethodGenerator(symmetricPassphrase.toCharArray())
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            )
        }

        // Add public key methods
        for (pk in recipientPublicKeys) {
            encGen.addMethod(
                JcePublicKeyKeyEncryptionMethodGenerator(pk)
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            )
        }

        val encryptedOut = encGen.open(finalStream, ByteArray(1 shl 16))

        // Compress
        val compGen = PGPCompressedDataGenerator(PGPCompressedData.ZIP)
        val compOut = compGen.open(encryptedOut)

        // Literal data
        val litGen = PGPLiteralDataGenerator()
        val litOut = litGen.open(
            compOut,
            PGPLiteralData.BINARY,
            filename,
            plainBytes.size.toLong(),
            Date()
        )

        litOut.write(plainBytes)
        litOut.close()
        compOut.close()
        encryptedOut.close()

        if (armorOutput) {
            finalStream.close()
            val text = outStream.toString(StandardCharsets.UTF_8.name())
            return sanitizeArmoredOutput(text).toByteArray(StandardCharsets.UTF_8)
        }

        return outStream.toByteArray()
    }

    /**
     * Decrypts OpenPGP encrypted data using available secret key rings + passphrase
     * or symmetric passphrase.
     */
    fun decryptData(
        encryptedBytes: ByteArray,
        secretKeyRings: List<PGPSecretKeyRing> = emptyList(),
        passphrase: String = "",
        keyringPublicKeys: List<PGPPublicKey> = emptyList()
    ): DecryptionResult {
        try {
            val decoderStream = PGPUtil.getDecoderStream(ByteArrayInputStream(encryptedBytes))
            val pgpFact = JcaPGPObjectFactory(decoderStream)
            var obj = pgpFact.nextObject()

            // Find PGPEncryptedDataList
            var encList: PGPEncryptedDataList? = null
            while (obj != null) {
                if (obj is PGPEncryptedDataList) {
                    encList = obj
                    break
                }
                obj = pgpFact.nextObject()
            }

            if (encList == null) {
                return DecryptionResult(
                    isSuccess = false,
                    errorMessage = "Not a valid OpenPGP encrypted message or corrupted payload."
                )
            }

            var decryptedStream: InputStream? = null
            var usedKeyId = ""
            var isSymmetric = false

            // Try to find matching private key or symmetric PBE method
            for (encData in encList) {
                if (encData is PGPPublicKeyEncryptedData) {
                    val keyId = encData.keyID
                    val keyIdHex = String.format("0x%016X", keyId)
                    // Search in secret key rings
                    var secretKey: PGPSecretKey? = null
                    for (ring in secretKeyRings) {
                        val sk = ring.getSecretKey(keyId)
                        if (sk != null) {
                            secretKey = sk
                            break
                        }
                    }

                    if (secretKey != null) {
                        try {
                            val privKey = secretKey.extractPrivateKey(
                                JcePBESecretKeyDecryptorBuilder()
                                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                    .build(passphrase.toCharArray())
                            )
                            decryptedStream = encData.getDataStream(
                                JcePublicKeyDataDecryptorFactoryBuilder()
                                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                    .build(privKey)
                            )
                            usedKeyId = keyIdHex
                            break
                        } catch (e: Exception) {
                            return DecryptionResult(
                                isSuccess = false,
                                decryptedByKeyId = keyIdHex,
                                errorMessage = "Incorrect passphrase for private key $keyIdHex: ${e.message}"
                            )
                        }
                    } else {
                        usedKeyId = keyIdHex
                    }
                } else if (encData is PGPPBEEncryptedData) {
                    isSymmetric = true
                    if (passphrase.isNotEmpty()) {
                        try {
                            decryptedStream = encData.getDataStream(
                                JcePBEDataDecryptorFactoryBuilder()
                                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                    .build(passphrase.toCharArray())
                            )
                            usedKeyId = "Symmetric Password"
                            break
                        } catch (e: Exception) {
                            return DecryptionResult(
                                isSuccess = false,
                                isSymmetricallyEncrypted = true,
                                errorMessage = "Incorrect symmetric password: ${e.message}"
                            )
                        }
                    }
                }
            }

            if (decryptedStream == null) {
                return if (isSymmetric) {
                    DecryptionResult(
                        isSuccess = false,
                        isSymmetricallyEncrypted = true,
                        errorMessage = "Symmetric passphrase required to decrypt this message."
                    )
                } else {
                    DecryptionResult(
                        isSuccess = false,
                        decryptedByKeyId = usedKeyId,
                        errorMessage = "No matching private key found in your keyring for Key ID $usedKeyId."
                    )
                }
            }

            // Read decrypted stream (handle compression & literal data)
            val plainFact = JcaPGPObjectFactory(decryptedStream)
            var messageObj = plainFact.nextObject()

            if (messageObj is PGPCompressedData) {
                val compFact = JcaPGPObjectFactory(messageObj.dataStream)
                messageObj = compFact.nextObject()
            }

            var originalFileName = ""
            val outputBytes = ByteArrayOutputStream()

            if (messageObj is PGPLiteralData) {
                originalFileName = messageObj.fileName
                val litStream = messageObj.inputStream
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (litStream.read(buffer).also { bytesRead = it } != -1) {
                    outputBytes.write(buffer, 0, bytesRead)
                }
            } else if (messageObj is PGPOnePassSignatureList) {
                // One pass signature followed by literal data
                val nextObj = plainFact.nextObject()
                if (nextObj is PGPLiteralData) {
                    originalFileName = nextObj.fileName
                    val litStream = nextObj.inputStream
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (litStream.read(buffer).also { bytesRead = it } != -1) {
                        outputBytes.write(buffer, 0, bytesRead)
                    }
                }
            } else {
                // Fallback direct read
                return DecryptionResult(
                    isSuccess = false,
                    errorMessage = "Unexpected decrypted payload structure (${messageObj?.javaClass?.simpleName})"
                )
            }

            val resultBytes = outputBytes.toByteArray()
            val resultText = String(resultBytes, StandardCharsets.UTF_8)

            return DecryptionResult(
                isSuccess = true,
                decryptedText = resultText,
                decryptedBytes = resultBytes,
                originalFileName = originalFileName.ifEmpty { "decrypted_file" },
                isSymmetricallyEncrypted = isSymmetric,
                decryptedByKeyId = usedKeyId
            )

        } catch (e: Exception) {
            return DecryptionResult(
                isSuccess = false,
                errorMessage = "Decryption error: ${e.message}"
            )
        }
    }

    /**
     * Creates a standard PGP Cleartext Signed Message:
     * -----BEGIN PGP SIGNED MESSAGE-----
     * Hash: SHA512
     * ...
     * -----BEGIN PGP SIGNATURE-----
     */
    fun createCleartextSignature(
        text: String,
        secretKeyRing: PGPSecretKeyRing,
        passphrase: String
    ): String {
        val masterSecret = secretKeyRing.secretKey
        val privateKey = masterSecret.extractPrivateKey(
            JcePBESecretKeyDecryptorBuilder()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(passphrase.toCharArray())
        )

        val out = ByteArrayOutputStream()
        val armoredOut = ArmoredOutputStream.builder().clearHeaders().build(out)
        armoredOut.beginClearText(HashAlgorithmTags.SHA512)

        val normalizedText = text.replace("\r\n", "\n").replace("\r", "\n")
        val lines = normalizedText.split("\n").map { it.trimEnd() }

        val sigGen = PGPSignatureGenerator(
            JcaPGPContentSignerBuilder(
                masterSecret.publicKey.algorithm,
                HashAlgorithmTags.SHA512
            ).setProvider(BouncyCastleProvider.PROVIDER_NAME)
        )
        sigGen.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, privateKey)

        val subpacketGen = PGPSignatureSubpacketGenerator()
        val uids = masterSecret.publicKey.userIDs
        if (uids.hasNext()) {
            subpacketGen.addSignerUserID(false, uids.next() as String)
        }
        sigGen.setHashedSubpackets(subpacketGen.generate())

        for (i in lines.indices) {
            val line = lines[i]
            val lineBytes = line.toByteArray(StandardCharsets.UTF_8)
            if (line.startsWith("-")) {
                armoredOut.write("- ".toByteArray(StandardCharsets.UTF_8))
            }
            armoredOut.write(lineBytes)
            sigGen.update(lineBytes)

            if (i < lines.size - 1) {
                armoredOut.write("\r\n".toByteArray(StandardCharsets.UTF_8))
                sigGen.update("\r\n".toByteArray(StandardCharsets.UTF_8))
            }
        }

        armoredOut.endClearText()
        sigGen.generate().encode(armoredOut)
        armoredOut.close()

        return sanitizeArmoredOutput(out.toString(StandardCharsets.UTF_8.name()))
    }

    /**
     * Verifies a cleartext signed message against public keys in the keyring.
     */
    fun verifyCleartextSignature(
        cleartextSignedMessage: String,
        allPublicKeys: List<PGPPublicKey>
    ): SignatureVerificationResult {
        try {
            val sigMarker = "-----BEGIN PGP SIGNATURE-----"
            val sigStart = cleartextSignedMessage.indexOf(sigMarker)
            if (sigStart == -1) {
                return SignatureVerificationResult(
                    isValid = false,
                    keyIdHex = "",
                    signerUserId = null,
                    signerFingerprint = null,
                    signatureDate = null,
                    isKeyFoundInKeyring = false,
                    message = "No OpenPGP signature found in message."
                )
            }

            val sigBlock = cleartextSignedMessage.substring(sigStart)
            val messageMarker = "-----BEGIN PGP SIGNED MESSAGE-----"
            val messageStart = cleartextSignedMessage.indexOf(messageMarker)
            val textStartIndex = if (messageStart != -1) {
                val searchFrom = messageStart + messageMarker.length
                val doubleNewline = cleartextSignedMessage.indexOf("\n\n", searchFrom)
                val crlfCrlf = cleartextSignedMessage.indexOf("\r\n\r\n", searchFrom)
                when {
                    crlfCrlf != -1 && (doubleNewline == -1 || crlfCrlf < doubleNewline) -> crlfCrlf + 4
                    doubleNewline != -1 -> doubleNewline + 2
                    else -> searchFrom
                }
            } else {
                0
            }

            val textPart = cleartextSignedMessage.substring(textStartIndex, sigStart).trimEnd('\r', '\n')
            val lines = textPart.replace("\r\n", "\n").replace("\r", "\n").split("\n").map {
                if (it.startsWith("- ")) it.substring(2).trimEnd() else it.trimEnd()
            }

            val sigStream = PGPUtil.getDecoderStream(ByteArrayInputStream(sigBlock.toByteArray(StandardCharsets.UTF_8)))
            val fact = JcaPGPObjectFactory(sigStream)
            var obj = fact.nextObject()
            var sigList: PGPSignatureList? = null

            while (obj != null) {
                if (obj is PGPSignatureList) {
                    sigList = obj
                    break
                }
                obj = fact.nextObject()
            }

            if (sigList == null || sigList.isEmpty) {
                return SignatureVerificationResult(
                    isValid = false,
                    keyIdHex = "",
                    signerUserId = null,
                    signerFingerprint = null,
                    signatureDate = null,
                    isKeyFoundInKeyring = false,
                    message = "No valid OpenPGP signature found in signature block."
                )
            }

            val sig = sigList[0]
            val sigKeyId = sig.keyID
            val sigKeyIdHex = String.format("0x%016X", sigKeyId)

            val matchingPub = allPublicKeys.firstOrNull { it.keyID == sigKeyId }
            if (matchingPub == null) {
                return SignatureVerificationResult(
                    isValid = false,
                    keyIdHex = sigKeyIdHex,
                    signerUserId = null,
                    signerFingerprint = null,
                    signatureDate = sig.creationTime?.time,
                    isKeyFoundInKeyring = false,
                    message = "Signature is present for Key ID $sigKeyIdHex, but this public key is not in your keyring."
                )
            }

            sig.init(
                JcaPGPContentVerifierBuilderProvider()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME),
                matchingPub
            )

            for (i in lines.indices) {
                val line = lines[i]
                sig.update(line.toByteArray(StandardCharsets.UTF_8))
                if (i < lines.size - 1) {
                    sig.update("\r\n".toByteArray(StandardCharsets.UTF_8))
                }
            }

            val verified = sig.verify()
            val uids = matchingPub.userIDs.asSequence().map { it.toString() }.toList()
            val fp = Hex.toHexString(matchingPub.fingerprint).uppercase()

            return SignatureVerificationResult(
                isValid = verified,
                keyIdHex = sigKeyIdHex,
                signerUserId = uids.firstOrNull() ?: "Unknown UID",
                signerFingerprint = fp,
                signatureDate = sig.creationTime?.time,
                isKeyFoundInKeyring = true,
                message = if (verified) {
                    "Signature is VALID and authentic."
                } else {
                    "BAD SIGNATURE: Content has been tampered with or corrupted!"
                }
            )

        } catch (e: Exception) {
            return SignatureVerificationResult(
                isValid = false,
                keyIdHex = "",
                signerUserId = null,
                signerFingerprint = null,
                signatureDate = null,
                isKeyFoundInKeyring = false,
                message = "Verification failed: ${e.message}"
            )
        }
    }
}
