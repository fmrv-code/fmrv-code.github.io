package fmrv.widgetstudio.data.mail

data class MailCredentials(val email: String, val appPassword: String)

data class MailSettings(val email: String = "", val connected: Boolean = false)

interface MailSource {
    suspend fun isConfigured(): Boolean

    suspend fun fetchUnread(limit: Int): List<MailMessage>
}
