package pmeig.spring.libraries.jpa.shared.model


@Suppress("unused")
class StructColumn(
  var name : String? = null,
  var age: Int = 0,
  var size: Long? = null
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as StructColumn

    if (age != other.age) return false
    if (size != other.size) return false
    if (name != other.name) return false

    return true
  }

  override fun hashCode(): Int {
    var result = age
    result = 31 * result + (size?.hashCode() ?: 0)
    result = 31 * result + (name?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return "StructColumn(name=$name, age=$age, size=$size)"
  }


}