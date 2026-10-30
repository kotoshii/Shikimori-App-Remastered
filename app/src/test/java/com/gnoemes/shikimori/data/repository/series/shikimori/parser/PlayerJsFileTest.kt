package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

//the shapes AnimeJoy publishes (BUGS #1, #59 N11)
class PlayerJsFileTest {

    @Test
    fun `tagged links become tracks, best quality first`() {
        val tracks = playerJsTracks("[360p]https://nod6.cdnjoyka.ru/a-360.mp4,[1080p]https://nod6.cdnjoyka.ru/a-1080.mp4")

        assertEquals(listOf(
                Track("1080", "https://nod6.cdnjoyka.ru/a-1080.mp4"),
                Track("360", "https://nod6.cdnjoyka.ru/a-360.mp4")
        ), tracks)
    }

    @Test
    fun `an untagged link has an unknown quality, not its url`() {
        val tracks = playerJsTracks("https://nod1.cdnjoyka.ru/2024/O/Dandadan/08.mp4")

        assertEquals(listOf(Track(Track.UNKNOWN_QUALITY, "https://nod1.cdnjoyka.ru/2024/O/Dandadan/08.mp4")), tracks)
    }

    @Test
    fun `a part that is not a full address is skipped`() {
        val tracks = playerJsTracks("[720p]/playlist/08.txt,[480p]https://nod6.cdnjoyka.ru/a-480.mp4")

        assertEquals(listOf(Track("480", "https://nod6.cdnjoyka.ru/a-480.mp4")), tracks)
    }

    @Test
    fun `no file has no tracks`() {
        assertTrue(playerJsTracks(null).isEmpty())
        assertTrue(playerJsTracks("").isEmpty())
    }
}
