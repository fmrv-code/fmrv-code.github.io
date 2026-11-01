package fmrv.widgetstudio.data.joplin

import fmrv.widgetstudio.domain.model.FailureReason
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HttpResponse(val code: Int, val body: String, val location: String?) {
    val isSuccess: Boolean get() = code in 200..299
}

/** Client HTTPS minimal (HttpURLConnection) : évite une dépendance réseau pour quelques requêtes. */
@Singleton
class HttpClient @Inject constructor() {

    suspend fun get(url: String, bearer: String? = null, followRedirects: Boolean = true): HttpResponse =
        request(url, "GET", bearer, body = null, followRedirects = followRedirects)

    suspend fun postForm(url: String, params: Map<String, String>): HttpResponse {
        val body = params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        return request(url, "POST", bearer = null, body = body, followRedirects = true)
    }

    private suspend fun request(
        url: String,
        method: String,
        bearer: String?,
        body: String?,
        followRedirects: Boolean,
    ): HttpResponse = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://")) {
            throw RemoteException(FailureReason.UNKNOWN, "Seul HTTPS est autorisé")
        }
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = followRedirects
                setRequestProperty("Accept", "application/json, text/plain, */*")
                if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            HttpResponse(code, text, connection.getHeaderField("Location"))
        } catch (e: IOException) {
            throw RemoteException(FailureReason.NETWORK, cause = e)
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
    }
}
