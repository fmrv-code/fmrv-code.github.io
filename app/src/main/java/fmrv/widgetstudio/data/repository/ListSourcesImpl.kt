package fmrv.widgetstudio.data.repository

import fmrv.widgetstudio.domain.model.WidgetContentType
import fmrv.widgetstudio.domain.repository.ItemsRepository
import fmrv.widgetstudio.domain.repository.ListSources
import fmrv.widgetstudio.domain.repository.MailRepository
import fmrv.widgetstudio.domain.repository.NewsRepository
import fmrv.widgetstudio.domain.repository.NotesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListSourcesImpl @Inject constructor(
    private val notes: NotesRepository,
    private val news: NewsRepository,
    private val mail: MailRepository,
) : ListSources {
    override fun forType(type: WidgetContentType): ItemsRepository? = when (type) {
        WidgetContentType.NOTES_LIST -> notes
        WidgetContentType.NEWS_FEED -> news
        WidgetContentType.MAIL_INBOX -> mail
        WidgetContentType.APP_SHORTCUT,
        WidgetContentType.APP_GRID,
        WidgetContentType.SEARCH_BAR,
        WidgetContentType.CLOCK_DATE,
        WidgetContentType.DEVICE_STATUS,
        WidgetContentType.WEATHER_CLOCK,
        -> null
    }
}
