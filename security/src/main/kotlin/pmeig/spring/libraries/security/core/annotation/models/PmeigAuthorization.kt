package pmeig.spring.libraries.security.core.annotation.models

import org.springframework.web.bind.annotation.RequestMethod
import pmeig.spring.libraries.security.core.annotation.PmeigSecurity
import pmeig.spring.libraries.security.core.models.SecurityFeatures

data class PmeigAuthorization(
  val path: Array<String> = emptyArray(),
  val methods: List<RequestMethod> = listOf(),
  val type: PmeigSecurity.Type = PmeigSecurity.Type.OR,
  var public: Boolean = false,
  var denied: Boolean = false,
  val accepted: MutableList<SecurityFeatures> = mutableListOf(),
  val rejected: MutableList<SecurityFeatures> = mutableListOf()
) {

  fun addAccepted(vararg features: String, type: PmeigSecurity.Type = PmeigSecurity.Type.OR) =
    addFeatures(type, features.toList()) { accepted }

  fun addRejected(vararg features: String, type: PmeigSecurity.Type = PmeigSecurity.Type.OR) =
    addFeatures(type, features.toList()) { rejected }

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

    other as PmeigAuthorization

    if (public != other.public) return false
    if (denied != other.denied) return false
    if (!path.contentEquals(other.path)) return false
    if (methods != other.methods) return false
    if (type != other.type) return false
    if (accepted != other.accepted) return false
    if (rejected != other.rejected) return false

    return true
  }

  override fun hashCode(): Int {
    var result = public.hashCode()
    result = 31 * result + denied.hashCode()
    result = 31 * result + path.contentHashCode()
    result = 31 * result + methods.hashCode()
    result = 31 * result + type.hashCode()
    result = 31 * result + accepted.hashCode()
    result = 31 * result + rejected.hashCode()
    return result
  }
}