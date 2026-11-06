package com.example.data.model

import com.example.R

enum class RingtoneCategory(val title: String, val emoji: String) {
    ALL("All", "🔔"),
    MEME("Meme", "👾"),
    MELODIC("Melodic", "🎵"),
    NATURE("Nature", "🌿"),
    LOUD("Alert", "🚨")
}

data class RingtoneItem(
    val id: String,
    val name: String,
    val category: RingtoneCategory,
    val tag: String, // e.g. "👾 Meme"
    val subtitle: String,
    val durationText: String = "0:20",
    val rawResId: Int? = null,
    val speechText: String? = null,
    val isMeme: Boolean = (category == RingtoneCategory.MEME)
)

object RingtoneCatalog {
    val MEME_UTH_JA = RingtoneItem(
        id = "meme_uth_ja",
        name = "uthja bhai",
        category = RingtoneCategory.MEME,
        tag = "👾 Meme",
        subtitle = "Funny Hindi wake-up meme • High energy beat",
        durationText = "0:20",
        rawResId = R.raw.meme_uth_ja,
        speechText = "Uth ja! Uth ja! Uth re! Chal chal uth re, chal bahut ho gaya, uth! Uth ja chal, uth ja ab! Uth ja! Soyega? Gyarah baje tak so raha hai? Hain? Gyarah baj gaye abhi tak so raha hai?"
    )

    val MEME_KAB_TAK_SOYEGA = RingtoneItem(
        id = "meme_kab_tak_soyega",
        name = "kab tak soyega",
        category = RingtoneCategory.MEME,
        tag = "👾 Meme",
        subtitle = "Motivational wake-up meme • 'Kaash dhyan deta'",
        durationText = "0:20",
        rawResId = R.raw.meme_kab_tak_soyega,
        speechText = "Kab tak soyega? Aath baje tak? Das baje tak? Are main poochh raha hoon kab tak soyega? Maut ke aane tak? Aankhein dhundhli hone tak? Kab tak soyega? Tere khwabon ke blur hone tak? Saanson ko aadha jhoolne tak? Kab tak soyega yaar? Dekh duniya kitni aage bhaag gayi hai! Jo chaand aadha tha, wo poora ho gaya! Kitne sooraj ug kar wapas dhal gaye! Kitne mausam aaye aur chale gaye! Aur tu sirf sapne dekhne mein hi reh gaya! Kaash ke tu thoda unko poora karne par bhi dhyan deta!"
    )

    val MEME_ARPIT_BALA = RingtoneItem(
        id = "meme_arpit_bala",
        name = "Arpit Bala",
        category = RingtoneCategory.MEME,
        tag = "👾 Meme",
        subtitle = "Original wake-up audio • High energy",
        durationText = "0:10",
        rawResId = R.raw.arpit_bala,
        speechText = null
    )

    val ALL_RINGTONES: List<RingtoneItem> = listOf(
        MEME_UTH_JA,
        MEME_KAB_TAK_SOYEGA,
        MEME_ARPIT_BALA,
        RingtoneItem(
            id = "good_morning",
            name = "Good Morningggg",
            category = RingtoneCategory.MELODIC,
            tag = "🎵 Melodic",
            subtitle = "Gentle morning bells & warm chimes",
            durationText = "0:15"
        ),
        RingtoneItem(
            id = "time_for_school",
            name = "Time for School",
            category = RingtoneCategory.MELODIC,
            tag = "🎵 Melodic",
            subtitle = "Brisk upbeat chime progression",
            durationText = "0:12"
        ),
        RingtoneItem(
            id = "happy_morning",
            name = "Happy Morning",
            category = RingtoneCategory.MELODIC,
            tag = "🎵 Melodic",
            subtitle = "Joyful acoustic morning melody",
            durationText = "0:14"
        ),
        RingtoneItem(
            id = "angelic_wake",
            name = "Angelic Wake Up",
            category = RingtoneCategory.MELODIC,
            tag = "🎵 Melodic",
            subtitle = "Soft angelic harp & celestial tones",
            durationText = "0:18"
        ),
        RingtoneItem(
            id = "wake_lazy",
            name = "Wake up you lazy",
            category = RingtoneCategory.LOUD,
            tag = "🚨 Alert",
            subtitle = "Persistent rhythmic wake-up nudge",
            durationText = "0:10"
        ),
        RingtoneItem(
            id = "rain",
            name = "Rain",
            category = RingtoneCategory.NATURE,
            tag = "🌿 Nature",
            subtitle = "Calming ambient morning rain",
            durationText = "0:30"
        ),
        RingtoneItem(
            id = "ocean",
            name = "Ocean",
            category = RingtoneCategory.NATURE,
            tag = "🌿 Nature",
            subtitle = "Soothing morning ocean waves",
            durationText = "0:30"
        ),
        RingtoneItem(
            id = "forest",
            name = "Forest",
            category = RingtoneCategory.NATURE,
            tag = "🌿 Nature",
            subtitle = "Fresh woodland ambiance with birds",
            durationText = "0:25"
        ),
        RingtoneItem(
            id = "birds",
            name = "Birds",
            category = RingtoneCategory.NATURE,
            tag = "🌿 Nature",
            subtitle = "Crisp dawn bird songs",
            durationText = "0:20"
        ),
        RingtoneItem(
            id = "wind",
            name = "Wind",
            category = RingtoneCategory.NATURE,
            tag = "🌿 Nature",
            subtitle = "Mountain breeze & serene air",
            durationText = "0:25"
        )
    )

    fun findByNameOrId(name: String?): RingtoneItem {
        if (name.isNullOrBlank()) return MEME_UTH_JA
        val trimmed = name.trim()
        return ALL_RINGTONES.firstOrNull {
            it.name.equals(trimmed, ignoreCase = true) ||
            it.id.equals(trimmed, ignoreCase = true) ||
            (it.id == "meme_arpit_bala" && (trimmed.contains("arpit", ignoreCase = true) || trimmed.contains("bala", ignoreCase = true) || trimmed.contains("uthja_original", ignoreCase = true))) ||
            (it.id == "meme_uth_ja" && (trimmed.contains("uth", ignoreCase = true) || trimmed.contains("uthja", ignoreCase = true))) ||
            (it.id == "meme_kab_tak_soyega" && (trimmed.contains("kab", ignoreCase = true) || trimmed.contains("soyega", ignoreCase = true)))
        } ?: RingtoneItem(
            id = trimmed.lowercase().replace(" ", "_"),
            name = trimmed,
            category = RingtoneCategory.MELODIC,
            tag = "🎵 Custom",
            subtitle = "Alarm tone",
            durationText = "0:20"
        )
    }

    fun isMemeRingtone(sound: String?): Boolean {
        if (sound.isNullOrBlank()) return false
        val item = findByNameOrId(sound)
        return item.isMeme || sound.contains("meme", ignoreCase = true) || sound.contains("arpit", ignoreCase = true) || sound.contains("bala", ignoreCase = true) || sound.contains("uth", ignoreCase = true) || sound.contains("kab", ignoreCase = true) || sound.contains("soyega", ignoreCase = true)
    }
}
