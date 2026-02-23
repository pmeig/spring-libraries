package pmeig.spring.libraries.security.authentication.jwt.handler

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtBuilder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import pmeig.spring.libraries.security.authentication.jwt.model.RequestUser

interface JwtClaimHandler<T: UserDetails> {
  fun parse(builder: JwtBuilder.BuilderClaims, user: T): JwtBuilder.BuilderClaims
  @Suppress("UNCHECKED_CAST")
  fun parse(builder: JwtBuilder.BuilderClaims, user: RequestUser)
    = parse(builder, User(user.pseudo, user.credential, listOf()) as T)
  fun unparse(claims: Claims): T
}