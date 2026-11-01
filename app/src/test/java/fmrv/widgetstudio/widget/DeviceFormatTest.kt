package fmrv.widgetstudio.widget

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceFormatTest {

    @Test
    fun `la taille est formatee en Go avec la virgule francaise`() {
        assertEquals("314,1 Go", DeviceFormat.size(314_100_000_000L, Locale.FRANCE))
        assertEquals("2,4 Go", DeviceFormat.size(2_400_000_000L, Locale.FRANCE))
    }

    @Test
    fun `sous un Go la taille passe en Mo`() {
        assertEquals("850 Mo", DeviceFormat.size(850_000_000L, Locale.FRANCE))
        assertEquals("0 Mo", DeviceFormat.size(0L, Locale.FRANCE))
    }

    @Test
    fun `la part utilisee se deduit de l'espace libre`() {
        assertEquals(0.25f, DeviceFormat.usedFraction(free = 75, total = 100))
        assertEquals(0f, DeviceFormat.usedFraction(free = 100, total = 100))
        assertEquals(1f, DeviceFormat.usedFraction(free = 0, total = 100))
    }

    @Test
    fun `un total inconnu ou incoherent donne zero ou reste borne`() {
        assertEquals(0f, DeviceFormat.usedFraction(free = 10, total = 0))
        assertEquals(0f, DeviceFormat.usedFraction(free = 200, total = 100))
    }

    @Test
    fun `la jauge garde la place de l'icone et celle du texte`() {
        assertEquals(34f, DeviceFormat.gaugeFillDp(trackDp = 200f, fraction = 0f, minDp = 34f, reserveDp = 72f))
        assertEquals(128f, DeviceFormat.gaugeFillDp(trackDp = 200f, fraction = 1f, minDp = 34f, reserveDp = 72f))
        // Piste trop étroite : on ne descend jamais sous la pastille d'icône.
        assertEquals(34f, DeviceFormat.gaugeFillDp(trackDp = 60f, fraction = 1f, minDp = 34f, reserveDp = 72f))
    }
}
