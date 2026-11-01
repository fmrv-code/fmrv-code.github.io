package fmrv.widgetstudio.data.joplin

import fmrv.widgetstudio.domain.model.FailureReason

/** Fichier distant du dossier de synchronisation. [modified] est une date ISO 8601 (triable lexicographiquement). */
data class RemoteFile(
    val id: String,
    val name: String,
    val modified: String,
    val downloadUrl: String? = null,
)

class RemoteException(
    val reason: FailureReason,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message ?: reason.name, cause)

/** Accès en lecture seule au dossier de synchronisation Joplin (OneDrive aujourd'hui). */
interface RemoteFiles {
    /** Fichiers `.md` les plus récemment modifiés, du plus récent au plus ancien. */
    suspend fun listMarkdownFiles(limit: Int): List<RemoteFile>

    suspend fun read(file: RemoteFile): String
}

interface Connection {
    suspend fun isConnected(): Boolean
}
