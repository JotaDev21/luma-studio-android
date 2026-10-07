package studio.luma

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
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
import studio.luma.renderer.ImageSource
import java.io.File

@RunWith(AndroidJUnit4::class)
class EnhancementFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun ready()=compose.waitUntil(180000) {!compose.activity.studio.ui.value.busy && !compose.activity.studio.ui.value.rendering}
    @Before fun show() {
        assertTrue(InstrumentationRegistry.getInstrumentation().targetContext.packageName.endsWith(".qa"))
        compose.runOnUiThread {compose.activity.setShowWhenLocked(true);compose.activity.setTurnScreenOn(true);compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
        compose.waitUntil(30000) {compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()};ready()
    }
    private fun capture(name:String) {
        val path=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: return
        File(path).mkdirs()
        File(path,name).outputStream().use {compose.onNodeWithTag("ai-comparison").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun entryShowsManualConnectionAndCancelPreservesPhoto() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity)
        val file=File(compose.activity.cacheDir,"ai-entry-test.png")
        val bitmap=Bitmap.createBitmap(96,128,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(85,115,155))
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        val original=runBlocking {store.import(Uri.fromFile(file))};val bytes=store.source(original).readBytes()
        try {
            compose.runOnUiThread {vm.disconnectEnhancement();vm.open(original)};ready()
            compose.onNodeWithTag("photo-enhance").assertIsDisplayed().performClick()
            compose.onNodeWithText("Conectar seu PC").assertIsDisplayed()
            compose.onNodeWithTag("ai-endpoint").assertIsDisplayed()
            compose.onNodeWithTag("ai-pair-code").assertIsDisplayed()
            compose.onNodeWithTag("ai-start").assertIsNotEnabled()
            compose.onNodeWithText("Cancelar").performClick()
            compose.onNodeWithTag("photo-enhance").assertIsDisplayed()
            assertFalse(vm.ui.value.enhancementConnected)
            assertFalse(vm.ui.value.enhancing)
            assertNull(vm.ui.value.enhancementProposal)
            assertEquals(original.id,vm.ui.value.project!!.id)
            assertArrayEquals(bytes,store.source(original).readBytes())
        } finally {
            compose.runOnUiThread {vm.closeEditor(true)};ready()
            runBlocking {store.deleteProject(original.id)};file.delete()
        }
    }
    @Test fun connectCompareDiscardAndSaveAsCopyPreserveTheOriginal() {
        val args=InstrumentationRegistry.getArguments()
        val endpoint=requireNotNull(args.getString("lumaAiEndpoint"))
        val code=requireNotNull(args.getString("lumaAiPairCode"))
        val vm=compose.activity.studio;val store=StudioStore(compose.activity)
        val file=File(compose.activity.cacheDir,"ai-test-portrait.png")
        val image=InstrumentationRegistry.getInstrumentation().context.assets.open("portrait-nasa.jpg").use {BitmapFactory.decodeStream(it)}
        val small=Bitmap.createScaledBitmap(image,240,(image.height*240f/image.width).toInt(),true)
        small.apply {file.outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()};image.recycle()
        val original=runBlocking {store.import(Uri.fromFile(file))};val originalBytes=store.source(original).readBytes()
        var createdId:String?=null
        try {
            compose.runOnUiThread {vm.open(original)};ready()
            compose.runOnUiThread {vm.update(vm.ui.value.edit.copy(adjustments=Adjustments(exposure=.12f)))};ready()
            val originalEdit=vm.ui.value.edit
            compose.onNodeWithTag("photo-enhance").assertIsDisplayed().performClick()
            compose.onNodeWithTag("ai-endpoint").performTextReplacement(endpoint)
            compose.onNodeWithTag("ai-pair-code").performTextReplacement(code)
            compose.onNodeWithTag("ai-start").performClick();ready()
            assertTrue(vm.ui.value.enhancementConnected);assertNull(vm.ui.value.error)
            val encrypted=compose.activity.getSharedPreferences("enhancement-connection",0).getString("secret",null)!!
            val connection=EnhancementConnectionStore(compose.activity).load()!!
            assertNotEquals(connection.token,encrypted)
            compose.onNodeWithTag("ai-strength").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {it(.65f)}
            compose.onNodeWithTag("ai-start").performClick();ready()
            assertNull(vm.ui.value.error)
            val first=vm.ui.value.enhancementProposal!!
            compose.onNodeWithTag("ai-compare-slider").performSemanticsAction(SemanticsActions.SetProgress) {it(.25f)}
            capture("ai-before-after.png")
            assertEquals(original.id,vm.ui.value.project!!.id);assertEquals(originalEdit,vm.ui.value.edit)
            compose.onNodeWithText("Manter como estava").performClick()
            assertNull(vm.ui.value.enhancementProposal);assertFalse(first.file.exists())
            assertArrayEquals(originalBytes,store.source(original).readBytes())
            compose.onNodeWithTag("photo-enhance").performClick()
            compose.onNodeWithTag("ai-start").performClick();ready()
            assertNull(vm.ui.value.error)
            val result=vm.ui.value.enhancementProposal!!
            compose.onNodeWithTag("ai-apply").performClick();ready()
            assertNull(vm.ui.value.error)
            val improved=vm.ui.value.project!!;createdId=improved.id
            assertNotEquals(original.id,improved.id)
            assertEquals(EditState(),improved.edit)
            assertEquals(result.width to result.height,ImageSource.dimensions(store.source(improved)))
            assertFalse(result.file.exists())
            assertEquals(originalEdit,runBlocking {store.list()}.first {it.id==original.id}.edit)
            assertArrayEquals(originalBytes,store.source(original).readBytes())
        } finally {
            compose.runOnUiThread {vm.cancelEnhancement()};ready()
            compose.runOnUiThread {vm.discardEnhancement();vm.disconnectEnhancement();vm.closeEditor(true)};ready()
            runBlocking {createdId?.let {store.deleteProject(it)};store.deleteProject(original.id)}
            file.delete()
        }
    }
}
