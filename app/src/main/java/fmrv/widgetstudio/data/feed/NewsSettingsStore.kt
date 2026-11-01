package fmrv.widgetstudio.data.feed

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.feedsDataStore by preferencesDataStore(name = "feeds")

/** Adresse du flux Franceinfo « À la une », modifiable au cas où la rédaction la changerait. */
@Singleton
class NewsSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : FeedUrls {

    val customUrl: Flow<String> = context.feedsDataStore.data.map { it[NEWS_URL].orEmpty() }

    suspend fun setCustomUrl(value: String) {
        context.feedsDataStore.edit { it[NEWS_URL] = value.trim() }
    }

    override suspend fun candidates(): List<String> {
        val custom = context.feedsDataStore.data.first()[NEWS_URL].orEmpty().trim()
        return (listOf(custom) + DEFAULT_URLS).filter { it.startsWith("https://") }.distinct()
    }

    companion object {
        val DEFAULT_URLS = listOf(
            "https://www.francetvinfo.fr/titres.rss",
            "https://www.franceinfo.fr/titres.rss",
        )
        private val NEWS_URL = stringPreferencesKey("news_url")
    }
}
