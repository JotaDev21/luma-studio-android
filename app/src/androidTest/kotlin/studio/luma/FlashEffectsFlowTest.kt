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
    private fun ready()=compose.waitUntil(45000) {!compose.activity.studio.ui.value.busy && !compose.activity.studio.ui.value.rendering}
    @Test fun addAdjustSelectSecondFlashUndoAndSaveKeepTheCorrectEffect() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        compose.runOnUiThread {compose.activity.setShowWhenLocked(true);compose.activity.setTurnScreenOn(true);compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
        ready();val vm=compose.activity.studio;val store=StudioStore(compose.activity)
        val file=File(compose.activity.cacheDir,"flash-flow.jpg").apply {instrumentation.context.assets.open("portrait-nasa.jpg").use {input->outputStream().use {input.copyTo(it)}}}
        compose.runOnUiThread {vm.import(Uri.fromFile(file))};ready()
        val project=vm.ui.value.project!!;val bytes=store.source(project).readBytes()
        try {
            compose.onNodeWithTag("photo-tools").performScrollToNode(hasContentDescription("Efeitos"))
            compose.onNodeWithContentDescription("Efeitos").performClick()
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
}
