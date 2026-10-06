package studio.luma

import android.content.pm.PackageManager
import android.graphics.*
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
import studio.luma.core.*
import studio.luma.data.*
import studio.luma.renderer.*
import java.io.File

@RunWith(AndroidJUnit4::class)
class SmartFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun showQaActivity() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        compose.runOnUiThread {compose.activity.setShowWhenLocked(true);compose.activity.setTurnScreenOn(true);compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
        compose.waitUntil(10000) {compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()};ready()
        assertEquals(PackageManager.PERMISSION_DENIED,compose.activity.packageManager.checkPermission("android.permission.INTERNET",compose.activity.packageName))
    }
    private fun ready()=compose.waitUntil(45000) {!compose.activity.studio.ui.value.busy && !compose.activity.studio.ui.value.rendering}
    private fun portrait():Bitmap=InstrumentationRegistry.getInstrumentation().context.assets.open("portrait-nasa.jpg").use {BitmapFactory.decodeStream(it)}
    private fun smartTool() {
        compose.onNodeWithTag("story-tools").performScrollToNode(hasText("Inteligente"));compose.onNodeWithText("Inteligente").performClick()
    }
    private fun capture(name:String) {
        val dir=File(requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir"))).apply {mkdirs()}
        File(dir,name).outputStream().use {compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun immediateSuggestionUsesLatestBeforeAndCancelPreservesPendingGestureHistory() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity);val file=File(compose.activity.cacheDir,"immediate-framing.png")
        Bitmap.createBitmap(480,600,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.BLUE);file.outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()}
        val project=runBlocking {store.import(Uri.fromFile(file))}
        try {
            compose.runOnUiThread {vm.open(project)};ready()
            val base=vm.ui.value.edit.copy(adjustments=Adjustments(brightness=.25f))
            compose.runOnUiThread {vm.update(base,false);vm.proposeSmartCrop(1f)};ready()
            assertNull(vm.ui.value.error);val before=vm.ui.value.smartProposal!!.before
            assertEquals(480,before.width);assertEquals(600,before.height);assertTrue(Color.red(before.getPixel(100,100))>10)
            assertEquals(base,vm.ui.value.edit);assertFalse(vm.ui.value.canUndo)
            compose.onNodeWithText("Cancelar").performClick();ready()
            assertEquals(base,vm.ui.value.edit);assertFalse(vm.ui.value.canUndo);assertFalse(vm.ui.value.canRedo)
            assertTrue(Color.red(vm.ui.value.preview!!.getPixel(100,100))>10)
            compose.runOnUiThread {vm.proposeSmartCrop(1f)};ready();compose.onNodeWithText("Aplicar sugestão").performClick();ready()
            compose.onNodeWithContentDescription("Desfazer").performClick();ready();assertEquals(base,vm.ui.value.edit)
        } finally {store.directory(project.id).deleteRecursively();file.delete()}
    }
    @Test fun realFaceCropPreviewCancelApplySingleUndoSaveReopenAndPngExport() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity);val file=File(compose.activity.cacheDir,"smart-portrait.png")
        val source=portrait();val focus=runBlocking {FocusAnalyzer().analyze(source)}
        assertTrue("Bundled detector must find a real face offline",focus.faces.isNotEmpty())
        file.outputStream().use {source.compress(Bitmap.CompressFormat.PNG,100,it)};source.recycle()
        val project=runBlocking {store.import(Uri.fromFile(file))};val bytes=store.source(project).readBytes()
        try {
            compose.runOnUiThread {vm.open(project)};ready()
            val mask=LocalMask("retouch","Retoque",type="radial",adjustments=Adjustments(exposure=.2f))
            compose.runOnUiThread {vm.update(vm.ui.value.edit.copy(masks=listOf(mask)))};ready()
            val base=vm.ui.value.edit
            compose.onNodeWithTag("photo-tools").performScrollToNode(hasText("Recorte"));compose.onNodeWithText("Recorte").performClick()
            compose.onNodeWithText("Recorte inteligente").performClick();compose.onNodeWithText("Paisagem · 1.91:1").performClick();ready()
            assertNull(vm.ui.value.error);val suggestion=vm.ui.value.smartProposal!!
            assertEquals(base,vm.ui.value.edit);assertEquals(listOf(mask),suggestion.edit.masks)
            val crop=suggestion.edit.transform.crop
            focus.faces.forEach {f->assertTrue(crop.left<=f.left && crop.right>=f.right && crop.top<=f.top && crop.bottom>=f.bottom)}
            assertTrue("The detected head should move this crop above center",crop.top<.15f)
            compose.onNodeWithText("Ver antes").performClick();compose.onNodeWithText("Ver sugestão").performClick()
            capture("smart-crop-proposal.png")
            compose.onNodeWithText("Cancelar").performClick();assertEquals(base,vm.ui.value.edit);assertNull(vm.ui.value.smartProposal)
            compose.onNodeWithText("Recorte inteligente").performClick();compose.onNodeWithText("Paisagem · 1.91:1").performClick();ready()
            compose.onNodeWithText("Aplicar sugestão").performClick();ready();val accepted=vm.ui.value.edit
            assertEquals(suggestion.edit,accepted)
            compose.onNodeWithContentDescription("Desfazer").performClick();ready();assertEquals(base,vm.ui.value.edit)
            compose.onNodeWithContentDescription("Refazer").performClick();ready();assertEquals(accepted,vm.ui.value.edit)
            compose.runOnUiThread {vm.save()};ready();val saved=runBlocking {store.list().first {it.id==project.id}}
            assertEquals(8,saved.schemaVersion);assertEquals(accepted,saved.edit)
            compose.runOnUiThread {vm.closeEditor(true)};ready();compose.runOnUiThread {vm.open(saved)};ready();assertEquals(accepted,vm.ui.value.edit)
            runBlocking {
                val gpu=GpuRenderer()
                try {
                    val uri=ExportEngine(compose.activity,store,gpu).export(saved,ExportOptions(ExportFormat.PNG,100))
                    try {val image=compose.activity.contentResolver.openInputStream(uri)!!.use {BitmapFactory.decodeStream(it)}
                        assertTrue(image.sameAs(vm.ui.value.preview));image.recycle()
                    } finally {compose.activity.contentResolver.delete(uri,null,null)}
                } finally {gpu.close()}
            }
            assertArrayEquals(bytes,store.source(project).readBytes())
        } finally {store.directory(project.id).deleteRecursively();file.delete()}
    }
    @Test fun twoRealFacesFallBackToWholePhotoInsideStoryAndPersist() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity);val file=File(compose.activity.cacheDir,"two-faces.png")
        val one=portrait();val image=Bitmap.createBitmap(one.width*2,one.height,Bitmap.Config.ARGB_8888)
        Canvas(image).apply {drawBitmap(one,0f,0f,null);drawBitmap(one,one.width.toFloat(),0f,null)};one.recycle()
        val focus=runBlocking {FocusAnalyzer().analyze(image)};assertTrue("Both real faces must be detected",focus.faces.size>=2)
        file.outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
        val layer=StoryLayer("photo","photo",LayerTransform(20f,20f,180f,320f),placeholder=true)
        val project=runBlocking {store.createStory(StoryTemplate(meta=AssetMeta("two_faces","Two faces","Test"),width=220,height=360,background=Color.WHITE,layers=listOf(layer)))}
        try {
            compose.runOnUiThread {vm.open(project)};ready();compose.runOnUiThread {vm.addStoryPhotos(listOf(Uri.fromFile(file)))};ready()
            val base=vm.ui.value.edit;smartTool()
            compose.onNodeWithText("Enquadrar foto selecionada").performClick();ready();assertNull(vm.ui.value.error)
            val proposed=vm.ui.value.smartProposal!!.edit.story!!.layers.single()
            assertEquals("contain",proposed.photoFit);assertEquals(Crop(),proposed.photoTransform.crop);assertEquals(base,vm.ui.value.edit)
            capture("smart-whole-photo-proposal.png")
            compose.onNodeWithText("Aplicar sugestão").performClick();ready()
            val preview=vm.ui.value.preview!!;assertEquals(Color.WHITE,preview.getPixel(100,50))
            focus.faces.forEach {f->val x=(20+(f.left+f.right)/2*180).toInt();val y=(20+(320-112.5)/2+(f.top+f.bottom)/2*112.5).toInt()
                assertNotEquals(Color.WHITE,preview.getPixel(x,y))}
            compose.runOnUiThread {vm.save()};ready();val saved=runBlocking {store.list().first {it.id==project.id}}
            assertEquals("contain",saved.edit.story!!.layers.single().photoFit)
            runBlocking {
                val gpu=GpuRenderer();val renderer=StoryRenderer(gpu)
                try {val output=renderer.render(saved.edit.story!!,store.directory(saved.id),saved.edit,maxEdge=0,export=true)
                    assertTrue(output.sameAs(preview));output.recycle()
                } finally {renderer.close();gpu.close()}
            }
        } finally {store.directory(project.id).deleteRecursively();file.delete()}
    }
    @Test fun orientationAssignmentKeepsMasksWithAssetsAndGraphicLayersAndCanBeUndone() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity)
        val files=listOf(File(compose.activity.cacheDir,"wide.png"),File(compose.activity.cacheDir,"tall.png"))
        listOf(200 to 100,100 to 200).forEachIndexed {i,(w,h)->Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply {
            eraseColor(if(i==0) Color.RED else Color.BLUE);files[i].outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()}}
        val layers=listOf(StoryLayer("portrait","photo",LayerTransform(10f,10f,100f,200f),placeholder=true),StoryLayer("landscape","photo",LayerTransform(10f,230f,200f,100f),placeholder=true),StoryLayer("text","text",LayerTransform(150f,100f,100f,100f),content="Meu texto",locked=true,typography=Typography(size=18f)))
        val project=runBlocking {store.createStory(StoryTemplate(meta=AssetMeta("assignment","Assignment","Test"),width=320,height=360,layers=layers))}
        try {
            compose.runOnUiThread {vm.open(project)};ready();compose.runOnUiThread {vm.addStoryPhotos(files.map(Uri::fromFile))};ready()
            val mask=LocalMask("photo_mask","Retoque",type="radial",adjustments=Adjustments(exposure=.1f))
            compose.runOnUiThread {vm.changeLayer("portrait",{it.copy(masks=listOf(mask))})};ready()
            val base=vm.ui.value.edit;val photos=base.story!!.layers.take(2);val originalBytes=photos.associate {it.asset!! to File(store.directory(project.id),it.asset!!).readBytes()}
            smartTool();compose.onNodeWithText("Organizar fotos").performClick();ready();assertNull(vm.ui.value.error)
            assertEquals(base,vm.ui.value.edit);val candidate=vm.ui.value.smartProposal!!.edit.story!!
            assertEquals(photos.reversed().map {it.asset},candidate.layers.take(2).map {it.asset})
            assertEquals(listOf(mask),candidate.layers[1].masks);assertTrue(candidate.layers[0].masks.isEmpty());assertEquals(layers.last(),candidate.layers.last())
            capture("smart-layout-proposal.png")
            compose.onNodeWithText("Aplicar sugestão").performClick();ready();val accepted=vm.ui.value.edit
            compose.onNodeWithContentDescription("Desfazer").performClick();ready();assertEquals(base,vm.ui.value.edit)
            compose.onNodeWithContentDescription("Refazer").performClick();ready();assertEquals(accepted,vm.ui.value.edit)
            compose.runOnUiThread {vm.save()};ready();assertEquals(accepted,runBlocking {store.list().first {it.id==project.id}}.edit)
            originalBytes.forEach {(name,bytes)->assertArrayEquals(bytes,File(store.directory(project.id),name).readBytes())}
        } finally {store.directory(project.id).deleteRecursively();files.forEach {it.delete()}}
    }
}
