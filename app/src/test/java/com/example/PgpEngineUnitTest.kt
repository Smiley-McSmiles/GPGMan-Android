package com.example

import com.example.crypto.PgpEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class PgpEngineUnitTest {

    @Test
    fun testFormatFingerprint() {
        val rawFp = "1234567890ABCDEF1234567890ABCDEF12345678"
        val formatted = PgpEngine.formatFingerprint(rawFp)
        assertEquals("1234 5678 90AB CDEF 1234 5678 90AB CDEF 1234 5678", formatted)
    }

    @Test
    fun testEd25519AndCv25519Generation() {
        val (armoredPub, armoredPriv) = PgpEngine.generateKeyPair(
            algorithmType = "Ed25519",
            name = "Ed25519 User",
            email = "ed@example.com",
            comment = "Modern ECC",
            keyBits = 256,
            passphrase = "edpassphrase",
            expiryDays = null
        )

        assertTrue(armoredPub.contains("-----BEGIN PGP PUBLIC KEY BLOCK-----"))
        assertTrue(armoredPriv.contains("-----BEGIN PGP PRIVATE KEY BLOCK-----"))
        org.junit.Assert.assertFalse(armoredPub.contains("Version:"))
        org.junit.Assert.assertFalse(armoredPriv.contains("Version:"))
        org.junit.Assert.assertFalse(armoredPub.contains("BCPG"))
        org.junit.Assert.assertFalse(armoredPriv.contains("BCPG"))

        val parsedPublic = PgpEngine.parseKeyBlock(armoredPub)
        assertEquals(1, parsedPublic.size)
        val pubInfo = parsedPublic[0]
        assertEquals("Ed25519 User", pubInfo.name)
        assertEquals("ed@example.com", pubInfo.email)
        assertTrue(pubInfo.algorithm.contains("Ed", ignoreCase = true))

        val parsedPrivate = PgpEngine.parseKeyBlock(armoredPriv)
        assertTrue(parsedPrivate.isNotEmpty())
        val privInfo = parsedPrivate.first { it.isSecretKey }
        assertEquals(true, privInfo.isSecretKey)
        assertEquals(true, privInfo.hasPassphrase)
    }

    @Test
    fun testEd25519SigningAndVerification() {
        val (armoredPub, armoredPriv) = PgpEngine.generateKeyPair(
            algorithmType = "Ed25519",
            name = "Ed Signer",
            email = "signer@ed25519.org",
            comment = "",
            keyBits = 256,
            passphrase = "edpass",
            expiryDays = null
        )

        val secretRing = PgpEngine.readSecretKeyRing(armoredPriv)
        assertNotNull(secretRing)

        val message = "This statement is cryptographically signed using Ed25519."
        val signedMessage = PgpEngine.createCleartextSignature(message, secretRing!!, "edpass")

        assertTrue(signedMessage.contains("-----BEGIN PGP SIGNED MESSAGE-----"))
        assertTrue(signedMessage.contains("-----BEGIN PGP SIGNATURE-----"))

        val allPubKeys = PgpEngine.readAllPublicKeys(armoredPub)
        val verifyResult = PgpEngine.verifyCleartextSignature(signedMessage, allPubKeys)

        assertTrue("Ed25519 signature must verify: ${verifyResult.message}", verifyResult.isValid)
        assertTrue(verifyResult.isKeyFoundInKeyring)
        assertTrue(verifyResult.signerUserId?.contains("Ed Signer") == true)
    }

    @Test
    fun testNistP256Generation() {
        val (armoredPub, armoredPriv) = PgpEngine.generateKeyPair(
            algorithmType = "NIST_P256",
            name = "NIST User",
            email = "nist@example.com",
            comment = "Suite B",
            keyBits = 256,
            passphrase = "",
            expiryDays = 365
        )

        assertTrue(armoredPub.contains("-----BEGIN PGP PUBLIC KEY BLOCK-----"))
        assertTrue(armoredPriv.contains("-----BEGIN PGP PRIVATE KEY BLOCK-----"))

        val parsed = PgpEngine.parseKeyBlock(armoredPub)
        assertEquals(1, parsed.size)
        assertTrue(parsed[0].algorithm.contains("ECDSA", ignoreCase = true))
    }

    @Test
    fun testGenerateAndParseRsaKey() {
        val (armoredPub, armoredPriv) = PgpEngine.generateRsaKeyPair(
            name = "Alice Test",
            email = "alice@example.com",
            comment = "Unit Test Key",
            keyBits = 2048,
            passphrase = "testpassphrase123",
            expiryDays = 30
        )

        assertTrue(armoredPub.contains("-----BEGIN PGP PUBLIC KEY BLOCK-----"))
        assertTrue(armoredPriv.contains("-----BEGIN PGP PRIVATE KEY BLOCK-----"))

        val parsedPublic = PgpEngine.parseKeyBlock(armoredPub)
        assertEquals(1, parsedPublic.size)
        val pubInfo = parsedPublic[0]
        assertEquals("Alice Test", pubInfo.name)
        assertEquals("alice@example.com", pubInfo.email)
        assertEquals("RSA", pubInfo.algorithm)
        assertEquals(false, pubInfo.isSecretKey)
        assertNotNull(pubInfo.keyIdHex)

        val parsedPrivate = PgpEngine.parseKeyBlock(armoredPriv)
        assertTrue(parsedPrivate.isNotEmpty())
        val privInfo = parsedPrivate.first { it.isSecretKey }
        assertEquals(true, privInfo.isSecretKey)
        assertEquals(true, privInfo.hasPassphrase)
    }

    @Test
    fun testSymmetricEncryptionAndDecryption() {
        val plainText = "Confidential GPG Message for Unit Testing"
        val password = "secret_vault_password_99"

        val encryptedBytes = PgpEngine.encryptData(
            plainBytes = plainText.toByteArray(StandardCharsets.UTF_8),
            filename = "note.txt",
            recipientPublicKeys = emptyList(),
            symmetricPassphrase = password,
            armorOutput = true
        )

        val armoredEncrypted = String(encryptedBytes, StandardCharsets.UTF_8)
        assertTrue(armoredEncrypted.contains("-----BEGIN PGP MESSAGE-----"))

        val decResult = PgpEngine.decryptData(
            encryptedBytes = encryptedBytes,
            secretKeyRings = emptyList(),
            passphrase = password
        )

        assertTrue(decResult.isSuccess)
        assertEquals(plainText, decResult.decryptedText)
        assertEquals(true, decResult.isSymmetricallyEncrypted)
    }

    @Test
    fun testAsymmetricEncryptionAndDecryption() {
        val (armoredPub, armoredPriv) = PgpEngine.generateRsaKeyPair(
            name = "Bob Recipient",
            email = "bob@example.com",
            comment = "",
            keyBits = 2048,
            passphrase = "bobpassphrase",
            expiryDays = null
        )

        val pubKey = PgpEngine.readPublicKey(armoredPub)
        assertNotNull(pubKey)

        val secretRing = PgpEngine.readSecretKeyRing(armoredPriv)
        assertNotNull(secretRing)

        val message = "Hello Bob, this is an asymmetric encrypted PGP message!"
        val encryptedBytes = PgpEngine.encryptData(
            plainBytes = message.toByteArray(StandardCharsets.UTF_8),
            filename = "secret.txt",
            recipientPublicKeys = listOf(pubKey!!),
            symmetricPassphrase = "",
            armorOutput = true
        )

        val decResult = PgpEngine.decryptData(
            encryptedBytes = encryptedBytes,
            secretKeyRings = listOf(secretRing!!),
            passphrase = "bobpassphrase"
        )

        assertTrue("Decryption should succeed: ${decResult.errorMessage}", decResult.isSuccess)
        assertEquals(message, decResult.decryptedText)
    }

    @Test
    fun testCleartextSignatureAndVerification() {
        val (armoredPub, armoredPriv) = PgpEngine.generateRsaKeyPair(
            name = "Signer Carol",
            email = "carol@example.com",
            comment = "",
            keyBits = 2048,
            passphrase = "carolpass",
            expiryDays = null
        )

        val secretRing = PgpEngine.readSecretKeyRing(armoredPriv)
        assertNotNull(secretRing)

        val message = "Official Announcement from Carol:\nRelease v1.0.0 is verified."
        val signedMessage = PgpEngine.createCleartextSignature(message, secretRing!!, "carolpass")

        assertTrue(signedMessage.contains("-----BEGIN PGP SIGNED MESSAGE-----"))
        assertTrue(signedMessage.contains("-----BEGIN PGP SIGNATURE-----"))

        val allPubKeys = PgpEngine.readAllPublicKeys(armoredPub)
        val verifyResult = PgpEngine.verifyCleartextSignature(signedMessage, allPubKeys)

        assertTrue("Signature must be valid: ${verifyResult.message}", verifyResult.isValid)
        assertTrue(verifyResult.isKeyFoundInKeyring)
        assertTrue(verifyResult.signerUserId?.contains("Carol") == true)
    }
}
