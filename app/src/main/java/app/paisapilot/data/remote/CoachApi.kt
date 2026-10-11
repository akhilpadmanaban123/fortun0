package app.paisapilot.data.remote

import app.paisapilot.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CoachTurn(val role: String, val content: String) {
    companion object {
        const val USER = "user"
        const val COACH = "assistant"
    }
}

@Serializable
private data class CoachRequest(val context: String, val messages: List<CoachTurn>)

@Serializable
private data class CoachResponse(val reply: String? = null, val error: String? = null)

/** Why a question to the coach could not be answered; each maps to a message for the user. */
enum class CoachFailure {
    NOT_CONFIGURED, NOT_SIGNED_IN, DAILY_LIMIT, OFFLINE, UNAVAILABLE
}

class CoachException(val failure: CoachFailure, cause: Throwable? = null) : Exception(failure.name, cause)

/**
 * Talks to the coach service (server/coach-worker), which holds the AI provider's key.
 * The app never contacts the AI provider directly and never contains that key. The service
 * identifies the user from their Firebase sign-in token, so only signed-in, verified users
 * can use it and each user's daily allowance is enforced on the server.
 */
@Singleton
class CoachApi @Inject constructor(private val firebase: FirebaseProvider) {

    private val json = Json { ignoreUnknownKeys = true }

    val isConfigured: Boolean get() = BuildConfig.COACH_URL.isNotBlank()

    suspend fun ask(context: String, messages: List<CoachTurn>): Result<String> = runCatching {
        if (!isConfigured) throw CoachException(CoachFailure.NOT_CONFIGURED)
        val token = try {
            firebase.auth?.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            throw CoachException(CoachFailure.OFFLINE, e)
        } ?: throw CoachException(CoachFailure.NOT_SIGNED_IN)

        val body = json.encodeToString(CoachRequest.serializer(), CoachRequest(context, messages))
        withContext(Dispatchers.IO) { post(token, body) }
    }

    private fun post(token: String, body: String): String {
        val connection = try {
            (URL(BuildConfig.COACH_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                outputStream.use { it.write(body.toByteArray()) }
            }
        } catch (e: IOException) {
            throw CoachException(CoachFailure.OFFLINE, e)
        }
        try {
            val status = try {
                connection.responseCode
            } catch (e: IOException) {
                throw CoachException(CoachFailure.OFFLINE, e)
            }
            when (status) {
                HttpURLConnection.HTTP_OK -> {
                    val text = connection.inputStream.bufferedReader().use { it.readText() }
                    return json.decodeFromString(CoachResponse.serializer(), text).reply?.takeIf { it.isNotBlank() }
                        ?: throw CoachException(CoachFailure.UNAVAILABLE)
                }
                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN ->
                    throw CoachException(CoachFailure.NOT_SIGNED_IN)
                HTTP_TOO_MANY_REQUESTS -> throw CoachException(CoachFailure.DAILY_LIMIT)
                else -> throw CoachException(CoachFailure.UNAVAILABLE)
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        /** AI replies can take a while, especially on free models. */
        const val READ_TIMEOUT_MS = 60_000
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
