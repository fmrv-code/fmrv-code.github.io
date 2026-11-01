package fmrv.widgetstudio.domain.model

/** Résumé d'une note Joplin affiché dans un widget. [id] est l'identifiant Joplin de la note. */
data class NoteSummary(
    val id: String,
    val title: String,
    val snippet: String,
    val updatedMillis: Long,
)

/** Dernier état connu des notes, lu depuis le cache local (jamais depuis le réseau). */
data class NotesSnapshot(
    val connected: Boolean,
    val notes: List<NoteSummary>,
    val refreshedAtMillis: Long,
)

enum class FailureReason { NETWORK, AUTH, FOLDER_NOT_FOUND, UNKNOWN }

sealed interface RefreshResult {
    data class Success(val noteCount: Int) : RefreshResult

    data object NotConnected : RefreshResult

    data class Failure(val reason: FailureReason) : RefreshResult
}

const val JOPLIN_PACKAGE = "net.cozic.joplin"
