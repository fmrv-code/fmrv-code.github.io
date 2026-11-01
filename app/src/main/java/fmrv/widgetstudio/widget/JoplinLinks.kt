package fmrv.widgetstudio.widget

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import fmrv.widgetstudio.domain.model.JOPLIN_PACKAGE
import fmrv.widgetstudio.domain.model.NoteSummary

/** Intents vers Joplin. L'ouverture directe d'une note dépend de la version de Joplin : repli sur l'application. */
object JoplinLinks {

    fun launchIntent(context: Context): Intent? =
        context.packageManager.getLaunchIntentForPackage(JOPLIN_PACKAGE)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Vrai si l'application Joplin installée sait ouvrir un lien `joplin://x-callback-url/openNote`. */
    fun supportsOpenNote(context: Context): Boolean =
        resolves(context.packageManager, noteLink("probe"))

    fun noteIntent(context: Context, note: NoteSummary, deepLinkSupported: Boolean): Intent? =
        if (deepLinkSupported) {
            noteLink(note.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            launchIntent(context)
        }

    private fun noteLink(noteId: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("joplin://x-callback-url/openNote?id=$noteId")).setPackage(JOPLIN_PACKAGE)

    private fun resolves(pm: PackageManager, intent: Intent): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0)) != null
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(intent, 0) != null
        }
}
