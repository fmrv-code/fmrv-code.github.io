package fmrv.widgetstudio.data.joplin

import fmrv.widgetstudio.domain.model.FailureReason
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class DriveItem(
    val id: String,
    val name: String,
    val lastModifiedDateTime: String = "",
    val file: JsonElement? = null,
    @SerialName("@microsoft.graph.downloadUrl") val downloadUrl: String? = null,
)

@Serializable
internal data class DriveChildren(
    val value: List<DriveItem> = emptyList(),
    @SerialName("@odata.nextLink") val nextLink: String? = null,
)

/** Lecture seule du dossier de synchronisation Joplin sur OneDrive via Microsoft Graph. */
@Singleton
class OneDriveRemoteFiles @Inject constructor(
    private val auth: OneDriveAuth,
    private val settings: JoplinSettingsStore,
    private val http: HttpClient,
    private val json: Json,
) : RemoteFiles {

    override suspend fun listMarkdownFiles(limit: Int): List<RemoteFile> {
        val token = auth.accessToken()
        val base = OneDriveEndpoints.childrenUrl(settings.current().folderPath)

        var sorted = true
        var url: String? = "$base?\$top=$PAGE_SIZE&\$orderby=lastModifiedDateTime%20desc"
        var scanned = 0
        val files = ArrayList<RemoteFile>()

        while (url != null && scanned < MAX_SCANNED) {
            val response = http.get(url, bearer = token)
            when {
                response.code == 404 -> throw RemoteException(FailureReason.FOLDER_NOT_FOUND)
                response.code == 401 || response.code == 403 -> throw RemoteException(FailureReason.AUTH)
                response.code == 400 && sorted && files.isEmpty() -> {
                    // Tri côté serveur refusé : on parcourt tout et on trie localement.
                    sorted = false
                    url = "$base?\$top=$PAGE_SIZE"
                    continue
                }
                !response.isSuccess -> throw RemoteException(FailureReason.UNKNOWN, "HTTP ${response.code}")
            }
            val page = try {
                json.decodeFromString<DriveChildren>(response.body)
            } catch (e: SerializationException) {
                throw RemoteException(FailureReason.UNKNOWN, cause = e)
            }
            scanned += page.value.size
            page.value
                .filter { it.file != null && it.name.endsWith(".md", ignoreCase = true) }
                .mapTo(files) { RemoteFile(it.id, it.name, it.lastModifiedDateTime, it.downloadUrl) }

            if (sorted && files.size >= limit) break
            url = page.nextLink
        }
        return files.sortedByDescending { it.modified }.take(limit)
    }

    override suspend fun read(file: RemoteFile): String {
        // L'URL de téléchargement est pré-signée : elle ne doit pas recevoir le jeton d'accès.
        val direct = file.downloadUrl
        if (direct != null) return download(direct)

        val redirect = http.get(OneDriveEndpoints.contentUrl(file.id), bearer = auth.accessToken(), followRedirects = false)
        val location = redirect.location
        return when {
            redirect.isSuccess -> redirect.body
            redirect.code in 300..399 && location != null -> download(location)
            redirect.code == 401 || redirect.code == 403 -> throw RemoteException(FailureReason.AUTH)
            else -> throw RemoteException(FailureReason.UNKNOWN, "HTTP ${redirect.code}")
        }
    }

    private suspend fun download(url: String): String {
        val response = http.get(url)
        if (!response.isSuccess) throw RemoteException(FailureReason.UNKNOWN, "HTTP ${response.code}")
        return response.body
    }

    private companion object {
        const val PAGE_SIZE = 200
        const val MAX_SCANNED = 3000
    }
}
