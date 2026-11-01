package fmrv.widgetstudio.domain.repository

import fmrv.widgetstudio.domain.model.NotesSnapshot
import fmrv.widgetstudio.domain.model.RefreshResult
import fmrv.widgetstudio.domain.model.WidgetContentType

/** Source de contenu d'un widget de type liste (notes, actualités, e-mails). */
interface ItemsRepository {
    /** Lit le cache local : rapide, sans réseau, utilisable au rendu d'un widget. */
    suspend fun snapshot(): NotesSnapshot

    /** Synchronise le cache depuis le service distant. */
    suspend fun refresh(): RefreshResult

    /** Efface le cache (déconnexion). */
    suspend fun clear()
}

interface NotesRepository : ItemsRepository

interface NewsRepository : ItemsRepository

interface MailRepository : ItemsRepository

/** Retrouve la source correspondant à un type de widget liste. */
interface ListSources {
    fun forType(type: WidgetContentType): ItemsRepository?
}
