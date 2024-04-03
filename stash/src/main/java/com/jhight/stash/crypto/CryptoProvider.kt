package com.jhight.stash.crypto

import javax.crypto.SecretKey

/**
 * Defines the [SecretKey] and corresponding cryptographic behavior.
 */
interface CryptoProvider {
    /**
     * The key used in encryption and decryption.
     */
    val key: SecretKey

    /**
     * Defines how encryption is performed.
     */
    fun encrypt(input: ByteArray): ByteArray

    /**
     * Defines how decryption is performed.
     */
    fun decrypt(input: ByteArray): ByteArray
}