package com.jhight.stash.crypto

import javax.crypto.SecretKey

interface CryptoProvider {
    val key: SecretKey
    fun encrypt(input: ByteArray): ByteArray
    fun decrypt(input: ByteArray): ByteArray
}