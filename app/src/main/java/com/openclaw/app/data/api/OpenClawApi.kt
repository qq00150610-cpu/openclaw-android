package com.openclaw.app.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.openclaw.app.data.model.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenClawApi(
    private var endpoint: String = "",
    private var apiKey: String = "",
    private var model: String = "gpt-4o-mini"
) {
    private val gson: Gson = GsonBuilder().create()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Active streaming call for cancellation
    @Volatile
    private var activeCall: Call? = null

    fun updateConfig(endpoint: String, apiKey: String, model: String) {
        this.endpoint = endpoint.trimEnd('/')
        this.apiKey = apiKey
        this.model = model
    }

    fun isConfigured(): Boolean = endpoint.isNotBlank() && apiKey.isNotBlank()

    /**
     * Cancel any active streaming request
     */
    fun cancelActiveRequest() {
        activeCall?.cancel()
        activeCall = null
    }

    /**
     * Non-streaming chat completion
     */
    fun sendMessage(messages: List<ApiMessage>): Result<String> {
        if (!isConfigured()) return Result.failure(IOException("API 未配置"))

        val request = ChatRequest(
            model = model,
            messages = messages,
            stream = false
        )

        val httpRequest = buildRequest(request)
        return try {
            val response = client.newCall(httpRequest).execute()
            val body = response.body?.string() ?: return Result.failure(IOException("空响应"))

            if (!response.isSuccessful) {
                val error = try {
                    gson.fromJson(body, ChatResponse::class.java)?.error
                } catch (e: Exception) { null }
                return Result.failure(IOException(error?.message ?: "HTTP ${response.code}"))
            }

            val chatResponse = gson.fromJson(body, ChatResponse::class.java)
            val content = chatResponse.choices?.firstOrNull()?.message?.content
            // Handle both String and List<ContentPart> content
            val textContent = when (content) {
                is String -> content
                else -> content?.toString() ?: ""
            }
            Result.success(textContent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Streaming chat completion - calls onChunk for each token.
     * Returns the Call so it can be cancelled.
     */
    fun sendMessageStream(
        messages: List<ApiMessage>,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isConfigured()) {
            onError("API 未配置")
            return
        }

        val request = ChatRequest(
            model = model,
            messages = messages,
            stream = true
        )

        val httpRequest = buildRequest(request)
        val call = client.newCall(httpRequest)
        activeCall = call

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activeCall = null
                if (call.isCanceled()) return // Intentional cancellation
                onError(e.message ?: "网络错误")
            }

            override fun onResponse(call: Call, response: Response) {
                activeCall = null
                if (!response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val error = try {
                        gson.fromJson(body, ChatResponse::class.java)?.error
                    } catch (e: Exception) { null }
                    onError(error?.message ?: "HTTP ${response.code}")
                    return
                }

                try {
                    val reader = response.body?.byteStream()?.bufferedReader()
                        ?: throw IOException("空响应")

                    processStream(reader, onChunk)
                    reader.close()
                    onComplete()
                } catch (e: Exception) {
                    if (e is IOException && e.message?.contains("canceled") == true) return
                    onError(e.message ?: "流处理错误")
                }
            }
        })
    }

    private fun processStream(reader: BufferedReader, onChunk: (String) -> Unit) {
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            // Check for cancellation
            if (Thread.currentThread().isInterrupted) break

            val l = line ?: continue
            if (!l.startsWith("data: ")) continue
            val data = l.removePrefix("data: ").trim()
            if (data == "[DONE]") return

            try {
                val json = JsonParser.parseString(data).asJsonObject
                val choices = json.getAsJsonArray("choices") ?: continue
                if (choices.size() == 0) continue

                val delta = choices[0].asJsonObject.getAsJsonObject("delta") ?: continue
                val content = delta.getAsJsonPrimitive("content")?.asString ?: continue
                onChunk(content)
            } catch (e: Exception) {
                // Skip malformed chunks
            }
        }
    }

    private fun buildRequest(chatRequest: ChatRequest): Request {
        val body = gson.toJson(chatRequest)
            .toRequestBody("application/json".toMediaType())

        return Request.Builder()
            .url("$endpoint/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(body)
            .build()
    }
}
