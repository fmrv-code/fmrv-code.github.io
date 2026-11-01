package fmrv.widgetstudio.domain.repository

import fmrv.widgetstudio.domain.model.NotesSnapshot
import fmrv.widgetstudio.domain.model.RefreshResult

interface NotesRepository {
    /** Lit le cache local : rapide, sans réseau, utilisable au rendu d'un widget. */
    suspend fun snapshot(): NotesSnapshot

    /** Synchronise le cache depuis le service distant. */
    suspend fun refresh(): RefreshResult

    /** Efface le cache (déconnexion). */
    suspend fun clear()
}
