package com.jhight.stash.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

private const val DEFAULT_KEY_ALIAS = "com.jhight.stash.key"
private const val DEFAULT_KEY_SIZE = 256

/**
 * An AES-256 [CryptoProvider] implementation, built around Android's Keystore mechanism. Keys are
 * stored and retrieved by a key alias, or generated if not found.
 */
class AndroidKeystoreCryptoProvider(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val keySize: Int = DEFAULT_KEY_SIZE
) : CryptoProvider {
    private var _key: SecretKey? = null

    override val key: SecretKey
        get() = _key ?: loadKey().also {
            _key = it
        }

    private fun loadKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (keyStore.containsAlias(keyAlias)) {
            return keyStore.getKey(keyAlias, null) as SecretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setKeySize(keySize)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()

        return keyStore.getKey(keyAlias, null) as SecretKey
    }

    private val cipher = Cipher.getInstance(
        String.format(
            "%s/%s/%s",
            KeyProperties.KEY_ALGORITHM_AES,
            KeyProperties.BLOCK_MODE_CBC,
            KeyProperties.ENCRYPTION_PADDING_PKCS7,
        )
    )

    override fun encrypt(input: ByteArray): ByteArray {
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(input)
        return cipher.iv + encrypted
    }

    override fun decrypt(input: ByteArray): ByteArray {
        val iv = input.sliceArray(0..15)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        val ciphertext = input.sliceArray(16 until input.size)
        return cipher.doFinal(ciphertext)
    }
}