package fmrv.widgetstudio.kinds.feeds

import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.Category
import fmrv.widgetstudio.domain.model.GMAIL_PACKAGE
import fmrv.widgetstudio.domain.model.JOPLIN_PACKAGE
import fmrv.widgetstudio.domain.model.NoteSummary
import fmrv.widgetstudio.domain.model.NotesSnapshot
import fmrv.widgetstudio.domain.model.StylePresets
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.domain.model.WidgetStyle
import fmrv.widgetstudio.domain.repository.ItemsRepository
import fmrv.widgetstudio.kinds.LoadEnv
import fmrv.widgetstudio.kinds.RenderEnv
import fmrv.widgetstudio.kinds.WidgetKind
import fmrv.widgetstudio.render.Click
import fmrv.widgetstudio.render.Colors
import fmrv.widgetstudio.render.Img
import fmrv.widgetstudio.render.ImgSource
import fmrv.widgetstudio.render.Mod
import fmrv.widgetstudio.render.Node
import fmrv.widgetstudio.render.Pad
import fmrv.widgetstudio.render.ScrollList
import fmrv.widgetstudio.render.Text
import fmrv.widgetstudio.render.TextAlign
import fmrv.widgetstudio.render.TextWeight
import fmrv.widgetstudio.render.VAlign
import fmrv.widgetstudio.render.box
import fmrv.widgetstudio.render.column
import fmrv.widgetstudio.render.row
import fmrv.widgetstudio.widget.JoplinLinks
import fmrv.widgetstudio.widget.NotesRefreshWorker
import fmrv.widgetstudio.widget.WidgetEntryPoint

data class FeedData(val snapshot: NotesSnapshot, val joplinDeepLink: Boolean)

/** Ce qui distingue les trois listes : source de données, liens, textes. */
enum class FeedSource { NOTES, NEWS, MAIL }

/** Liste défilante alimentée par un service (notes Joplin, actualités Franceinfo, e-mails Gmail). */
class FeedListKind(
    private val source: FeedSource,
    override val id: String,
    override val title: Int,
    override val description: Int,
    override val defaultStyle: WidgetStyle,
) : WidgetKind<FeedData> {

    override val category = Category.INFO
    override val defaultSize = WidgetSizeOption.WIDE

    override fun dataSource(entry: WidgetEntryPoint): ItemsRepository = when (source) {
        FeedSource.NOTES -> entry.notesRepository()
        FeedSource.NEWS -> entry.newsRepository()
        FeedSource.MAIL -> entry.mailRepository()
    }

    override suspend fun load(env: LoadEnv): FeedData {
        val snapshot = dataSource(env.entry).snapshot()
        // Cache vide et ancien : on lance une actualisation en arrière-plan pour la prochaine fois.
        val stale = System.currentTimeMillis() - snapshot.refreshedAtMillis > RETRY_MS
        if (snapshot.connected && snapshot.notes.isEmpty() && stale) NotesRefreshWorker.enqueueNow(env.context)
        val deepLink = source == FeedSource.NOTES && JoplinLinks.supportsOpenNote(env.context)
        return FeedData(snapshot, deepLink)
    }

    private fun headerClick(): Click = when (source) {
        FeedSource.NOTES -> Click.LaunchApp(JOPLIN_PACKAGE)
        FeedSource.MAIL -> Click.LaunchApp(GMAIL_PACKAGE)
        FeedSource.NEWS -> Click.OpenUri("https://www.franceinfo.fr")
    }

    private fun itemClick(item: NoteSummary, deepLink: Boolean): Click? = when (source) {
        FeedSource.NOTES -> if (deepLink) {
            Click.OpenUri("joplin://x-callback-url/openNote?id=${item.id}", JOPLIN_PACKAGE)
        } else {
            Click.LaunchApp(JOPLIN_PACKAGE)
        }
        FeedSource.NEWS -> item.link?.takeIf { it.startsWith("https://") }?.let { Click.OpenUri(it) }
        FeedSource.MAIL -> Click.LaunchApp(GMAIL_PACKAGE)
    }

    override fun render(env: RenderEnv, data: FeedData): Node {
        val titleSp = Fit.listTitleSp(env.widthDp, env.heightDp)
        val showHeader = env.heightDp >= 80f
        val showSnippet = env.heightDp >= 100f
        val items = data.snapshot.notes

        return column(Mod.Fill.copy(pad = Pad.sym(horizontal = 12f, vertical = 8f))) {
            if (showHeader) {
                +row(Mod(fillWidth = true), v = VAlign.CENTER) {
                    +Text(
                        env.name.ifBlank { env.string(title) }, titleSp + 2f, env.text, TextWeight.BOLD, env.font,
                        mod = Mod(weight = 1f, click = headerClick()),
                    )
                    +Img(ImgSource.Res(R.drawable.ic_refresh), 20f, 20f, tint = env.text, mod = Mod(pad = Pad.all(8f), click = Click.Refresh))
                }
            }
            when {
                !data.snapshot.connected -> +hint(env, env.string(notConnectedText()), titleSp)
                items.isEmpty() -> +hint(env, env.string(R.string.notes_empty), titleSp)
                else -> +ScrollList(
                    items.map { item ->
                        column(Mod(fillWidth = true, pad = Pad.sym(vertical = 4f), click = itemClick(item, data.joplinDeepLink))) {
                            +Text(
                                item.title.ifBlank { env.string(R.string.notes_untitled) }, titleSp, env.text, TextWeight.MEDIUM, env.font,
                                mod = Mod(fillWidth = true),
                            )
                            if (showSnippet) {
                                val ago = RelativeTime.format(env, item.updatedMillis)
                                val line = listOf(ago, item.snippet).filter { it.isNotBlank() }.joinToString(" · ")
                                if (line.isNotEmpty()) {
                                    +Text(line, (titleSp * 0.8f).coerceIn(9f, 12f), Colors.withOpacity(env.text, 0.72f), font = env.font, mod = Mod(fillWidth = true))
                                }
                            }
                        }
                    },
                    Mod(fillWidth = true, fillHeight = true),
                )
            }
        }
    }

    private fun notConnectedText(): Int = if (source == FeedSource.MAIL) R.string.mail_not_connected else R.string.notes_not_connected

    private fun hint(env: RenderEnv, text: String, sp: Float): Node =
        box(Mod.Fill) { +Text(text, sp, Colors.withOpacity(env.text, 0.85f), font = env.font, align = TextAlign.CENTER, maxLines = 3) }

    private object Fit {
        fun listTitleSp(w: Float, h: Float): Float = (minOf(w, h) * 0.085f).coerceIn(11f, 15f)
    }

    private companion object {
        const val RETRY_MS = 5 * 60 * 1000L
    }
}

val NotesFeedKind = FeedListKind(
    FeedSource.NOTES, "feed.notes", R.string.kind_feed_notes, R.string.kind_feed_notes_desc, StylePresets.Coral.style,
)
val NewsFeedKind = FeedListKind(
    FeedSource.NEWS, "feed.news", R.string.kind_feed_news, R.string.kind_feed_news_desc, StylePresets.BlueNight.style,
)
val MailFeedKind = FeedListKind(
    FeedSource.MAIL, "feed.mail", R.string.kind_feed_mail, R.string.kind_feed_mail_desc, StylePresets.Lavender.style,
)
