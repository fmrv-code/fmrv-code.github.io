package fmrv.widgetstudio.kinds

import fmrv.widgetstudio.domain.model.Category
import fmrv.widgetstudio.kinds.apps.AppFolderKind
import fmrv.widgetstudio.kinds.apps.AppIconKind
import fmrv.widgetstudio.kinds.apps.AppPillKind
import fmrv.widgetstudio.kinds.apps.SearchBarKind
import fmrv.widgetstudio.kinds.calendar.BigDateKind
import fmrv.widgetstudio.kinds.calendar.CountdownKind
import fmrv.widgetstudio.kinds.calendar.MonthCalendarKind
import fmrv.widgetstudio.kinds.calendar.ProgressKind
import fmrv.widgetstudio.kinds.calendar.WeekStripKind
import fmrv.widgetstudio.kinds.clocks.AnalogClockKind
import fmrv.widgetstudio.kinds.clocks.DigitalClockKind
import fmrv.widgetstudio.kinds.clocks.StackedClockKind
import fmrv.widgetstudio.kinds.clocks.WorldClockKind
import fmrv.widgetstudio.kinds.device.DeviceStatusKind
import fmrv.widgetstudio.kinds.feeds.MailFeedKind
import fmrv.widgetstudio.kinds.feeds.NewsFeedKind
import fmrv.widgetstudio.kinds.feeds.NotesFeedKind

/** Catalogue de tous les types de widgets. Ajouter un type = l'ajouter ici. */
object Kinds {
    val all: List<WidgetKind<*>> = listOf(
        DigitalClockKind,
        StackedClockKind,
        AnalogClockKind,
        WorldClockKind,
        MonthCalendarKind,
        WeekStripKind,
        BigDateKind,
        CountdownKind,
        ProgressKind,
        AppIconKind,
        AppPillKind,
        AppFolderKind,
        SearchBarKind,
        DeviceStatusKind,
        NotesFeedKind,
        NewsFeedKind,
        MailFeedKind,
    )

    fun byId(id: String): WidgetKind<*>? = all.firstOrNull { it.id == id }

    fun inCategory(category: Category): List<WidgetKind<*>> = all.filter { it.category == category }
}
