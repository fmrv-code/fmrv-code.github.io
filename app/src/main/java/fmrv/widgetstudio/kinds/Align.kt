package fmrv.widgetstudio.kinds

import fmrv.widgetstudio.render.HAlign
import fmrv.widgetstudio.render.TextAlign

/** Alignement choisi dans un réglage ("start" | "center" | "end"). */
internal fun hAlignOf(value: String): HAlign = when (value) {
    "start" -> HAlign.START
    "end" -> HAlign.END
    else -> HAlign.CENTER
}

internal fun textAlignOf(value: String): TextAlign = when (value) {
    "start" -> TextAlign.START
    "end" -> TextAlign.END
    else -> TextAlign.CENTER
}
