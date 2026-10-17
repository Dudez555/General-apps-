package com.example.data.crypto

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object E2EEEngine {
  private const val ALGORITHM = "AES/GCM/NoPadding"
  private const val TAG_LENGTH_BITS = 128
  private const val IV_LENGTH_BYTES = 12

  fun deriveKey(conversationId: String): SecretKey {
    val md = MessageDigest.getInstance("SHA-256")
    val keyBytes = md.digest("WiFiSMS-E2EE-Salt:$conversationId".toByteArray(Charsets.UTF_8))
    return SecretKeySpec(keyBytes, "AES")
  }

  fun encrypt(plainText: String, secretKey: SecretKey): String {
    val cipher = Cipher.getInstance(ALGORITHM)
    val iv = ByteArray(IV_LENGTH_BYTES).apply { SecureRandom().nextBytes(this) }
    val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
    val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    val combined = ByteArray(iv.size + cipherText.size)
    System.arraycopy(iv, 0, combined, 0, iv.size)
    System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
    return Base64.encodeToString(combined, Base64.NO_WRAP)
  }

  fun decrypt(base64Payload: String, secretKey: SecretKey): String {
    return try {
      val combined = Base64.decode(base64Payload, Base64.NO_WRAP)
      if (combined.size < IV_LENGTH_BYTES) return "[Encrypted payload corrupted]"
      val iv = ByteArray(IV_LENGTH_BYTES)
      System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES)
      val cipherText = ByteArray(combined.size - IV_LENGTH_BYTES)
      System.arraycopy(combined, IV_LENGTH_BYTES, cipherText, 0, cipherText.size)
      val cipher = Cipher.getInstance(ALGORITHM)
      val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
      cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
      val plainTextBytes = cipher.doFinal(cipherText)
      String(plainTextBytes, Charsets.UTF_8)
    } catch (e: Exception) {
      "[E2EE Decryption Error: ${e.message ?: "Invalid key or tamper detected"}]"
    }
  }

  fun getFingerprint(conversationId: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(conversationId.toByteArray(Charsets.UTF_8))
    return digest.take(8).joinToString("-") { String.format("%02X", it) }
  }
}
