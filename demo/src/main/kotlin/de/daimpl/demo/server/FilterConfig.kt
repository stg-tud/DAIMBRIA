package de.daimpl.demo.server

import de.daimpl.daimbria.LensGraph
import de.daimpl.daimbria.TransformationEngine
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FilterConfig {

    @Bean
    fun schemaMigrationFilter(
        migrationEngine: TransformationEngine,
        lensGraph: LensGraph
    ): FilterRegistrationBean<SchemaMigrationFilter> {
        val registrationBean = FilterRegistrationBean<SchemaMigrationFilter>()
        registrationBean.filter = SchemaMigrationFilter(migrationEngine, lensGraph)
        registrationBean.addUrlPatterns("/api/*")
        registrationBean.order = 1
        return registrationBean
    }
}