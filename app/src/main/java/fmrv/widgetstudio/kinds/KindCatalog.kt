package fmrv.widgetstudio.kinds

import fmrv.widgetstudio.domain.model.Category
import javax.inject.Inject
import javax.inject.Singleton

/** Accès au catalogue des types de widgets, injectable (et remplaçable dans les tests). */
interface KindCatalog {
    val all: List<WidgetKind<*>>

    fun byId(id: String): WidgetKind<*>?

    fun inCategory(category: Category): List<WidgetKind<*>> = all.filter { it.category == category }
}

@Singleton
class DefaultKindCatalog @Inject constructor() : KindCatalog {
    override val all: List<WidgetKind<*>> get() = Kinds.all

    override fun byId(id: String): WidgetKind<*>? = Kinds.byId(id)
}
