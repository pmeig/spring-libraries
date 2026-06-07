package pmeig.spring.libraries.security.core

import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails


fun interface AuthenticationFactory<T: UserDetails> {
  fun from(user: T?): Authentication?
}