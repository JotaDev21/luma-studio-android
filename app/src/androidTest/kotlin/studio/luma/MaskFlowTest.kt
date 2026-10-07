package studio.luma

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.geometry.Offset
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
import studio.luma.renderer.*
import java.io.File

@RunWith(AndroidJUnit4::class)
class MaskFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun showQaActivity() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        compose.runOnUiThread {compose.activity.setShowWhenLocked(true);compose.activity.setTurnScreenOn(true);compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
        compose.waitUntil(10000) {compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()}
        compose.waitUntil(20000) {!compose.activity.studio.ui.value.busy}
    }
    private fun fixture():File {
        val file=File(compose.activity.cacheDir,"mask-test-original.png")
        val bitmap=Bitmap.createBitmap(120,160,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.rgb(80,100,120))}
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle();return file
    }
    private fun ready() {
        compose.waitUntil(20000) {!compose.activity.studio.ui.value.busy && !compose.activity.studio.ui.value.rendering}
        // These regression cases exercise the complete editor; the guide has separate tests.
        if(compose.onAllNodesWithTag("story-guide-full").fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()) compose.onNodeWithTag("story-guide-full").performScrollTo().performClick()
    }
    @Test fun brushUiUndoSaveReopenAndExportPreserveOriginal() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity);val file=fixture()
        val project=runBlocking {store.import(Uri.fromFile(file))};val bytes=store.source(project).readBytes()
        try {
            compose.runOnUiThread {vm.open(project)};ready()
            compose.runOnUiThread {vm.update(vm.ui.value.edit.copy(transform=Transform(quarterTurns=1)))};ready()
            compose.onNodeWithContentDescription("Mais ferramentas").performClick();compose.onNodeWithText("Máscaras e retoques").performClick()
            compose.waitUntil(20000) {vm.ui.value.maskPreview!=null && !vm.ui.value.rendering}
            compose.onNodeWithText("+ Pincel").performClick();ready()
            compose.onNodeWithTag("mask-photo").performTouchInput {down(Offset(width*.25f,height*.5f));moveTo(Offset(width*.75f,height*.5f),500);up()}
            ready();assertEquals(1,vm.maskPhotoEdit().masks.single().strokes.size)
            compose.onNodeWithContentDescription("Desfazer").performClick();ready();assertTrue(vm.maskPhotoEdit().masks.single().strokes.isEmpty())
            compose.onNodeWithContentDescription("Refazer").performClick();ready();assertEquals(1,vm.maskPhotoEdit().masks.single().strokes.size)
            compose.onNodeWithText("Ajuste",substring=false).performClick()
            compose.onNodeWithTag("local-adjustment").performSemanticsAction(SemanticsActions.SetProgress) {it(1f)}
            compose.runOnUiThread {vm.finishGesture()};ready()
            val output=vm.ui.value.maskPreview!!
            assertTrue(Color.red(output.getPixel(output.width/2,output.height/2))>100);assertEquals(80,Color.red(output.getPixel(10,10)))
            val screenshotDir=File(requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir"))).apply {mkdirs()}
            File(screenshotDir,"mask-editor.png").outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
            compose.onNodeWithText("Concluir máscaras").performClick();ready()
            val edit=vm.ui.value.edit
            compose.runOnUiThread {vm.save()};ready()
            val saved=runBlocking {store.list().first {it.id==project.id}}
            assertEquals(8,saved.schemaVersion);assertEquals(edit,saved.edit)
            compose.runOnUiThread {vm.closeEditor(true)};compose.waitUntil(10000) {vm.ui.value.page==Page.HOME}
            compose.runOnUiThread {vm.open(saved)};ready();assertEquals(edit,vm.ui.value.edit)
            runBlocking {
                val gpu=GpuRenderer()
                try {
                    val uri=ExportEngine(compose.activity,store,gpu).export(saved,ExportOptions(ExportFormat.PNG,100))
                    try {
                        val exported=compose.activity.contentResolver.openInputStream(uri)!!.use {BitmapFactory.decodeStream(it)}
                        assertTrue(exported.sameAs(vm.ui.value.preview));exported.recycle()
                    } finally {compose.activity.contentResolver.delete(uri,null,null)}
                } finally {gpu.close()}
            }
            assertArrayEquals(bytes,store.source(project).readBytes())
        } finally {store.directory(project.id).deleteRecursively();file.delete()}
    }
    @Test fun storyMasksArePerPhotoAndPhotoConversionKeepsRetouch() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity);val file=fixture()
        val project=runBlocking {store.import(Uri.fromFile(file))}
        try {
            val mask=LocalMask("area","Retoque",type="radial",radiusX=.3f,radiusY=.3f,adjustments=Adjustments(exposure=1f))
            compose.runOnUiThread {vm.open(project)};ready()
            compose.runOnUiThread {vm.update(vm.ui.value.edit.copy(masks=listOf(mask)));vm.convertPhotoToStory()};ready()
            val layer=vm.ui.value.edit.story!!.layers.single()
            assertEquals(listOf(mask),layer.masks);assertTrue(vm.ui.value.edit.masks.isEmpty())
            compose.onNodeWithText("Máscaras da foto").performClick()
            compose.waitUntil(20000) {vm.ui.value.maskPreview!=null && !vm.ui.value.rendering}
            assertEquals(listOf(mask),vm.maskPhotoEdit().masks)
            compose.onNodeWithText("+ Circular").performClick();ready()
            compose.onNodeWithText("Concluir máscaras").performClick();ready()
            assertFalse(vm.ui.value.maskEditor);assertEquals(2,vm.ui.value.edit.story!!.layers.single().masks.size)
            assertEquals(vm.ui.value.edit.story!!.width.toFloat()/vm.ui.value.edit.story!!.height,vm.ui.value.preview!!.width.toFloat()/vm.ui.value.preview!!.height,.01f)
            compose.runOnUiThread {vm.duplicateLayer(layer.id)};ready()
            val second=vm.ui.value.selectedLayer!!
            compose.runOnUiThread {vm.changeLayer(second,{it.copy(masks=emptyList())})};ready()
            assertEquals(2,vm.ui.value.edit.story!!.layers.first().masks.size);assertTrue(vm.ui.value.edit.story!!.layers.last().masks.isEmpty())
            compose.runOnUiThread {vm.save()};ready()
            val saved=runBlocking {store.list().first {it.id==project.id}}
            assertEquals(vm.ui.value.edit,saved.edit)
            compose.runOnUiThread {vm.addStoryPhotos(listOf(Uri.fromFile(file)),layer.id)};ready()
            assertTrue(vm.ui.value.edit.story!!.layers.first {it.id==layer.id}.masks.isEmpty())
            compose.runOnUiThread {vm.openMasks(layer.id)};compose.waitUntil(20000) {vm.ui.value.maskPreview!=null && !vm.ui.value.rendering}
            compose.onNodeWithContentDescription("Desfazer").performClick();ready()
            assertFalse(vm.ui.value.maskEditor)
            assertEquals(2,vm.ui.value.edit.story!!.layers.first {it.id==layer.id}.masks.size)
        } finally {store.directory(project.id).deleteRecursively();file.delete()}
    }
}
