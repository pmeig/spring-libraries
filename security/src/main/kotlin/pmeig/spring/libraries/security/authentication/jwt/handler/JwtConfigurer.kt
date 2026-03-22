package pmeig.spring.libraries.security.authentication.jwt.handler

import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.JwtParserBuilder

interface JwtConfigurer {
  fun prepareCreateToken(builder: JwtBuilder): JwtBuilder
  fun prepareParseToken(token: String, parser: JwtParserBuilder): JwtParserBuilder
}