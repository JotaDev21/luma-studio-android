package studio.luma

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
import studio.luma.renderer.*
import java.io.File

@RunWith(AndroidJUnit4::class)
class SegmentationFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun showQaActivity() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        compose.runOnUiThread {compose.activity.setShowWhenLocked(true);compose.activity.setTurnScreenOn(true);compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
        compose.waitUntil(10000) {compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()}
        ready()
        assertEquals(PackageManager.PERMISSION_DENIED,compose.activity.packageManager.checkPermission("android.permission.INTERNET",compose.activity.packageName))
    }
    private fun ready()=compose.waitUntil(30000) {!compose.activity.studio.ui.value.busy && !compose.activity.studio.ui.value.rendering}
    private fun openMasks() {
        compose.onNodeWithTag("photo-tools").performScrollToNode(hasText("Máscaras"));compose.onNodeWithText("Máscaras").performClick()
        try {compose.waitUntil(20000) {compose.activity.studio.ui.value.maskPreview!=null && !compose.activity.studio.ui.value.rendering}}
        catch(e:androidx.compose.ui.test.ComposeTimeoutException) {
            val state=compose.activity.studio.ui.value
            throw AssertionError("Masks failed to open: page=${state.page}, editor=${state.maskEditor}, busy=${state.busy}, rendering=${state.rendering}, error=${state.error}",e)
        }
    }
    @Test fun portraitInferenceSelectionCorrectionCutoutReopenAndExportWorkWithoutNetworkPermission() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity)
        val file=File(compose.activity.cacheDir,"segmentation-portrait.jpg")
        InstrumentationRegistry.getInstrumentation().context.assets.open("portrait-nasa.jpg").use {input->file.outputStream().use {input.copyTo(it)}}
        val project=runBlocking {store.import(Uri.fromFile(file))};val originalBytes=store.source(project).readBytes()
        try {
            compose.runOnUiThread {vm.open(project)};ready();openMasks()
            compose.onNodeWithText("Pessoa / fundo").performClick();compose.onNodeWithText("Selecionar pessoa").performClick();ready()
            assertNull(vm.ui.value.error)
            val person=vm.maskPhotoEdit().masks.single();assertEquals("person",person.type);assertNotNull(person.raster)
            val raster=person.raster!!;val alpha=raster.decode()
            fun coverage(x:Float,y:Float)=alpha[(y*raster.height).toInt()*raster.width+(x*raster.width).toInt()].toInt() and 255
            assertTrue("Portrait face must be foreground",coverage(.55f,.2f)>200)
            assertTrue("Flag corner must be background",coverage(.05f,.05f)<80)
            compose.onNodeWithText("Pessoa / fundo").performClick();compose.onNodeWithText("Selecionar fundo").performClick();ready()
            val background=vm.maskPhotoEdit().masks.last();assertTrue(background.inverted);assertEquals(raster,background.raster)
            compose.runOnUiThread {vm.brushStroke(background.id,BrushStroke(listOf(Point(.1f,.1f))))};ready()
            assertTrue(vm.maskPhotoEdit().masks.last().strokes.single().erase)
            compose.onNodeWithContentDescription("Desfazer").performClick();ready();assertTrue(vm.maskPhotoEdit().masks.last().strokes.isEmpty())
            compose.runOnUiThread {vm.deleteMask(background.id);vm.deleteMask(person.id)};ready()
            compose.onNodeWithText("Pessoa / fundo").performClick();compose.onNodeWithText("Remover fundo").performClick();ready()
            val cutout=vm.maskPhotoEdit().masks.single();assertTrue(cutout.cutout)
            val output=vm.ui.value.maskPreview!!
            assertEquals(0,Color.alpha(output.getPixel(20,20)));assertTrue(Color.alpha(output.getPixel((output.width*.55f).toInt(),(output.height*.2f).toInt()))>240)
            compose.onNodeWithContentDescription("Comparar original").performClick()
            compose.onNodeWithTag("mask-photo").performTouchInput {click(androidx.compose.ui.geometry.Offset(width*.05f,height*.05f))};ready()
            assertEquals(1,vm.maskPhotoEdit().masks.single().strokes.size)
            assertTrue(Color.alpha(vm.ui.value.maskPreview!!.getPixel(24,30))>200)
            compose.onNodeWithContentDescription("Desfazer").performClick();ready()
            compose.onNodeWithContentDescription("Comparar original").performClick()
            compose.onNodeWithText("Ocultar área").performClick()
            val capture=File(requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir"))).apply {mkdirs()}
            File(capture,"person-cutout-editor.png").outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
            compose.onNodeWithText("Concluir máscaras").performClick();ready()
            compose.runOnUiThread {vm.save()};ready()
            val saved=runBlocking {store.list().first {it.id==project.id}}
            assertEquals(8,saved.schemaVersion);assertEquals(vm.ui.value.edit,saved.edit)
            compose.runOnUiThread {vm.closeEditor(true)};compose.waitUntil(10000) {vm.ui.value.page==Page.HOME}
            compose.runOnUiThread {vm.open(saved)};ready();assertEquals(saved.edit,vm.ui.value.edit)
            runBlocking {
                val gpu=GpuRenderer()
                try {
                    for(format in ExportFormat.entries) {
                        val uri=ExportEngine(compose.activity,store,gpu).export(saved,ExportOptions(format,100))
                        try {
                            val decoded=compose.activity.contentResolver.openInputStream(uri)!!.use {BitmapFactory.decodeStream(it)}
                            if(format==ExportFormat.JPEG) {assertTrue(Color.red(decoded.getPixel(20,20))>250);assertEquals(255,Color.alpha(decoded.getPixel(20,20)))}
                            else {assertEquals(0,Color.alpha(decoded.getPixel(20,20)));assertTrue(Color.alpha(decoded.getPixel(264,120))>240)}
                            if(format==ExportFormat.PNG) assertTrue(decoded.sameAs(vm.ui.value.preview))
                            decoded.recycle()
                        } finally {compose.activity.contentResolver.delete(uri,null,null)}
                    }
                } finally {gpu.close()}
            }
            assertArrayEquals(originalBytes,store.source(project).readBytes())
        } finally {store.directory(project.id).deleteRecursively();file.delete()}
    }
    @Test fun missingPersonReportsErrorWithoutAddingAnEmptyMask() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity);val file=File(compose.activity.cacheDir,"no-person.png")
        val bitmap=Bitmap.createBitmap(480,600,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.BLACK)}
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        val project=runBlocking {store.import(Uri.fromFile(file))}
        try {
            compose.runOnUiThread {vm.open(project)};ready();openMasks()
            compose.onNodeWithText("Pessoa / fundo").performClick();compose.onNodeWithText("Selecionar pessoa").performClick();ready()
            assertTrue(vm.maskPhotoEdit().masks.isEmpty());assertNotNull(vm.ui.value.error)
            assertTrue(vm.ui.value.error!!.contains("Não encontrei uma pessoa"));assertFalse(vm.ui.value.busy)
            compose.onNodeWithText("Entendi").performClick();assertNull(vm.ui.value.error)
        } finally {store.directory(project.id).deleteRecursively();file.delete()}
    }
}
