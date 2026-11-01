package fmrv.widgetstudio.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.GMAIL_PACKAGE
import fmrv.widgetstudio.domain.model.NoteSummary
import fmrv.widgetstudio.domain.model.WidgetContentType

/** Intents et textes propres à chaque type de widget liste. */
object ListLinks {

    @StringRes
    fun defaultTitle(type: WidgetContentType): Int = when (type) {
        WidgetContentType.NEWS_FEED -> R.string.type_news
        WidgetContentType.MAIL_INBOX -> R.string.type_mail
        else -> R.string.type_notes
    }

    @StringRes
    fun notConnectedHint(type: WidgetContentType): Int = when (type) {
        WidgetContentType.MAIL_INBOX -> R.string.mail_not_connected
        else -> R.string.notes_not_connected
    }

    /** Appui sur le titre du widget : ouvre l'application (ou le site) du service. */
    fun headerIntent(context: Context, type: WidgetContentType): Intent? = when (type) {
        WidgetContentType.NOTES_LIST -> JoplinLinks.launchIntent(context)
        WidgetContentType.MAIL_INBOX -> launch(context, GMAIL_PACKAGE)
        WidgetContentType.NEWS_FEED -> view("https://www.franceinfo.fr")
        else -> null
    }

    /** Appui sur un élément : la note dans Joplin, l'article dans le navigateur ou l'application Franceinfo, ou Gmail. */
    fun itemIntent(context: Context, type: WidgetContentType, item: NoteSummary, joplinDeepLink: Boolean): Intent? =
        when (type) {
            WidgetContentType.NOTES_LIST -> JoplinLinks.noteIntent(context, item, joplinDeepLink)
            WidgetContentType.NEWS_FEED -> item.link?.takeIf { it.startsWith("https://") }?.let(::view)
            WidgetContentType.MAIL_INBOX -> launch(context, GMAIL_PACKAGE)
            else -> null
        }

    private fun launch(context: Context, packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun view(url: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
