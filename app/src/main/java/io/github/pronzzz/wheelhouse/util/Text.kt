package io.github.pronzzz.wheelhouse.util

import java.text.Normalizer
import java.util.Locale

object Text {
    private val articles = listOf("the ", "a ", "an ")
    private val diacritics = Regex("\\p{InCombiningDiacriticalMarks}+")

    /** Sort key that ignores case, accents, leading articles and punctuation ("The Beatles" -> "beatles"). */
    fun sortKey(s: String): String {
        var k = Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replace(diacritics, "").lowercase(Locale.ROOT)
        for (a in articles) if (k.startsWith(a) && k.length > a.length) { k = k.substring(a.length); break }
        val firstAlnum = k.indexOfFirst { it.isLetterOrDigit() }
        return if (firstAlnum > 0) k.substring(firstAlnum) else k
    }

    /** Index letter used by the fast-scroll overlay: A–Z, or # for everything else. */
    fun indexLetter(s: String): Char {
        val c = sortKey(s).firstOrNull()?.uppercaseChar() ?: return '#'
        return if (c in 'A'..'Z') c else '#'
    }

    /** 3:05, 1:02:09 */
    fun duration(ms: Long): String {
        val total = (ms.coerceAtLeast(0) / 1000)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.ROOT, "%d:%02d", m, s)
    }

    /** MediaStore uses "<unknown>" for missing tags. */
    fun clean(s: String?, fallback: String): String {
        val t = s?.trim()
        return if (t.isNullOrEmpty() || t == "<unknown>") fallback else t
    }

    fun plural(n: Int, one: String, many: String = one + "s") = "$n ${if (n == 1) one else many}"
}
