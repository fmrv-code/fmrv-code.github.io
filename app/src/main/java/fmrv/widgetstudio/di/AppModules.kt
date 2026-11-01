package fmrv.widgetstudio.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fmrv.widgetstudio.data.apps.AppAssetProvider
import fmrv.widgetstudio.data.apps.AppIconLoader
import fmrv.widgetstudio.data.apps.InstalledAppsSource
import fmrv.widgetstudio.data.db.AppDatabase
import fmrv.widgetstudio.data.db.WidgetInstanceDao
import fmrv.widgetstudio.data.feed.FeedFetcher
import fmrv.widgetstudio.data.feed.FeedUrls
import fmrv.widgetstudio.data.feed.HttpFeedFetcher
import fmrv.widgetstudio.data.feed.NewsRepositoryImpl
import fmrv.widgetstudio.data.feed.NewsSettingsStore
import fmrv.widgetstudio.data.joplin.Connection
import fmrv.widgetstudio.data.joplin.FileNotesCacheStore
import fmrv.widgetstudio.data.joplin.JoplinNotesRepository
import fmrv.widgetstudio.data.joplin.NotesCacheStore
import fmrv.widgetstudio.data.joplin.OneDriveAuth
import fmrv.widgetstudio.data.joplin.OneDriveRemoteFiles
import fmrv.widgetstudio.data.joplin.RemoteFiles
import fmrv.widgetstudio.data.mail.ImapMailSource
import fmrv.widgetstudio.data.mail.MailRepositoryImpl
import fmrv.widgetstudio.data.mail.MailSource
import fmrv.widgetstudio.data.repository.WidgetRepositoryImpl
import fmrv.widgetstudio.domain.repository.AppIconProvider
import fmrv.widgetstudio.kinds.DefaultKindCatalog
import fmrv.widgetstudio.kinds.KindCatalog
import fmrv.widgetstudio.domain.repository.AppsSource
import fmrv.widgetstudio.domain.repository.MailRepository
import fmrv.widgetstudio.domain.repository.NewsRepository
import fmrv.widgetstudio.domain.repository.NotesRepository
import fmrv.widgetstudio.domain.repository.WidgetPinner
import fmrv.widgetstudio.domain.repository.WidgetRepository
import fmrv.widgetstudio.domain.repository.WidgetUpdater
import fmrv.widgetstudio.render.AssetProvider
import fmrv.widgetstudio.widget.GlanceWidgetPinner
import fmrv.widgetstudio.widget.GlanceWidgetUpdater
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "widget_studio.db")
            // Version 2 : modèle de widget entièrement refondu. Les widgets déjà posés affichent
            // « Touchez pour configurer » et se reconfigurent ; plus sûr qu'une migration non testable ici.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    fun provideWidgetInstanceDao(database: AppDatabase): WidgetInstanceDao = database.widgetInstanceDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun bindWidgetRepository(impl: WidgetRepositoryImpl): WidgetRepository

    @Binds abstract fun bindAppsSource(impl: InstalledAppsSource): AppsSource

    @Binds abstract fun bindAppIconProvider(impl: AppIconLoader): AppIconProvider

    @Binds abstract fun bindAssetProvider(impl: AppAssetProvider): AssetProvider

    @Binds abstract fun bindKindCatalog(impl: DefaultKindCatalog): KindCatalog

    @Binds abstract fun bindWidgetPinner(impl: GlanceWidgetPinner): WidgetPinner

    @Binds abstract fun bindWidgetUpdater(impl: GlanceWidgetUpdater): WidgetUpdater

    @Binds abstract fun bindNotesRepository(impl: JoplinNotesRepository): NotesRepository

    @Binds abstract fun bindRemoteFiles(impl: OneDriveRemoteFiles): RemoteFiles

    @Binds abstract fun bindNotesCacheStore(impl: FileNotesCacheStore): NotesCacheStore

    @Binds abstract fun bindConnection(impl: OneDriveAuth): Connection

    @Binds abstract fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository

    @Binds abstract fun bindFeedFetcher(impl: HttpFeedFetcher): FeedFetcher

    @Binds abstract fun bindFeedUrls(impl: NewsSettingsStore): FeedUrls

    @Binds abstract fun bindMailRepository(impl: MailRepositoryImpl): MailRepository

    @Binds abstract fun bindMailSource(impl: ImapMailSource): MailSource
}
