package fmrv.widgetstudio.kinds

import androidx.annotation.StringRes
import fmrv.widgetstudio.domain.model.Params

/** Choix d'une liste : libellé tiré des ressources ([labelRes]) ou texte direct ([label]). */
data class ChoiceOption(
    val value: String,
    @StringRes val labelRes: Int = 0,
    val label: String = "",
)

/** Réglage proposé par l'éditeur. Le formulaire est généré : un nouveau type de widget n'a pas d'écran à écrire. */
sealed interface ParamField {
    val key: String

    @get:StringRes
    val label: Int

    /** Le réglage n'est affiché que si cette condition sur les paramètres est vraie. */
    val visible: (Params) -> Boolean

    data class Toggle(
        override val key: String,
        @StringRes override val label: Int,
        override val visible: (Params) -> Boolean = { true },
    ) : ParamField

    data class Choice(
        override val key: String,
        @StringRes override val label: Int,
        val options: List<ChoiceOption>,
        override val visible: (Params) -> Boolean = { true },
    ) : ParamField

    data class Text(
        override val key: String,
        @StringRes override val label: Int,
        @StringRes val hint: Int = 0,
        val maxLength: Int = 40,
        override val visible: (Params) -> Boolean = { true },
    ) : ParamField

    /** Une application ; enregistre aussi son nom sous `<key>_label`. */
    data class AppPick(
        override val key: String,
        @StringRes override val label: Int,
        override val visible: (Params) -> Boolean = { true },
    ) : ParamField

    /** Plusieurs applications (dossier). */
    data class AppList(
        override val key: String,
        @StringRes override val label: Int,
        val max: Int,
        override val visible: (Params) -> Boolean = { true },
    ) : ParamField

    /** Une date, stockée au format AAAA-MM-JJ. */
    data class Date(
        override val key: String,
        @StringRes override val label: Int,
        override val visible: (Params) -> Boolean = { true },
    ) : ParamField
}

/** Clé sous laquelle le nom lisible d'une application choisie est mémorisé. */
fun appLabelKey(key: String): String = key + "_label"
