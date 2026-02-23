package pmeig.spring.libraries.security.authentication.jwt.model

import java.security.Key

interface KeyPair {
  val public: Key
  val private: Key
}

data class SecretKeyPair(override val private: Key) : KeyPair {
  override val public: Key = private
}

data class KeyPairImpl(override val private: Key, override val public: Key) : KeyPair