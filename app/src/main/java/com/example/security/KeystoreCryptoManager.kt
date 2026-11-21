package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface CryptoProvider {
  fun encrypt(plaintext: String): String

  fun decrypt(encodedPayload: String): String?

  fun isHardwareOrKeystoreBacked(): Boolean
}

/**
 * Pure AES-256-GCM engine that accepts an injected [SecretKey] so cryptographic round-trip and
 * tamper-detection tests can run deterministically on JVM as well as on AndroidKeyStore.
 */
class AesGcmCryptoEngine(
  private val keyProvider: () -> SecretKey,
  private val keystoreBacked: Boolean = false,
) : CryptoProvider {

  override fun encrypt(plaintext: String): String {
    if (plaintext.isEmpty()) return ""
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
    val iv = cipher.iv
    val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
    val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
    val ctB64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    return "$VERSION_PREFIX$ivB64:$ctB64"
  }

  override fun decrypt(encodedPayload: String): String? {
    if (encodedPayload.isEmpty()) return ""
    return try {
      if (!encodedPayload.startsWith(VERSION_PREFIX)) return null
      val body = encodedPayload.removePrefix(VERSION_PREFIX)
      val parts = body.split(":", limit = 2)
      if (parts.size != 2) return null
      val iv = Base64.decode(parts[0], Base64.NO_WRAP)
      val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
      if (iv.size != GCM_IV_LENGTH_BYTES) return null

      val cipher = Cipher.getInstance(TRANSFORMATION)
      val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
      cipher.init(Cipher.DECRYPT_MODE, keyProvider(), spec)
      val plainBytes = cipher.doFinal(ciphertext)
      String(plainBytes, StandardCharsets.UTF_8)
    } catch (_: Exception) {
      null
    }
  }

  override fun isHardwareOrKeystoreBacked(): Boolean = keystoreBacked

  companion object {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val VERSION_PREFIX = "v1:"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
  }
}

/**
 * Android Keystore AES-256-GCM key manager. Replaces deprecated EncryptedSharedPreferences with
 * direct AndroidKeyStore SecretKey + AES/GCM/NoPadding encryption compatible with API 24–36+.
 */
class AndroidKeystoreCryptoManager(
  private val alias: String = DEFAULT_KEY_ALIAS
) : CryptoProvider {

  private val engine = AesGcmCryptoEngine(
    keyProvider = { getOrCreateSecretKey() },
    keystoreBacked = true,
  )

  override fun encrypt(plaintext: String): String = engine.encrypt(plaintext)

  override fun decrypt(encodedPayload: String): String? = engine.decrypt(encodedPayload)

  override fun isHardwareOrKeystoreBacked(): Boolean {
    return try {
      val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
      ks.containsAlias(alias) || run {
        getOrCreateSecretKey()
        ks.containsAlias(alias)
      }
    } catch (_: Exception) {
      false
    }
  }

  @Synchronized
  private fun getOrCreateSecretKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    val existing = keyStore.getKey(alias, null) as? SecretKey
    if (existing != null) return existing

    val keyGenerator = KeyGenerator.getInstance(
      KeyProperties.KEY_ALGORITHM_AES,
      ANDROID_KEYSTORE,
    )
    val spec = KeyGenParameterSpec.Builder(
      alias,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
    )
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256)
      .setRandomizedEncryptionRequired(true)
      .build()

    keyGenerator.init(spec)
    return keyGenerator.generateKey()
  }

  companion object {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val DEFAULT_KEY_ALIAS = "smartkey_clip_vault_aes_v1"
  }
}
