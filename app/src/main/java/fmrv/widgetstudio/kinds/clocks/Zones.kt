package fmrv.widgetstudio.kinds.clocks

import fmrv.widgetstudio.kinds.ChoiceOption

/** Fuseaux proposés par l'horloge mondiale (identifiant IANA + nom de ville en français). */
object Zones {
    val all: List<Pair<String, String>> = listOf(
        "Europe/Paris" to "Paris",
        "Europe/London" to "Londres",
        "Europe/Berlin" to "Berlin",
        "Europe/Madrid" to "Madrid",
        "Europe/Rome" to "Rome",
        "Europe/Lisbon" to "Lisbonne",
        "Europe/Athens" to "Athènes",
        "Europe/Moscow" to "Moscou",
        "Europe/Istanbul" to "Istanbul",
        "Africa/Casablanca" to "Casablanca",
        "Africa/Algiers" to "Alger",
        "Africa/Dakar" to "Dakar",
        "Africa/Cairo" to "Le Caire",
        "Africa/Johannesburg" to "Johannesburg",
        "Asia/Dubai" to "Dubaï",
        "Asia/Kolkata" to "New Delhi",
        "Asia/Bangkok" to "Bangkok",
        "Asia/Shanghai" to "Pékin",
        "Asia/Tokyo" to "Tokyo",
        "Asia/Seoul" to "Séoul",
        "Australia/Sydney" to "Sydney",
        "Pacific/Auckland" to "Auckland",
        "Pacific/Tahiti" to "Papeete",
        "America/New_York" to "New York",
        "America/Toronto" to "Toronto",
        "America/Chicago" to "Chicago",
        "America/Denver" to "Denver",
        "America/Los_Angeles" to "Los Angeles",
        "America/Mexico_City" to "Mexico",
        "America/Sao_Paulo" to "São Paulo",
        "America/Argentina/Buenos_Aires" to "Buenos Aires",
    )

    val options: List<ChoiceOption> = all.map { (id, city) -> ChoiceOption(value = id, label = city) }

    /** Nom de la ville ; l'identifiant brut si le fuseau n'est pas dans la liste. */
    fun label(zoneId: String): String = all.firstOrNull { it.first == zoneId }?.second ?: zoneId.substringAfterLast('/')
}
