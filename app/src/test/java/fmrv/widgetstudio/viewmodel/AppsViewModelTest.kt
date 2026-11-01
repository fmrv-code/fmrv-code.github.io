package fmrv.widgetstudio.viewmodel

import fmrv.widgetstudio.domain.model.AppInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class AppsViewModelTest {
    private val apps = listOf(
        AppInfo("com.android.chrome", "Chrome"),
        AppInfo("com.whatsapp", "WhatsApp"),
        AppInfo("com.samsung.android.calendar", "Agenda"),
    )

    @Test
    fun `une requete vide retourne toutes les applications`() {
        assertEquals(apps, filterApps(apps, "  "))
    }

    @Test
    fun `le filtre ignore la casse et cherche dans le nom`() {
        assertEquals(listOf(apps[1]), filterApps(apps, "whats"))
    }

    @Test
    fun `le filtre cherche aussi dans le nom de package`() {
        assertEquals(listOf(apps[2]), filterApps(apps, "samsung"))
    }

    @Test
    fun `aucun resultat donne une liste vide`() {
        assertEquals(emptyList(), filterApps(apps, "zzz"))
    }
}
