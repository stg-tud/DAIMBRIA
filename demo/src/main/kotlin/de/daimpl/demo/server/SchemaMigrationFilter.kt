package de.daimpl.demo.server

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.daimpl.daimbria.LensGraph
import de.daimpl.daimbria.TransformationEngine
import jakarta.servlet.*
import jakarta.servlet.FilterConfig
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.util.ContentCachingResponseWrapper
import java.io.IOException

class SchemaMigrationFilter(
    private val migrationEngine: TransformationEngine,
    private val lensGraph: LensGraph
) : Filter {

    private val mapper = ObjectMapper()

    companion object {
        const val SERVER_SCHEMA_VERSION = "1.2"
    }

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain
    ) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        // transform data in server schema version
        if (httpRequest.contentType == "application/json") {
            val clientSchemaVersion = httpRequest.getHeader("X-Schema-Version") ?: "1.0"
            val jsonNode: JsonNode = mapper.readTree(httpRequest.inputStream)

            val pathToServerSchema = lensGraph.lensFromTo(clientSchemaVersion, SERVER_SCHEMA_VERSION)
            val serverData = migrationEngine.applyMigrations(jsonNode, pathToServerSchema)

            httpRequest.setAttribute("transformedJson", serverData)
        }

        val wrappedResponse = ContentCachingResponseWrapper(httpResponse)

        // controller is called here
        chain.doFilter(request, wrappedResponse)

        // transform (modified) data back to client schema version
        val originalResponseContent = wrappedResponse.contentAsByteArray
        if (originalResponseContent.isNotEmpty()) {
            val originalResponse = String(originalResponseContent)

            val jsonNode: JsonNode = mapper.readTree(originalResponse)
            val clientSchemaVersion = httpRequest.getHeader("X-Schema-Version") ?: "1.0 - Fallback"
            val pathToClientSchema = lensGraph.lensFromTo(SERVER_SCHEMA_VERSION, clientSchemaVersion)
            val clientData = migrationEngine.applyMigrations(jsonNode, pathToClientSchema)

            val modifiedResponseContent = mapper.writeValueAsString(clientData)
            wrappedResponse.resetBuffer()
            wrappedResponse.writer.write(modifiedResponseContent)
        }

        wrappedResponse.copyBodyToResponse()
    }

    override fun init(filterConfig: FilterConfig?) {}

    override fun destroy() {}
}