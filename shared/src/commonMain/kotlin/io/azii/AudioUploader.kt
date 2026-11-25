package io.azii

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AudioUploader(
    private val serverUrl: String
) {

    private val client = HttpClient()

    suspend fun upload(audio: ByteArray): String {

        require(audio.isNotEmpty()) {
            "Audio is empty"
        }

        val response = client.post(serverUrl) {
            contentType(ContentType.parse("audio/wav"))
            setBody(audio)
        }

        val responseBody = response.bodyAsText()

        if (response.status.value !in 200..299) {
            throw Exception(
                "Server error ${response.status.value}: $responseBody"
            )
        }

        return responseBody
    }

    fun close() {
        client.close()
    }
}