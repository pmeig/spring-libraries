package pmeig.spring.libraries.jpa.core.auditing

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import javax.sql.DataSource

@EnableJpaAuditing
@ConditionalOnClass(DataSource::class)
@Configuration
class JpaAuditingConfiguration {
}