package pmeig.spring.libraries.jpa.data.bigquery.mapper

import com.google.cloud.bigquery.TableResult
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class BigQueryEntityMapper {

  fun <T: Any> map(entity: KClass<T>, result: TableResult): List<T> = result.iterateAll().map { entity.java.getDeclaredConstructor().newInstance() }
}