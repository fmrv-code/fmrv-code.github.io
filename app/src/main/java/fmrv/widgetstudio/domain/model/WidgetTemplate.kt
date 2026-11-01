package fmrv.widgetstudio.domain.model

import androidx.annotation.StringRes

enum class TemplateSection { APPS, INFO, DEVICE }

/**
 * Modèle de widget proposé dans la galerie : un type, un style de départ et une taille conseillée.
 * [config] est la configuration initiale (sans identifiant de widget).
 */
data class WidgetTemplate(
    val id: String,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val section: TemplateSection,
    val config: WidgetConfig,
    val size: WidgetSizeOption,
)
