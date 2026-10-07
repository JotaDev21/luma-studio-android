package studio.luma

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.luma.core.ExportSize
import studio.luma.data.ExportOptions
import studio.luma.ui.ExportDialog
import studio.luma.ui.LumaTheme

@RunWith(AndroidJUnit4::class)
class ExportDialogTest {
    @get:Rule val compose=createComposeRule()
    private fun choose(row:String,value:String) {
        if(row=="export-publication" && compose.onAllNodesWithTag(row).fetchSemanticsNodes().isEmpty()) compose.onNodeWithTag("export-advanced").performClick()
        compose.onNodeWithTag(row).performScrollTo().performScrollToNode(hasText(value))
        compose.onNodeWithText(value).performClick()
    }
    @Test fun recommendedSaveDoesNotRequireChoosingTechnicalOptions() {
        var selected:ExportOptions?=null
        compose.setContent {LumaTheme {ExportDialog({}) {selected=it}}}
        compose.onNodeWithTag("export-publication").assertDoesNotExist()
        compose.onNodeWithText("Formato").assertDoesNotExist()
        compose.onNodeWithText("Salvar na galeria").assertIsDisplayed().performClick()
        compose.runOnIdle {assertEquals(ExportOptions(),selected)}
    }

    @Test fun storyPresetAndResolutionProduceTheSameDimensionsInEitherSelectionOrder() {
        var selected:ExportOptions?=null
        compose.setContent {LumaTheme {ExportDialog({},story=true,canvasSize=ExportSize(1080,1920)) {selected=it}}}
        choose("export-resolution","4K")
        choose("export-publication","Story")
        compose.onNodeWithTag("export-size").assertTextEquals("Saída: 2160 × 3840 px")
        compose.onNodeWithText("Salvar na galeria").performClick()
        compose.runOnIdle {assertEquals(ExportOptions(maxEdge=3840,width=2160,height=3840),selected)}
        choose("export-resolution","1440p")
        compose.onNodeWithTag("export-size").assertTextEquals("Saída: 1440 × 2560 px")
        compose.onNodeWithText("Salvar na galeria").performClick()
        compose.runOnIdle {assertEquals(ExportOptions(maxEdge=2560,width=1440,height=2560),selected)}
    }

    @Test fun customDimensionsStayExactAfterChoosing4K() {
        var selected:ExportOptions?=null
        compose.setContent {LumaTheme {ExportDialog({}) {selected=it}}}
        choose("export-resolution","4K")
        choose("export-publication","Personalizado")
        compose.onNodeWithText("Largura").performScrollTo().performTextReplacement("1500")
        compose.onNodeWithText("Altura").performScrollTo().performTextReplacement("2000")
        compose.onNodeWithTag("export-size").performScrollTo().assertTextEquals("Saída: 1500 × 2000 px")
        compose.onNodeWithText("4K").assertDoesNotExist()
        compose.onNodeWithText("Salvar na galeria").performClick()
        compose.runOnIdle {assertEquals(ExportOptions(width=1500,height=2000),selected)}
    }
}
