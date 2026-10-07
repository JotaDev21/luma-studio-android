package studio.luma

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import studio.luma.data.StudioStore
import java.io.File

@RunWith(AndroidJUnit4::class)
class FlashEffectsFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun ready() {
        compose.waitUntil(45000) {!compose.activity.studio.ui.value.busy && !compose.activity.studio.ui.value.rendering}
        // These regression cases exercise the complete editor; the guide has separate tests.
        if(compose.onAllNodesWithTag("story-guide-full").fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()) compose.onNodeWithTag("story-guide-full").performScrollTo().performClick()
    }
    @Before fun show() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        compose.runOnUiThread {compose.activity.setShowWhenLocked(true);compose.activity.setTurnScreenOn(true);compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
        ready()
    }
    @Test fun addAdjustSelectSecondFlashUndoAndSaveKeepTheCorrectEffect() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        ready();val vm=compose.activity.studio;val store=StudioStore(compose.activity)
        val file=File(compose.activity.cacheDir,"flash-flow.jpg").apply {instrumentation.context.assets.open("portrait-nasa.jpg").use {input->outputStream().use {input.copyTo(it)}}}
        compose.runOnUiThread {vm.import(Uri.fromFile(file))};ready()
        val project=vm.ui.value.project!!;val bytes=store.source(project).readBytes()
        try {
            compose.onNode(hasText("Efeitos") and hasAnyAncestor(hasTestTag("photo-tools"))).assertIsDisplayed().performClick()
            compose.onNode(hasText("Flash") and hasAnyAncestor(hasTestTag("effect-categories"))).performClick()
            compose.onNodeWithTag("effect-flash-effect-bolso").performClick();ready()
            assertEquals("flash-effect-bolso",vm.ui.value.edit.effects.last().preset.meta.id)
            compose.onNodeWithText("Ajustar luz do flash").performClick()
            compose.onNodeWithTag("flash-radius").performTouchInput {click(centerRight)};ready()
            val adjusted=vm.ui.value.edit.effects.single();assertTrue(adjusted.preset.parameters.getValue("radius")>.8f)
            compose.onNodeWithText("Pronto").performClick()
            compose.onNodeWithTag("effect-layer-${adjusted.id}").performClick()
            compose.onNodeWithTag("effect-flash-effect-direto").performClick();ready()
            compose.onNodeWithTag("effect-intensity").performTouchInput {click(center)};ready()
            assertEquals(adjusted,vm.ui.value.edit.effects.first())
            assertTrue(vm.ui.value.edit.effects.last().intensity in .4f.. .6f)
            compose.onNodeWithText("Ajustar luz do flash").performClick()
            compose.onNodeWithTag("flash-hardness").performTouchInput {click(centerLeft)};ready()
            assertEquals(adjusted,vm.ui.value.edit.effects.first())
            assertTrue(vm.ui.value.edit.effects.last().preset.parameters.getValue("hardness")<.2f)
            compose.onNodeWithText("Pronto").performClick()
            val directory=File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")!!).apply {mkdirs()}
            File(directory,"flash-effects-editor.png").outputStream().use {compose.onNode(isRoot()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
            compose.runOnUiThread {vm.undo()};ready();assertEquals(.93f,vm.ui.value.edit.effects.last().preset.parameters.getValue("hardness"))
            compose.runOnUiThread {vm.save("Flash effects QA")};ready()
            val saved=runBlocking {store.list().first {it.id==project.id}}
            assertEquals(vm.ui.value.edit.effects,saved.edit.effects);assertArrayEquals(bytes,store.source(project).readBytes());assertNull(vm.ui.value.error)
        } finally {compose.runOnUiThread {vm.closeEditor(true)};ready();runBlocking {store.deleteProject(project.id)};file.delete()}
    }
    @Test fun storyFlashControlsScrollIntoViewAndSavePositionWithoutChangingThePhoto() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val vm=compose.activity.studio;val store=StudioStore(compose.activity)
        val file=File(compose.activity.cacheDir,"story-flash-flow.jpg").apply {instrumentation.context.assets.open("portrait-nasa.jpg").use {input->outputStream().use {input.copyTo(it)}}}
        compose.runOnUiThread {vm.createStory()};ready();val project=vm.ui.value.project!!
        try {
            compose.runOnUiThread {vm.addStoryPhotos(listOf(Uri.fromFile(file)))};ready()
            val photo=vm.ui.value.edit.story!!.layers.first {it.type=="photo"}
            val original=File(store.directory(project.id),photo.asset!!).readBytes()
            compose.onNodeWithTag("story-tools").performScrollToNode(hasText("Efeitos"))
            compose.onNode(hasText("Efeitos") and hasAnyAncestor(hasTestTag("story-tools"))).performClick()
            compose.onNode(hasText("Flash") and hasAnyAncestor(hasTestTag("effect-categories"))).performClick()
            compose.onNodeWithTag("effect-flash-effect-bolso").performClick();ready()
            compose.onNodeWithText("Ajustar luz do flash").assertIsDisplayed().performClick()
            compose.onNodeWithTag("flash-x").performScrollTo().performTouchInput {click(centerLeft)};ready()
            assertTrue(vm.ui.value.edit.effects.single().preset.parameters.getValue("x")<.2f)
            compose.onNodeWithText("Pronto").performClick();ready()
            compose.onNodeWithTag("effect-intensity").assertIsDisplayed().performTouchInput {click(center)};ready()
            assertTrue(vm.ui.value.edit.effects.single().intensity in .4f.. .6f)
            val intensityBounds=compose.onNodeWithTag("effect-intensity").fetchSemanticsNode().boundsInRoot
            val exportBounds=compose.onNodeWithText("Exportar Story").fetchSemanticsNode().boundsInRoot
            assertTrue("Intensity overlaps Story export",intensityBounds.bottom<=exportBounds.top)
            val directory=File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")!!).apply {mkdirs()}
            File(directory,"flash-effects-story.png").outputStream().use {compose.onNode(isRoot()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
            compose.runOnUiThread {vm.save("Story flash QA")};ready()
            val saved=runBlocking {store.list().first {it.id==project.id}}
            assertEquals(vm.ui.value.edit.effects,saved.edit.effects);assertEquals(photo,saved.edit.story!!.layers.first {it.type=="photo"})
            assertArrayEquals(original,File(store.directory(project.id),photo.asset!!).readBytes());assertNull(vm.ui.value.error)
        } finally {compose.runOnUiThread {vm.closeEditor(true)};ready();runBlocking {store.deleteProject(project.id)};file.delete()}
    }
}
