package pmeig.spring.libraries.security.core.crypto

import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class CryptoServiceTest {

  private val cryptoProperties: CryptoProperties = CryptoProperties()

  private val cryptoService: CryptoService = CryptoService(cryptoProperties)

  // -------------------------------------------------------------------------
  // encrypt()
  // -------------------------------------------------------------------------

  @Nested
  inner class Encrypt {

    @Test
    fun `should return empty string when value is null`() {
      val result = cryptoService.encrypt(null)
      assertEquals("", result)
    }

    @Test
    fun `should return empty string when value is empty`() {
      val result = cryptoService.encrypt("")
      assertEquals("", result)
    }

    @Test
    fun `should return a non-empty Base64 string for a valid input`() {
      val result = cryptoService.encrypt("hello")
      assertTrue(result.isNotEmpty())
    }

    @Test
    fun `should produce different ciphertext for different inputs`() {
      val result1 = cryptoService.encrypt("hello")
      val result2 = cryptoService.encrypt("world")
      assertNotEquals(result1, result2)
    }

    @Test
    fun `should produce the same ciphertext for the same input (deterministic)`() {
      val result1 = cryptoService.encrypt("hello")
      val result2 = cryptoService.encrypt("hello")
      assertEquals(result1, result2)
    }

    @Test
    fun `should handle input longer than the secret key (key cycling)`() {
      // "mysecretkey" is 11 chars — input of 20 chars forces key cycling
      val result = cryptoService.encrypt("a".repeat(20))
      assertNotNull(result)
      assertTrue(result.isNotEmpty())
    }

    @Test
    fun `should handle single character input`() {
      val result = cryptoService.encrypt("a")
      assertTrue(result.isNotEmpty())
    }

    @Test
    fun `should handle special characters`() {
      val result = cryptoService.encrypt("héllo wörld!")
      assertTrue(result.isNotEmpty())
    }
  }

  // -------------------------------------------------------------------------
  // decrypt()
  // -------------------------------------------------------------------------

  @Nested
  inner class Decrypt {

    @Test
    fun `should return empty string when value is null`() {
      val result = cryptoService.decrypt(null)
      assertEquals("", result)
    }

    @Test
    fun `should return empty string when value is empty`() {
      val result = cryptoService.decrypt("")
      assertEquals("", result)
    }

    @Test
    fun `should decrypt an encrypted value back to the original`() {
      val original = "hello"
      val encrypted = cryptoService.encrypt(original)
      val decrypted = cryptoService.decrypt(encrypted)
      assertEquals(original, decrypted)
    }

    @Test
    fun `should correctly round-trip a long string (key cycling)`() {
      val original = "This is a longer string that exceeds the key length"
      val encrypted = cryptoService.encrypt(original)
      val decrypted = cryptoService.decrypt(encrypted)
      assertEquals(original, decrypted)
    }

    @Test
    fun `should correctly round-trip special characters`() {
      val original = "héllo wörld!"
      val encrypted = cryptoService.encrypt(original)
      val decrypted = cryptoService.decrypt(encrypted)
      assertEquals(original, decrypted)
    }

    @Test
    fun `should correctly round-trip a single character`() {
      val original = "z"
      val encrypted = cryptoService.encrypt(original)
      val decrypted = cryptoService.decrypt(encrypted)
      assertEquals(original, decrypted)
    }

    @Test
    fun `should correctly round-trip numeric string`() {
      val original = "1234567890"
      val encrypted = cryptoService.encrypt(original)
      val decrypted = cryptoService.decrypt(encrypted)
      assertEquals(original, decrypted)
    }
  }
}