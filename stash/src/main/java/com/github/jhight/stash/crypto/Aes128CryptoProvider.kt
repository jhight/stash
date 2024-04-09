package com.github.jhight.stash.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * A simple AES-128 [CryptoProvider] implementation that uses a randomly generated key and IV.
 */
class Aes128CryptoProvider : CryptoProvider {
    override val key = SecretKeySpec(SecureRandom().generateSeed(16), "AES")
    private val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    private val ivSpec = IvParameterSpec(SecureRandom().generateSeed(16))

    override fun encrypt(input: ByteArray): ByteArray {
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        return cipher.doFinal(input)
    }

    override fun decrypt(input: ByteArray): ByteArray {
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        return cipher.doFinal(input)
    }
}