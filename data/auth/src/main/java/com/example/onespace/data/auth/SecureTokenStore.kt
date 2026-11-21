package com.example.onespace.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.onespace.core.network.AccessTokenProvider
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts and decrypts token blobs. */
internal interface TokenCipher {
    fun encrypt(plain: String): String
    fun decrypt(blob: String): String?
}

/** Where encrypted blobs are kept; only ciphertext ever touches disk. */
internal interface BlobStore {
    fun read(key: String): String?
    fun write(key: String, value: String?)
}

/**
 * SEC-02: access and refresh tokens are encrypted with an AES-256/GCM key that lives in the
 * Android Keystore (never exported); only ciphertext is written to app storage.
 */
internal class KeystoreTokenCipher : TokenCipher {
    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    override fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val sealed = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(sealed, Base64.NO_WRAP)
    }

    override fun decrypt(blob: String): String? = runCatching {
        val (iv, sealed) = blob.split(":", limit = 2)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        }
        String(cipher.doFinal(Base64.decode(sealed, Base64.NO_WRAP)), Charsets.UTF_8)
    }.getOrNull()

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "onespace_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

internal class SharedPreferencesBlobStore(context: Context) : BlobStore {
    private val prefs = context.getSharedPreferences("onespace_secure_session", Context.MODE_PRIVATE)
    override fun read(key: String): String? = prefs.getString(key, null)
    override fun write(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}

/** Holds the session tokens in memory and persists them encrypted; read by OkHttp for `/me/...`. */
class SecureTokenStore internal constructor(
    private val blobs: BlobStore,
    private val cipher: TokenCipher
) : AccessTokenProvider {
    @Volatile private var access: String? = blobs.read(ACCESS)?.let(cipher::decrypt)
    @Volatile private var refresh: String? = blobs.read(REFRESH)?.let(cipher::decrypt)

    override fun accessToken(): String? = access
    fun refreshToken(): String? = refresh

    internal fun save(accessToken: String, refreshToken: String) {
        access = accessToken
        refresh = refreshToken
        blobs.write(ACCESS, cipher.encrypt(accessToken))
        blobs.write(REFRESH, cipher.encrypt(refreshToken))
    }

    internal fun clear() {
        access = null
        refresh = null
        blobs.write(ACCESS, null)
        blobs.write(REFRESH, null)
    }

    private companion object {
        const val ACCESS = "access_token"
        const val REFRESH = "refresh_token"
    }
}
