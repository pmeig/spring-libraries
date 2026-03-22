@file:Suppress("unused")

package pmeig.spring.libraries.security.core.annotation.models

import pmeig.spring.libraries.security.core.annotation.PmeigSecurity
import pmeig.spring.libraries.security.core.models.SecurityFeatures

class GlobalSecurity {
  private val features = mutableMapOf<GlobalSecurityKey, List<String>>()
  companion object {
    private val ACCEPTED_OR = GlobalSecurityKey(true, PmeigSecurity.Type.OR)
    private val ACCEPTED_AND = GlobalSecurityKey(true, PmeigSecurity.Type.AND)
    private val DENIED_OR = GlobalSecurityKey(false, PmeigSecurity.Type.OR)
    private val DENIED_AND = GlobalSecurityKey(false, PmeigSecurity.Type.AND)
  }


  fun addFeature(accepted: Boolean, vararg features: String, type: PmeigSecurity.Type = PmeigSecurity.Type.OR) =
    addFeature(accepted, features.toList(), type)

  fun addFeature(accepted: Boolean, features: List<String>, type: PmeigSecurity.Type = PmeigSecurity.Type.OR) {
    val key = GlobalSecurityKey(accepted, type)
    this.features[key] = (this.features[key] ?: mutableListOf()) + features
  }

  val accepted: List<SecurityFeatures> get() = listOf(
    SecurityFeatures(
      features[ACCEPTED_OR] ?: emptyList(),
      PmeigSecurity.Type.OR
    ), SecurityFeatures(features[ACCEPTED_AND] ?: emptyList(), PmeigSecurity.Type.AND))

  val denied: List<SecurityFeatures> get() = listOf(SecurityFeatures(features[DENIED_OR] ?: emptyList(), PmeigSecurity.Type.OR),
    SecurityFeatures(features[DENIED_AND] ?: emptyList(), PmeigSecurity.Type.AND))
}

internal data class GlobalSecurityKey(val accepted: Boolean, val type: PmeigSecurity.Type)
