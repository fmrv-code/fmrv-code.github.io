package fmrv.widgetstudio.viewmodel

import android.graphics.Bitmap
import fmrv.widgetstudio.R
import fmrv.widgetstudio.data.FakeWidgetInstanceDao
import fmrv.widgetstudio.data.repository.WidgetRepositoryImpl
import fmrv.widgetstudio.domain.model.AppInfo
import fmrv.widgetstudio.domain.model.BackgroundStyle
import fmrv.widgetstudio.domain.model.Params
import fmrv.widgetstudio.domain.model.PinResult
import fmrv.widgetstudio.domain.model.StylePresets
import fmrv.widgetstudio.domain.model.WidgetFont
import fmrv.widgetstudio.domain.model.WidgetInstance
import fmrv.widgetstudio.domain.model.WidgetShape
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.domain.repository.AppIconProvider
import fmrv.widgetstudio.domain.repository.AppsSource
import fmrv.widgetstudio.domain.repository.WidgetPinner
import fmrv.widgetstudio.domain.repository.WidgetUpdater
import fmrv.widgetstudio.kinds.KindCatalog
import fmrv.widgetstudio.kinds.WidgetKind
import fmrv.widgetstudio.kinds.apps.AppFolderKind
import fmrv.widgetstudio.kinds.apps.AppIconKind
import fmrv.widgetstudio.kinds.calendar.CountdownKind
import fmrv.widgetstudio.kinds.clocks.DigitalClockKind
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

    private val catalog = object : KindCatalog {
        override val all: List<WidgetKind<*>> = listOf(DigitalClockKind, CountdownKind, AppIconKind, AppFolderKind)
        override fun byId(id: String) = all.firstOrNull { it.id == id }
    }

    private val chrome = AppInfo("com.android.chrome", "Chrome")
    private val maps = AppInfo("com.google.maps", "Maps")

    private val repository = WidgetRepositoryImpl(FakeWidgetInstanceDao())
    private val updater = RecordingUpdater()
    private val pinner = FakePinner()

    private fun viewModel() = EditorViewModel(
        repository = repository,
        appsSource = FakeApps(listOf(chrome, maps)),
        icons = NoIcons(),
        updater = updater,
        pinner = pinner,
        catalog = catalog,
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
    fun `une creation demarre avec le style, les reglages et la taille du type`() {
        val vm = viewModel()
        vm.start(widgetId = 0, kindId = "clock.digital", presetPackage = null)

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals("clock.digital", state.instance?.kindId)
        assertEquals(DigitalClockKind.defaultStyle, state.instance?.style)
        assertEquals("1", state.instance?.params?.string("h24"))
        assertEquals(DigitalClockKind.defaultSize, state.pinSize)
        assertFalse(state.isPlaced)
        assertTrue(state.canSave)
    }

    @Test
    fun `un type inconnu ou absent demande de choisir un type`() {
        val vm = viewModel()
        vm.start(widgetId = 0, kindId = "nexiste.pas", presetPackage = null)
        assertTrue(vm.state.value.needsKind)
        assertFalse(vm.state.value.canSave)
    }

    @Test
    fun `un compte a rebours n'est enregistrable qu'avec une date valide`() {
        val vm = viewModel()
        vm.start(0, "calendar.countdown", null)
        assertFalse(vm.state.value.canSave)

        vm.setParam("date", "2026-12-25")

        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun `une icone pour une application pre-remplit l'application, son nom et le nom du widget`() {
        val vm = viewModel()
        vm.start(0, "app.icon", presetPackage = chrome.packageName)

        val instance = assertNotNull(vm.state.value.instance)
        assertEquals(chrome.packageName, instance.params.string("app"))
        assertEquals("Chrome", instance.params.string("app_label"))
        assertEquals("Chrome", instance.name)
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun `changer d'application conserve un nom personnalise mais suit sinon l'application`() {
        val vm = viewModel()
        vm.start(0, "app.icon", presetPackage = chrome.packageName)

        vm.selectApp("app", maps)
        assertEquals("Maps", vm.state.value.instance?.name)

        vm.setName("Mon trajet")
        vm.selectApp("app", chrome)
        assertEquals("Mon trajet", vm.state.value.instance?.name)
        assertEquals(chrome.packageName, vm.state.value.instance?.params?.string("app"))
    }

    @Test
    fun `un dossier exige au moins une application et plafonne a huit sans doublon`() {
        val vm = viewModel()
        vm.start(0, "app.folder", null)
        assertFalse(vm.state.value.canSave)

        repeat(12) { vm.addToList("apps", AppInfo("pkg.n$it", "App $it"), WidgetInstance.MAX_FOLDER_APPS) }
        vm.addToList("apps", AppInfo("pkg.n0", "App 0"), WidgetInstance.MAX_FOLDER_APPS)

        val apps = assertNotNull(vm.state.value.instance).params.list("apps")
        assertEquals(WidgetInstance.MAX_FOLDER_APPS, apps.size)
        assertEquals("pkg.n0", apps.first())
        assertTrue(vm.state.value.canSave)

        vm.removeFromList("apps", "pkg.n0")
        assertFalse("pkg.n0" in assertNotNull(vm.state.value.instance).params.list("apps"))
    }

    @Test
    fun `un style predefini remplace les couleurs mais garde la forme et la police`() {
        val vm = viewModel()
        vm.start(0, "clock.digital", null)
        vm.setStyle { it.copy(shape = WidgetShape.PILL, font = WidgetFont.SERIF) }

        vm.applyPreset(StylePresets.Coral)

        val style = assertNotNull(vm.state.value.instance).style
        assertEquals(BackgroundStyle.GRADIENT, style.background)
        assertEquals(StylePresets.Coral.style.colorStart, style.colorStart)
        assertEquals(WidgetShape.PILL, style.shape)
        assertEquals(WidgetFont.SERIF, style.font)
        assertTrue(StylePresets.Coral.isAppliedTo(style))
    }

    @Test
    fun `pinToHome memorise la configuration en attente et demande l'epinglage a la taille choisie`() = runTest {
        val vm = viewModel()
        vm.start(0, "clock.digital", null)
        vm.setPinSize(WidgetSizeOption.WIDE)

        vm.pinToHome()

        assertEquals(listOf(WidgetSizeOption.WIDE), pinner.requestedSizes)
        assertEquals(EditorEvent.PinRequested, vm.events.first())
        val adopted = assertNotNull(repository.adoptPending(77))
        assertEquals("clock.digital", adopted.kindId)
    }

    @Test
    fun `pinToHome sans reglage indispensable ne demande rien au lanceur`() {
        val vm = viewModel()
        vm.start(0, "calendar.countdown", null)

        vm.pinToHome()

        assertTrue(pinner.requestedSizes.isEmpty())
    }

    @Test
    fun `un lanceur sans epinglage direct produit un message d'erreur`() = runTest {
        pinner.result = PinResult.UNSUPPORTED
        val vm = viewModel()
        vm.start(0, "clock.digital", null)

        vm.pinToHome()

        assertEquals(EditorEvent.Message(R.string.error_pin_unsupported), vm.events.first())
    }

    @Test
    fun `save enregistre un widget pose, le redessine et le signale`() = runTest {
        repository.save(WidgetInstance(5, "clock.digital", "Ancien", params = Params(mapOf("h24" to "1"))))
        val vm = viewModel()
        vm.start(widgetId = 5, kindId = null, presetPackage = null)
        assertTrue(vm.state.value.isPlaced)

        vm.setBool("h24", false)
        vm.setName("Horloge du salon")
        vm.save()

        assertEquals(EditorEvent.Saved(5), vm.events.first())
        val saved = assertNotNull(repository.get(5))
        assertEquals("Horloge du salon", saved.name)
        assertEquals("0", saved.params.string("h24"))
        assertEquals(listOf(5), updater.refreshed)
    }

    @Test
    fun `un widget tout juste epingle reprend la configuration en attente sans interaction`() = runTest {
        repository.savePending(WidgetInstance(0, "calendar.countdown", params = Params(mapOf("date" to "2026-12-25"))))
        val vm = viewModel()

        vm.start(widgetId = 42, kindId = null, presetPackage = null)

        assertEquals(EditorEvent.Saved(42), vm.events.first())
        assertEquals("2026-12-25", repository.get(42)?.params?.string("date"))
        assertEquals(listOf(42), updater.refreshed)
    }

    @Test
    fun `un widget pose depuis le menu du lanceur propose d'abord de choisir un type`() {
        val vm = viewModel()
        vm.start(widgetId = 9, kindId = null, presetPackage = null)
        assertTrue(vm.state.value.needsKind)

        vm.chooseKind("clock.digital")

        val instance = assertNotNull(vm.state.value.instance)
        assertEquals(9, instance.appWidgetId)
        assertEquals("clock.digital", instance.kindId)
        assertTrue(vm.state.value.isPlaced)
        assertNull(vm.state.value.instance?.params?.values?.get("inconnu"))
    }

    @Test
    fun `start est idempotent`() {
        val vm = viewModel()
        vm.start(0, "clock.digital", null)
        vm.setName("Modifié")

        vm.start(0, "app.icon", null)

        assertEquals("Modifié", vm.state.value.instance?.name)
        assertEquals("clock.digital", vm.state.value.instance?.kindId)
    }
}
