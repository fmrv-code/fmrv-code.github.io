package fmrv.widgetstudio.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import fmrv.widgetstudio.domain.repository.MailRepository
import fmrv.widgetstudio.domain.repository.NewsRepository
import fmrv.widgetstudio.domain.repository.NotesRepository
import fmrv.widgetstudio.domain.repository.WidgetRepository
import fmrv.widgetstudio.domain.repository.WidgetUpdater
import fmrv.widgetstudio.render.AssetProvider

/** Accès Hilt pour les composants instanciés par le système (récepteurs, Glance) et pour les types de widgets. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetRepository(): WidgetRepository

    fun widgetUpdater(): WidgetUpdater

    fun assetProvider(): AssetProvider

    fun notesRepository(): NotesRepository

    fun newsRepository(): NewsRepository

    fun mailRepository(): MailRepository
}

fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)
