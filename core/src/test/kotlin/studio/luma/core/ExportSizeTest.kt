package studio.luma.core

import kotlin.test.*

class ExportSizeTest {
    @Test fun storyResolutionIsExactEvenWhenTheSourceIsSmaller() {
        for (source in listOf(ExportSize(1080, 1920), ExportSize(360, 640))) {
            assertEquals(ExportSize(1080, 1920), source.atLongEdge(1920))
            assertEquals(ExportSize(1440, 2560), source.atLongEdge(2560))
            assertEquals(ExportSize(2160, 3840), source.atLongEdge(3840))
        }
    }

    @Test fun landscapeFeedAndSquareKeepTheirProportions() {
        assertEquals(ExportSize(3840, 2160), ExportSize(1920, 1080).atLongEdge(3840))
        assertEquals(ExportSize(2048, 2560), ExportSize(1080, 1350).atLongEdge(2560))
        assertEquals(ExportSize(3840, 3840), ExportSize(1080, 1080).atLongEdge(3840))
        assertEquals(ExportSize(1080, 1920), ExportSize(1080, 1920).atLongEdge(0))
    }

    @Test fun fittingUsesTheContentAspectRatioRatherThanTheContainerLongEdge() {
        val story = ExportSize(1080, 1920)
        assertEquals(3840, story.fittedLongEdge(ExportSize(2160, 3840)))
        assertEquals(3840, story.fittedLongEdge(ExportSize(3840, 3840)))
        assertEquals(2160, story.fittedLongEdge(ExportSize(3840, 2160)))
        assertEquals(1350, story.fittedLongEdge(ExportSize(1080, 1350)))
    }

    @Test fun oversizedAndPartialDimensionsAreRejectedWithoutFallback() {
        assertTrue(ExportSize(2160, 3840).isValid)
        assertTrue(ExportSize(3840, 3840).isValid)
        assertFalse(ExportSize(8192, 8192).isValid)
        assertFalse(ExportSize(0, 1920).isValid)
        assertFalse(ExportSize(63, 1920).isValid)
        assertFailsWith<IllegalArgumentException> { ExportSize(1080, 1920).atLongEdge(-1) }
    }
}
