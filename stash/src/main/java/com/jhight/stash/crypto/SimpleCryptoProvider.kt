package com.jhight.stash.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * A customizable [CryptoProvider] implementation that allows users to specify any cipher and key.
 * Defaults to an AES-128 cipher with a randomly generated key and IV. This implementation does not
 * use the Android Keystore.
 *
 * @param key The key to use for encryption and decryption. Defaults to a randomly generated key.
 * @param cipher The [Cipher] to use for encryption and decryption. Defaults to `"AES/CBC/PKCS5Padding"`.
 * @param ivSpec The [IvParameterSpec] to use for encryption and decryption. Defaults to a randomly generated IV.
 */
class SimpleCryptoProvider(
    override val key: SecretKey = SecretKeySpec(SecureRandom().generateSeed(16), "AES"),
    private val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"),
    private val ivSpec: IvParameterSpec = IvParameterSpec(SecureRandom().generateSeed(16)),
) : CryptoProvider {
    override fun encrypt(input: ByteArray): ByteArray {
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        return cipher.doFinal(input)
    }

    override fun decrypt(input: ByteArray): ByteArray {
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        return cipher.doFinal(input)
    }
}