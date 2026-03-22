package pmeig.spring.libraries.security.core.annotation.models

import org.springframework.web.bind.annotation.RequestMethod
import pmeig.spring.libraries.security.core.annotation.PmeigSecurity
import pmeig.spring.libraries.security.core.authorization.SecurityAuthorization
import pmeig.spring.libraries.security.core.models.SecurityFeatures

data class AnnotationAuthorization(
  val path: List<String> = emptyList(),
  val methods: List<RequestMethod> = listOf(),
  var public: Boolean = false,
  var denied: Boolean = false,
  val accepted: MutableList<SecurityFeatures> = mutableListOf(),
  val rejected: MutableList<SecurityFeatures> = mutableListOf()
) {

  fun addAccepted(vararg features: String, type: PmeigSecurity.Type = PmeigSecurity.Type.OR) =
    addFeatures(type, features.toList()) { accepted }

  fun addRejected(vararg features: String, type: PmeigSecurity.Type = PmeigSecurity.Type.OR) =
    addFeatures(type, features.toList()) { rejected }

  fun toSecuritiesAuthorization(): List<SecurityAuthorization> {
    return accepted.map {
      toSecurityAuthorization(true, it)
    } + rejected.map {
      toSecurityAuthorization(false, it)
    }
  }

  private fun toSecurityAuthorization(contain: Boolean, features: SecurityFeatures) =
    SecurityAuthorization(path, methods, features.features.toSet(), public, denied, contain, features.type)

  private fun addFeatures(
    type: PmeigSecurity.Type,
    newFeatures: List<String>,
    getter: () -> MutableList<SecurityFeatures>
  ) {
    val features = getter()
    var index = features.indexOfFirst { it.type == type }
    index = if (index == -1) features.size else index
    features[index] = SecurityFeatures(features[index].features + newFeatures, type)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AnnotationAuthorization

    if (public != other.public) return false
    if (denied != other.denied) return false
    if (!path.containsAll(other.path)) return false
    if (methods != other.methods) return false
    if (accepted != other.accepted) return false
    if (rejected != other.rejected) return false

    return true
  }

  override fun hashCode(): Int {
    var result = public.hashCode()
    result = 31 * result + denied.hashCode()
    result = 31 * result + path.hashCode()
    result = 31 * result + methods.hashCode()
    result = 31 * result + accepted.hashCode()
    result = 31 * result + rejected.hashCode()
    return result
  }
}