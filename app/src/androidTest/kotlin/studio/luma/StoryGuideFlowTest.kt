package studio.luma

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import studio.luma.core.*
import studio.luma.data.*
import studio.luma.ui.StudioApp
import java.io.File

@RunWith(AndroidJUnit4::class)
class StoryGuideFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun ready()=compose.waitUntil(45000) {!compose.activity.studio.ui.value.busy && !compose.activity.studio.ui.value.rendering}
    @Before fun show() {
        assertTrue(compose.activity.packageName.endsWith(".qa"))
        compose.runOnUiThread {compose.activity.setShowWhenLocked(true);compose.activity.setTurnScreenOn(true);compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
        compose.waitUntil(30000) {compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()};ready()
    }
    private fun capture(name:String) {
        val path=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: return
        File(path).mkdirs()
        val node=if(compose.onAllNodes(isDialog()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()) compose.onNode(isDialog()) else compose.onNode(isRoot())
        File(path,name).outputStream().use {node.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    private fun withStory(blank:Boolean=false,action:(StudioViewModel,StudioStore,Project,File)->Unit) {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity)
        val fixture=File(compose.activity.cacheDir,"guided-story-portrait.jpg")
        InstrumentationRegistry.getInstrumentation().context.assets.open("portrait-nasa.jpg").use {input->fixture.outputStream().use {input.copyTo(it)}}
        val template=if(blank) null else vm.ui.value.templates.first {it.document().missingPhotos==1 && it.document().layers.any {l->l.type=="text" && !l.locked}}
        compose.runOnUiThread {vm.createStory(template)};ready()
        compose.onNodeWithTag("story-return-guide").performClick()
        val project=vm.ui.value.project!!
        try {action(vm,store,project,fixture)}
        finally {vm.ui.value.exported?.let {compose.activity.contentResolver.delete(it,null,null)};compose.runOnUiThread {vm.closeEditor(true)};ready();runBlocking {store.deleteProject(project.id)};fixture.delete()}
    }
    @Test fun missingPhotoBlocksProgressThenTextAnd4KSaveCompleteWithoutAdvancedEditor()=withStory {vm,store,base,fixture->
        compose.onNodeWithTag("story-guide").assertExists()
        compose.onNodeWithTag("story-guide-next").performScrollTo().assertIsNotEnabled()
        val bytes=fixture.readBytes()
        compose.runOnUiThread {vm.addStoryPhotos(listOf(Uri.fromFile(fixture)))};ready()
        compose.onNodeWithTag("story-guide-next").performScrollTo().performClick()
        compose.onNodeWithTag("story-guide-text-1").performScrollTo().performClick()
        compose.onNodeWithTag("story-text-field").performTextReplacement("MEMÓRIAS DO MEU DIA")
        compose.onNodeWithText("Mais opções de texto").assertExists()
        compose.onNodeWithText("Aplicar").performClick();ready()
        assertTrue(vm.ui.value.edit.story!!.layers.any {it.content=="MEMÓRIAS DO MEU DIA"})
        capture("guided-story-texts.png")
        compose.onNodeWithTag("story-guide-next").performScrollTo().performClick()
        compose.onNodeWithTag("story-guide-save").performScrollTo().performClick()
        compose.onNodeWithTag("export-resolution").performScrollToNode(hasText("4K"))
        compose.onNode(hasText("4K") and hasAnyAncestor(hasTestTag("export-resolution"))).performClick()
        compose.onNodeWithText("Salvar na galeria").performClick();ready()
        assertNull(vm.ui.value.error)
        val size=ExportSize(vm.ui.value.edit.story!!.width,vm.ui.value.edit.story!!.height).atLongEdge(3840)
        compose.activity.contentResolver.openInputStream(vm.ui.value.exported!!).use {input->val image=BitmapFactory.decodeStream(input);assertEquals(size.width to size.height,image.width to image.height);image.recycle()}
        compose.onNodeWithText("Story salvo na galeria, na pasta Luma Studio.").assertExists()
        assertArrayEquals(bytes,fixture.readBytes())
        assertEquals(base.edit,runBlocking {store.list()}.first {it.id==base.id}.edit)
    }
    @Test fun backBetweenStepsAndReopeningRecoverEditableDraft()=withStory {vm,store,base,fixture->
        compose.runOnUiThread {vm.addStoryPhotos(listOf(Uri.fromFile(fixture)))};ready()
        compose.onNodeWithTag("story-guide-next").performScrollTo().performClick()
        compose.onNodeWithTag("story-guide-text-1").performScrollTo().performClick()
        compose.onNodeWithTag("story-text-field").performTextReplacement("CONTINUAR DEPOIS")
        compose.onNodeWithText("Aplicar").performClick();ready()
        compose.onNodeWithTag("story-guide-back").performScrollTo().performClick()
        val edit=vm.ui.value.edit
        assertEquals(0,edit.story!!.missingPhotos)
        compose.runOnUiThread {vm.keepDraftAndClose()};ready()
        compose.runOnUiThread {vm.open(runBlocking {store.list()}.first {it.id==base.id})};ready()
        compose.onNodeWithTag("story-return-guide").performClick()
        assertEquals(edit,vm.ui.value.edit)
        assertTrue(vm.ui.value.canUndo)
        compose.onNodeWithTag("story-guide").assertExists()
    }
    @Test fun doubledFontCanCreateTextOnlyStoryEditAndSave()=withStory(blank=true) {vm,_,_,_->
        val density=compose.activity.resources.displayMetrics.density
        compose.runOnUiThread {compose.activity.setContent {CompositionLocalProvider(LocalDensity provides Density(density,2f)) {StudioApp(vm)}}}
        compose.onNodeWithTag("story-return-guide").performScrollTo().performClick()
        compose.onNodeWithTag("story-guide-next").performScrollTo().performClick()
        compose.onNodeWithTag("story-guide-add-text").performScrollTo().performClick()
        compose.onNodeWithTag("story-text-field").performTextReplacement("UM DIA ESPECIAL")
        capture("guided-text-font-200.png")
        compose.onNodeWithText("Aplicar").assertIsDisplayed().performClick();ready()
        compose.onNodeWithTag("story-guide-next").performScrollTo().performClick()
        capture("guided-story-font-200.png")
        compose.onNodeWithTag("story-guide-save").performScrollTo().performClick()
        compose.onNodeWithText("Salvar na galeria").assertIsDisplayed().performClick();ready()
        assertNotNull(vm.ui.value.exported)
        assertNull(vm.ui.value.error)
    }
    @Test fun completeEditorHasButtonAlternativeToDraggingAndReturnsToGuide()=withStory(blank=true) {vm,_,_,_->
        compose.runOnUiThread {vm.addLayer("text","TESTE DE POSIÇÃO")};ready()
        val layer=vm.ui.value.edit.story!!.layers.last()
        compose.onNodeWithTag("story-guide-full").performScrollTo().performClick()
        compose.onNodeWithText("Ajustar texto").performClick()
        compose.onNodeWithContentDescription("Mover 10 pixels para a direita").performClick();ready()
        assertEquals(layer.transform.x+10,vm.ui.value.edit.story!!.layers.last().transform.x,.01f)
        compose.onNodeWithText("Pronto").performClick()
        compose.onNodeWithTag("story-return-guide").performClick()
        compose.onNodeWithTag("story-guide").assertExists()
    }
}
