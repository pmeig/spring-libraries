package pmeig.spring.libraries.security.authentication.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.MacAlgorithm
import io.jsonwebtoken.security.SignatureAlgorithm
import pmeig.spring.libraries.security.authentication.jwt.model.KeyPair
import pmeig.spring.libraries.security.authentication.jwt.model.KeyPairImpl
import pmeig.spring.libraries.security.authentication.jwt.model.SecretKeyPair
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.SecretKey
import kotlin.io.encoding.Base64

@Suppress("unused")
enum class JwtAlgorithm(val create: (private: String?, public: String?) -> KeyPair) {
  HS256(secretKeyCreator(Jwts.SIG.HS256)),
  HS384(secretKeyCreator(Jwts.SIG.HS384)),
  HS512(secretKeyCreator(Jwts.SIG.HS512)),
  RS256(keyPairCreator(Jwts.SIG.RS256)),
  RS384(keyPairCreator(Jwts.SIG.RS384)),
  RS512(keyPairCreator(Jwts.SIG.RS512)),
  ES256(keyPairCreator(Jwts.SIG.ES256)),
  ES384(keyPairCreator(Jwts.SIG.ES384)),
  ES512(keyPairCreator(Jwts.SIG.ES512)),
  PS256(keyPairCreator(Jwts.SIG.PS256)),
  PS384(keyPairCreator(Jwts.SIG.PS384)),
  PS512(keyPairCreator(Jwts.SIG.PS512));
}

private fun keyPairCreator(algorithm: SignatureAlgorithm): (secret: String?, public: String?) -> KeyPair = { private, public ->
  if (!private.isNullOrEmpty() && !public.isNullOrEmpty()) {
    val keyFactory = KeyFactory.getInstance(algorithm.id)
    val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(private)))
    val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(Base64.decode(public)))
    KeyPairImpl(privateKey, publicKey)
  } else {
    val pair = algorithm.keyPair().build()
    println("the private key for Jwt generated is ${base64Encode(pair.private.encoded)}")
    println("the public key for Jwt generated is ${base64Encode(pair.public.encoded)}")
    KeyPairImpl(pair.private, pair.public)
  }
}

private fun secretKeyCreator(algorithm: MacAlgorithm): (private: String?, public: String?) -> KeyPair = { secret, _ ->
  val secret: SecretKey = if (null != secret) {
    Keys.hmacShaKeyFor(Base64.decode(secret))
  } else {
    val newSecret = algorithm.key().build()
    println("the secret for Jwt generated is ${base64Encode(newSecret.encoded)}")
    newSecret
  }
  SecretKeyPair(secret)
}

private fun base64Encode(encoded: ByteArray): String = Base64.encode(encoded)
