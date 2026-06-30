package com.gis.systrace.core.network.client

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class HttpResponse(
    val code: Int,
    val body: String?,
    val isSuccessful: Boolean,
    val rawBody: String? = body,
)

class SysTraceHttpClient {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun postJson(
        baseUrl: String,
        path: String,
        jsonBody: String,
        apiKey: String,
        pathParams: Map<String, String> = emptyMap(),
    ): HttpResponse {
        val url = buildUrl(baseUrl, path, pathParams)
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(JSON_MEDIA))
            .header("Content-Type", "application/json")
            .apply {
                if (apiKey.isNotBlank()) {
                    header("X-API-Key", apiKey)
                }
            }
            .build()
        return execute(request)
    }

    fun getJson(
        baseUrl: String,
        path: String,
        query: Map<String, String> = emptyMap(),
        apiKey: String,
        pathParams: Map<String, String> = emptyMap(),
    ): HttpResponse {
        val urlBuilder = StringBuilder(buildUrl(baseUrl, path, pathParams))
        if (query.isNotEmpty()) {
            urlBuilder.append('?')
            urlBuilder.append(query.entries.joinToString("&") { "${it.key}=${it.value}" })
        }
        val request = Request.Builder()
            .url(urlBuilder.toString())
            .get()
            .apply {
                if (apiKey.isNotBlank()) {
                    header("X-API-Key", apiKey)
                }
            }
            .build()
        return execute(request)
    }

    private fun execute(request: Request): HttpResponse {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            return HttpResponse(
                code = response.code,
                body = body,
                isSuccessful = response.isSuccessful,
                rawBody = body,
            )
        }
    }

    private fun buildUrl(baseUrl: String, path: String, pathParams: Map<String, String>): String {
        var resolvedPath = path
        pathParams.forEach { (key, value) ->
            resolvedPath = resolvedPath.replace("{$key}", value).replace(":$key", value)
        }
        val base = baseUrl.trimEnd('/')
        val normalized = resolvedPath.trimStart('/')
        return "$base/$normalized"
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
