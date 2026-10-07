package studio.luma

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
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
class AccessibilityFlowTest {
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
        val node=if(compose.onAllNodes(isDialog()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()) compose.onNode(isDialog()) else if(compose.onAllNodesWithTag("look-browser").fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()) compose.onNodeWithTag("look-browser") else compose.onNode(isRoot())
        File(path,name).outputStream().use {node.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    private fun withPhoto(fontScale:Float=1f,action:(StudioViewModel)->Unit) {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity)
        val file=File(compose.activity.cacheDir,"accessibility-portrait.jpg")
        InstrumentationRegistry.getInstrumentation().context.assets.open("portrait-nasa.jpg").use {input->file.outputStream().use {input.copyTo(it)}}
        val base=runBlocking {store.import(Uri.fromFile(file))};val bytes=store.source(base).readBytes()
        try {
            compose.runOnUiThread {vm.open(base)};ready()
            if(fontScale!=1f) {val density=compose.activity.resources.displayMetrics.density;compose.runOnUiThread {compose.activity.setContent {CompositionLocalProvider(LocalDensity provides Density(density,fontScale)) {StudioApp(vm)}}}}
            action(vm)
            assertArrayEquals(bytes,store.source(base).readBytes())
        } finally {compose.runOnUiThread {vm.closeEditor(true)};ready();runBlocking {store.deleteProject(base.id)};file.delete()}
    }
    @Test fun homeAndGuidedStoryPassAutomatedAccessibilityChecks() {
        compose.enableAccessibilityChecks()
        compose.onRoot().tryPerformAccessibilityChecks()
        val vm=compose.activity.studio
        compose.runOnUiThread {vm.createStory()};ready()
        compose.onNodeWithTag("story-return-guide").performClick()
        val id=vm.ui.value.project!!.id
        try {compose.onRoot().tryPerformAccessibilityChecks();compose.onNodeWithTag("story-guide-next").performScrollTo().assertHeightIsAtLeast(48.dp)}
        finally {compose.runOnUiThread {vm.closeEditor(true)};ready();runBlocking {StudioStore(compose.activity).deleteProject(id)}}
    }
    @Test fun photoEditorPassesAutomatedAccessibilityChecks()=withPhoto {_->
        compose.enableAccessibilityChecks()
        compose.onRoot().tryPerformAccessibilityChecks()
        compose.onNodeWithTag("photo-save").assertHeightIsAtLeast(48.dp)
    }
    @Test fun doubledFontCanSearchChooseAndAdjustFilter()=withPhoto(2f) {vm->
        compose.onNodeWithTag("all-filters").performScrollTo().performClick()
        compose.onNodeWithTag("look-search").performTextReplacement("Flash Verde 89")
        val id=vm.ui.value.filters.first {it.meta.name=="Flash Verde 89"}.meta.id
        compose.onNodeWithTag("look-grid").performScrollToNode(hasTestTag("look-$id"))
        compose.onNode(hasTestTag("look-$id") and hasAnyAncestor(hasTestTag("look-grid"))).assertIsDisplayed().performClick();ready()
        compose.onNode(hasTestTag("look-intensity") and hasAnyAncestor(hasTestTag("look-browser"))).performSemanticsAction(SemanticsActions.SetProgress) {it(.55f)};ready()
        assertEquals(id,vm.ui.value.edit.filter!!.preset.meta.id)
        assertEquals(.55f,vm.ui.value.edit.filter!!.intensity,.01f)
        capture("accessible-filters-font-200.png")
        compose.onNodeWithText("Usar este filtro").assertIsDisplayed().performClick()
    }
    @Test fun doubledFontCanChangeColorCurveAndReadItsControl()=withPhoto(2f) {vm->
        compose.onNodeWithContentDescription("Mais ferramentas").performScrollTo().performClick()
        compose.onNodeWithText("Cor e ferramentas avançadas").performScrollTo().performClick()
        compose.onNodeWithText("Cor Pro").performScrollTo().performClick()
        val slider=compose.onNodeWithTag("Meios-tons").performScrollTo()
        capture("accessible-color-before-font-200.png")
        slider.assertIsDisplayed().assertContentDescriptionEquals("Meios-tons").performSemanticsAction(SemanticsActions.SetProgress) {it(.6f)};ready()
        assertTrue(vm.ui.value.edit.color.curves.rgb.any {it.x==.5f && kotlin.math.abs(it.y-.6f)<.01f})
        capture("accessible-color-font-200.png")
        compose.onNodeWithText("Pronto").assertIsDisplayed().performClick()
    }
    @Test fun doubledFontCanPositionCircularMaskWithoutDragging()=withPhoto(2f) {vm->
        compose.onNodeWithContentDescription("Mais ferramentas").performScrollTo().performClick()
        compose.onNodeWithText("Máscaras e retoques").performScrollTo().performClick();ready()
        compose.onNodeWithTag("mask-add").performScrollToNode(hasText("+ Circular"))
        compose.onNodeWithText("+ Circular").performClick();ready()
        compose.onNodeWithTag("Centro horizontal").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {it(.3f)};ready()
        assertEquals(.3f,vm.maskPhotoEdit().masks.last().center.x,.01f)
        capture("accessible-mask-font-200.png")
        compose.onNodeWithText("Concluir máscaras").performScrollTo().performClick();ready()
        assertFalse(vm.ui.value.maskEditor)
    }
    @Test fun doubledFontCanCropWithoutDraggingAndUndoTheChange()=withPhoto(2f) {vm->
        val before=vm.ui.value.edit
        compose.onNode(hasText("Recortar") and hasAnyAncestor(hasTestTag("photo-tools"))).performScrollTo().performClick()
        compose.onNodeWithText("Ajustar bordas sem arrastar").performScrollTo().performClick()
        compose.onNodeWithTag("Borda esquerda").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {it(.1f)};ready()
        compose.onNodeWithTag("Borda superior").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {it(.15f)};ready()
        assertEquals(.1f,vm.ui.value.edit.transform.crop.left,.01f)
        assertEquals(.15f,vm.ui.value.edit.transform.crop.top,.01f)
        capture("accessible-crop-font-200.png")
        compose.onNodeWithText("Pronto").performClick()
        compose.runOnUiThread {vm.undo()};ready()
        assertEquals(.1f,vm.ui.value.edit.transform.crop.left,.01f)
        assertEquals(before.transform.crop.top,vm.ui.value.edit.transform.crop.top,.01f)
        compose.runOnUiThread {vm.undo()};ready()
        assertEquals(before,vm.ui.value.edit)
    }
    @Test fun reducedMotionPreferenceSurvivesClosingAndReopeningHelp()=withPhoto {_->
        val prefs=compose.activity.getSharedPreferences("accessibility",0)
        val before=prefs.getBoolean("reduceMotion",false)
        try {
            compose.onNodeWithContentDescription("Mais ferramentas").performClick()
            compose.onNodeWithText("Facilidade de uso").performScrollTo().performClick()
            val toggle=compose.onNodeWithTag("reduce-motion")
            if(!before) toggle.performClick()
            toggle.assertIsOn()
            compose.onNodeWithText("Entendi").performClick()
            compose.onNodeWithContentDescription("Mais ferramentas").performClick()
            compose.onNodeWithText("Facilidade de uso").performScrollTo().performClick()
            compose.onNodeWithTag("reduce-motion").assertIsOn()
            compose.onNodeWithText("Entendi").performClick()
        } finally {prefs.edit().putBoolean("reduceMotion",before).commit()}
    }
}
