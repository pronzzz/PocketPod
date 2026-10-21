package io.github.pronzzz.wheelhouse

import io.github.pronzzz.wheelhouse.util.Text
import org.junit.Assert.assertEquals
import org.junit.Test

class TextTest {

    @Test fun sortKeyIgnoresArticlesCaseAndAccents() {
        assertEquals("beatles", Text.sortKey("The Beatles"))
        assertEquals("beyonce", Text.sortKey("Beyoncé"))
        assertEquals("tribe called quest", Text.sortKey("A Tribe Called Quest"))
        assertEquals("killers", Text.sortKey("  the Killers"))
    }

    @Test fun sortKeyKeepsShortNamesThatLookLikeArticles() {
        assertEquals("the", Text.sortKey("The"))
        assertEquals("a", Text.sortKey("A"))
    }

    @Test fun sortKeySkipsLeadingPunctuation() {
        assertEquals("sorry", Text.sortKey("...Sorry"))
    }

    @Test fun indexLetter() {
        assertEquals('B', Text.indexLetter("The Beatles"))
        assertEquals('#', Text.indexLetter("2Pac"))
        assertEquals('#', Text.indexLetter(""))
    }

    @Test fun durationFormatting() {
        assertEquals("0:00", Text.duration(0))
        assertEquals("3:05", Text.duration(185_000))
        assertEquals("1:02:09", Text.duration(3_729_000))
        assertEquals("0:00", Text.duration(-5))
    }

    @Test fun cleanReplacesUnknownTags() {
        assertEquals("Unknown Artist", Text.clean("<unknown>", "Unknown Artist"))
        assertEquals("Unknown Artist", Text.clean("  ", "Unknown Artist"))
        assertEquals("Muse", Text.clean(" Muse ", "x"))
    }

    @Test fun plural() {
        assertEquals("1 Song", Text.plural(1, "Song"))
        assertEquals("3 Songs", Text.plural(3, "Song"))
    }
}
