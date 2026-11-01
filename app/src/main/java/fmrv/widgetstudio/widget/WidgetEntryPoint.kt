package fmrv.widgetstudio.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import fmrv.widgetstudio.domain.repository.AppIconProvider
import fmrv.widgetstudio.domain.repository.WidgetRepository
import fmrv.widgetstudio.domain.repository.WidgetUpdater

/** Accès Hilt pour les composants instanciés par le système (receivers, Glance). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetRepository(): WidgetRepository

    fun iconProvider(): AppIconProvider

    fun widgetUpdater(): WidgetUpdater
}

fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)
