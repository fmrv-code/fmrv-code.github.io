package fmrv.widgetstudio.viewmodel

import android.graphics.Bitmap
import fmrv.widgetstudio.R
import fmrv.widgetstudio.data.FakeWidgetConfigDao
import fmrv.widgetstudio.data.repository.WidgetRepositoryImpl
import fmrv.widgetstudio.domain.model.AppInfo
import fmrv.widgetstudio.domain.model.NoteSummary
import fmrv.widgetstudio.domain.model.NotesSnapshot
import fmrv.widgetstudio.domain.model.RefreshResult
import fmrv.widgetstudio.domain.model.PinResult
import fmrv.widgetstudio.domain.model.TemplateSection
import fmrv.widgetstudio.domain.model.WidgetTemplate
import fmrv.widgetstudio.domain.model.WidgetConfig
import fmrv.widgetstudio.domain.model.WidgetContentType
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.domain.repository.AppIconProvider
import fmrv.widgetstudio.domain.repository.AppsSource
import fmrv.widgetstudio.domain.repository.ItemsRepository
import fmrv.widgetstudio.domain.repository.ListSources
import fmrv.widgetstudio.domain.repository.WidgetPinner
import fmrv.widgetstudio.domain.repository.WidgetUpdater
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private class FakeApps(private val apps: List<AppInfo>) : AppsSource {
        override suspend fun loadApps() = apps
        override suspend fun labelOf(packageName: String) = apps.firstOrNull { it.packageName == packageName }?.label
    }

    private class NoIcons : AppIconProvider {
        override suspend fun icon(packageName: String, sizePx: Int): Bitmap? = null
    }

    private class FakeNotes(private val cached: List<NoteSummary> = emptyList()) : ItemsRepository {
        override suspend fun snapshot() = NotesSnapshot(connected = true, notes = cached, refreshedAtMillis = 1L)
        override suspend fun refresh(): RefreshResult = RefreshResult.Success(cached.size)
        override suspend fun clear() = Unit
    }

    private class FakeSources(private val cached: List<NoteSummary>) : ListSources {
        override fun forType(type: WidgetContentType): ItemsRepository? = if (type.isList) FakeNotes(cached) else null
    }

    private class RecordingUpdater : WidgetUpdater {
        val refreshed = mutableListOf<Int>()
        override suspend fun refresh(appWidgetId: Int) {
            refreshed += appWidgetId
        }
        override suspend fun refreshAll() = Unit
    }

    private class FakePinner(var result: PinResult = PinResult.REQUESTED) : WidgetPinner {
        val requestedSizes = mutableListOf<WidgetSizeOption>()
        override fun requestPin(size: WidgetSizeOption): PinResult {
            requestedSizes += size
            return result
        }
    }

    private val chrome = AppInfo("com.android.chrome", "Chrome")
    private val maps = AppInfo("com.google.maps", "Maps")

    private val repository = WidgetRepositoryImpl(FakeWidgetConfigDao())
    private val updater = RecordingUpdater()
    private val pinner = FakePinner()

    private fun viewModel(cachedNotes: List<NoteSummary> = emptyList()) = EditorViewModel(
        repository = repository,
        appsSource = FakeApps(listOf(chrome, maps)),
        icons = NoIcons(),
        updater = updater,
        pinner = pinner,
        sources = FakeSources(cachedNotes),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `une creation pour une application pre-remplit la cible et le nom`() {
        val vm = viewModel()
        vm.start(widgetId = 0, presetPackage = chrome.packageName)

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(chrome.packageName, state.config.targetPackage)
        assertEquals("Chrome", state.config.name)
        assertTrue(state.canSave)
        assertFalse(state.isPlaced)
    }

    @Test
    fun `un raccourci sans application ne peut pas etre enregistre mais l'horloge le peut`() {
        val vm = viewModel()
        vm.start(widgetId = 0, presetPackage = null)

        assertFalse(vm.state.value.canSave)
        vm.update { it.copy(contentType = WidgetContentType.CLOCK_DATE) }
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun `pinToHome sans application ne demande rien au lanceur`() {
        val vm = viewModel()
        vm.start(widgetId = 0, presetPackage = null)

        vm.pinToHome()

        assertTrue(pinner.requestedSizes.isEmpty())
    }

    @Test
    fun `pinToHome memorise la configuration en attente et demande l'epinglage`() = runTest {
        val vm = viewModel()
        vm.start(widgetId = 0, presetPackage = chrome.packageName)
        vm.setPinSize(WidgetSizeOption.WIDE)

        vm.pinToHome()

        assertEquals(listOf(WidgetSizeOption.WIDE), pinner.requestedSizes)
        assertEquals(EditorEvent.PinRequested, vm.events.first())
        val adopted = repository.adoptPending(77)
        assertNotNull(adopted)
        assertEquals(chrome.packageName, adopted.targetPackage)
    }

    @Test
    fun `un lanceur sans epinglage direct produit un message d'erreur`() = runTest {
        pinner.result = PinResult.UNSUPPORTED
        val vm = viewModel()
        vm.start(widgetId = 0, presetPackage = chrome.packageName)

        vm.pinToHome()

        assertEquals(EditorEvent.Message(R.string.error_pin_unsupported), vm.events.first())
    }

    @Test
    fun `save enregistre un widget pose, le redessine et le signale`() = runTest {
        repository.save(WidgetConfig(appWidgetId = 5, name = "Ancien", targetPackage = chrome.packageName, targetLabel = "Chrome"))
        val vm = viewModel()
        vm.start(widgetId = 5, presetPackage = null)
        assertTrue(vm.state.value.isPlaced)

        vm.update { it.copy(name = "  Navigation  ", opacity = 0.5f) }
        vm.save()

        assertEquals(EditorEvent.Saved(5), vm.events.first())
        assertEquals("Navigation", repository.get(5)?.name)
        assertEquals(0.5f, repository.get(5)?.opacity)
        assertEquals(listOf(5), updater.refreshed)
    }

    @Test
    fun `un nom vide est remplace par le nom de l'application`() = runTest {
        repository.save(WidgetConfig(appWidgetId = 5, targetPackage = chrome.packageName, targetLabel = "Chrome"))
        val vm = viewModel()
        vm.start(widgetId = 5, presetPackage = null)

        vm.update { it.copy(name = "   ") }
        vm.save()
        vm.events.first()

        assertEquals("Chrome", repository.get(5)?.name)
    }

    @Test
    fun `un widget tout juste epingle reprend la configuration en attente sans interaction`() = runTest {
        repository.savePending(
            WidgetConfig(appWidgetId = 0, name = "Maps", targetPackage = maps.packageName, targetLabel = "Maps"),
        )
        val vm = viewModel()

        vm.start(widgetId = 42, presetPackage = null)

        assertEquals(EditorEvent.Saved(42), vm.events.first())
        assertEquals(maps.packageName, repository.get(42)?.targetPackage)
        assertEquals(listOf(42), updater.refreshed)
        assertEquals(42, vm.state.value.config.appWidgetId)
    }

    @Test
    fun `un widget pose sans configuration en attente ouvre un editeur vierge`() {
        val vm = viewModel()
        vm.start(widgetId = 9, presetPackage = null)

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(9, state.config.appWidgetId)
        assertNull(state.config.targetPackage)
        assertFalse(state.canSave)
    }

    @Test
    fun `changer d'application conserve un nom personnalise mais suit sinon l'application`() {
        val vm = viewModel()
        vm.start(widgetId = 0, presetPackage = chrome.packageName)

        vm.selectApp(maps)
        assertEquals("Maps", vm.state.value.config.name)

        vm.update { it.copy(name = "Mon trajet") }
        vm.selectApp(chrome)
        assertEquals("Mon trajet", vm.state.value.config.name)
        assertEquals(chrome.packageName, vm.state.value.config.targetPackage)
    }

    @Test
    fun `start est idempotent`() {
        val vm = viewModel()
        vm.start(widgetId = 0, presetPackage = chrome.packageName)
        vm.update { it.copy(name = "Modifié") }

        vm.start(widgetId = 0, presetPackage = maps.packageName)

        assertEquals("Modifié", vm.state.value.config.name)
        assertEquals(chrome.packageName, vm.state.value.config.targetPackage)
    }

    @Test
    fun `une liste de notes peut etre enregistree sans application`() {
        val vm = viewModel()
        vm.start(widgetId = 0, presetPackage = null)

        vm.update { it.copy(contentType = WidgetContentType.NOTES_LIST) }

        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun `l'apercu reprend les notes en cache`() {
        val cached = listOf(NoteSummary("a", "Courses", "Pain", 10L))
        val vm = viewModel(cachedNotes = cached)
        vm.start(widgetId = 0, presetPackage = null)

        vm.update { it.copy(contentType = WidgetContentType.NOTES_LIST) }

        assertEquals(cached, vm.state.value.previewNotes)
    }

    @Test
    fun `les actualites et les e-mails se passent aussi d'application`() {
        val vm = viewModel()
        vm.start(widgetId = 0, presetPackage = null)

        vm.update { it.copy(contentType = WidgetContentType.NEWS_FEED) }
        assertTrue(vm.state.value.canSave)

        vm.update { it.copy(contentType = WidgetContentType.MAIL_INBOX) }
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun `changer de type recharge l'apercu`() {
        val cached = listOf(NoteSummary("a", "Titre", "Résumé", 10L))
        val vm = viewModel(cachedNotes = cached)
        vm.start(widgetId = 0, presetPackage = null)
        assertTrue(vm.state.value.previewNotes.isEmpty()) // type par défaut : raccourci, pas de liste

        vm.update { it.copy(contentType = WidgetContentType.NEWS_FEED) }

        assertEquals(cached, vm.state.value.previewNotes)
    }

    @Test
    fun `un dossier exige au moins une application et plafonne a huit`() {
        val vm = viewModel()
        vm.start(widgetId = 0, presetPackage = null)
        vm.update { it.copy(contentType = WidgetContentType.APP_GRID) }
        assertFalse(vm.state.value.canSave)

        repeat(12) { vm.addPackage(AppInfo("pkg.n$it", "App $it")) }

        assertTrue(vm.state.value.canSave)
        assertEquals(WidgetConfig.MAX_GRID_APPS, vm.state.value.config.packages.size)
        assertEquals("pkg.n0", vm.state.value.config.packages.first())
    }

    @Test
    fun `une application ajoutee deux fois n'apparait qu'une fois et peut etre retiree`() {
        val vm = viewModel()
        vm.start(widgetId = 0, presetPackage = null)
        vm.update { it.copy(contentType = WidgetContentType.APP_GRID) }

        vm.addPackage(chrome)
        vm.addPackage(chrome)
        vm.addPackage(maps)
        assertEquals(listOf(chrome.packageName, maps.packageName), vm.state.value.config.packages)

        vm.removePackage(chrome.packageName)
        assertEquals(listOf(maps.packageName), vm.state.value.config.packages)
    }

    @Test
    fun `un modele fournit la configuration et la taille de depart`() {
        val template = WidgetTemplate(
            id = "pill",
            title = 0,
            subtitle = 0,
            section = TemplateSection.APPS,
            config = WidgetConfig(
                appWidgetId = WidgetConfig.NO_ID,
                contentType = WidgetContentType.DEVICE_STATUS,
                accentColor = 0xFFE5484D.toInt(),
            ),
            size = fmrv.widgetstudio.domain.model.WidgetSizeOption.BAR,
        )
        val vm = viewModel()

        vm.start(widgetId = 0, presetPackage = null, template = template)

        assertEquals(WidgetContentType.DEVICE_STATUS, vm.state.value.config.contentType)
        assertEquals(0xFFE5484D.toInt(), vm.state.value.config.accentColor)
        assertEquals(fmrv.widgetstudio.domain.model.WidgetSizeOption.BAR, vm.state.value.pinSize)
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun `une barre de recherche exige une application`() {
        val vm = viewModel()
        vm.start(widgetId = 0, presetPackage = null)
        vm.update { it.copy(contentType = WidgetContentType.SEARCH_BAR) }
        assertFalse(vm.state.value.canSave)

        vm.selectApp(chrome)
        assertTrue(vm.state.value.canSave)
    }
}
