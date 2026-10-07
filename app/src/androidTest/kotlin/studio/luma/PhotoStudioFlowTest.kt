package studio.luma

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
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import studio.luma.core.*
import studio.luma.data.StudioStore
import studio.luma.renderer.GpuRenderer
import java.io.File

@RunWith(AndroidJUnit4::class)
class PhotoStudioFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun show() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        compose.runOnUiThread {compose.activity.setShowWhenLocked(true);compose.activity.setTurnScreenOn(true);compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
        compose.waitUntil(10000) {compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()};ready()
    }
    private fun ready()=compose.waitUntil(45000) {!compose.activity.studio.ui.value.busy && !compose.activity.studio.ui.value.rendering}
    private fun capture(name:String,browser:Boolean=false) {
        val directory=File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")!!).apply {mkdirs()}
        File(directory,name).outputStream().use {(if(browser) compose.onNodeWithTag("look-browser") else compose.onNode(isRoot())).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    private fun withPhoto(portrait:Boolean=false,action:(StudioViewModel,StudioStore,Project)->Unit) {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity);val file=File(compose.activity.cacheDir,"looks-fixture.png")
        val image=if(portrait) InstrumentationRegistry.getInstrumentation().context.assets.open("portrait-nasa.jpg").use {BitmapFactory.decodeStream(it)} else Bitmap.createBitmap(360,480,Bitmap.Config.ARGB_8888).apply {for(y in 0 until height) for(x in 0 until width) setPixel(x,y,Color.rgb(25+x*210/width,35+y*195/height,30+(x+y)*180/(width+height)))}
        image.apply {file.outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()}
        val project=runBlocking {store.import(Uri.fromFile(file))}
        var failure:Throwable?=null
        try {compose.runOnUiThread {vm.open(project)};ready();action(vm,store,project)}
        catch(e:Throwable) {failure=e;throw e}
        finally {
            val cleanup=runCatching {compose.runOnUiThread {vm.closeEditor(true)};ready()}.exceptionOrNull()
            runBlocking {store.deleteProject(project.id)};file.delete()
            if(cleanup!=null) {if(failure!=null) failure.addSuppressed(cleanup) else throw cleanup}
        }
    }
    @Test fun gallerySearchFavoritesIntensityAndUndoStayClear()=withPhoto(true) {vm,store,_ ->
        val favorites=runBlocking {store.favorites()}
        try {
            compose.onNodeWithTag("all-filters").performClick()
            compose.onNodeWithTag("look-search").performTextReplacement("filme")
            compose.onNode(hasTestTag("look-amber-35") and hasAnyAncestor(hasTestTag("look-grid"))).assertIsDisplayed()
            compose.onNodeWithTag("look-search").performTextReplacement("petala")
            compose.onNode(hasTestTag("look-studio-soft-03") and hasAnyAncestor(hasTestTag("look-grid"))).assertIsDisplayed()
            compose.onNodeWithTag("look-search").performTextReplacement("neon rosa")
            val card=hasTestTag("look-studio-neon-01") and hasAnyAncestor(hasTestTag("look-grid"))
            compose.onNode(card).performClick();ready();assertEquals("studio-neon-01",vm.ui.value.edit.filter!!.preset.meta.id)
            compose.onNode(hasContentDescription("Favoritar Neon Rosa") and hasAnyAncestor(hasTestTag("look-grid"))).performClick()
            compose.onNodeWithTag("look-search").performTextClearance()
            compose.onNode(hasText("Favoritos") and hasAnyAncestor(hasTestTag("look-collections"))).performClick()
            compose.onNode(card).assertIsDisplayed();compose.onNodeWithTag("look-search").assertIsNotFocused()
            compose.onNode(hasTestTag("look-intensity") and hasAnyAncestor(hasTestTag("look-browser"))).performSemanticsAction(SemanticsActions.SetProgress) {it(.42f)};ready()
            capture("look-gallery.png",true)
            compose.onNodeWithText("Usar este filtro").performClick();ready()
            assertEquals(.42f,vm.ui.value.edit.filter!!.intensity,.01f)
            capture("photo-editor.png")
            compose.onNode(hasText("Luz e cor") and hasAnyAncestor(hasTestTag("photo-tools"))).performClick()
            compose.onNodeWithTag("photo-adjustment").performSemanticsAction(SemanticsActions.SetProgress) {it(.35f)};ready()
            // Leaving a slider commits its gesture, so undo reverses the look alone.
            compose.onNode(hasText("Filtros") and hasAnyAncestor(hasTestTag("photo-tools"))).performClick()
            compose.onNodeWithContentDescription("Remover filtro").performClick();ready()
            assertNull(vm.ui.value.edit.filter);assertEquals(.35f,vm.ui.value.edit.adjustments.exposure,.01f)
            compose.onNodeWithContentDescription("Desfazer").performClick();ready()
            assertEquals(.42f,vm.ui.value.edit.filter!!.intensity,.01f);assertEquals(.35f,vm.ui.value.edit.adjustments.exposure,.01f)
            compose.onNodeWithContentDescription("Desfazer").performClick();ready();assertEquals(0f,vm.ui.value.edit.adjustments.exposure,.01f)
            assertNull(vm.ui.value.error)
        } finally {
            (vm.ui.value.favorites-favorites).forEach {id->compose.runOnUiThread {vm.toggleFavorite(id)}}
            (favorites-vm.ui.value.favorites).forEach {id->compose.runOnUiThread {vm.toggleFavorite(id)}}
        }
    }
    private fun thumbnail(vm:StudioViewModel,preset:FilterPreset):Bitmap {
        val workers=CoroutineScope(Dispatchers.Default+SupervisorJob())
        try {
            val pending=workers.async {vm.thumbnail(preset)}
            // Waiting through the Compose rule also advances UI thumbnail continuations.
            compose.waitUntil(20000) {pending.isCompleted}
            return runBlocking {pending.await()}!!
        } finally {workers.cancel()}
    }
    @Test fun thumbnailsRespectCropManualColorSavedIntensityAndInvalidation()=withPhoto {vm,store,project ->
        val preset=vm.ui.value.filters.first {it.meta.id=="studio-editorial-01"}.copy(savedIntensity=.37f)
        val base=EditState(transform=Transform(Crop(.1f,.12f,.9f,.94f),1),adjustments=Adjustments(exposure=.42f),color=ColorTreatment(balance=listOf(.08f,-.03f,.04f)))
        compose.runOnUiThread {vm.update(base)};ready()
        val thumb=thumbnail(vm,preset)
        val source=vm.ui.value.source!!;val scale=224f/maxOf(source.width,source.height)
        val small=Bitmap.createScaledBitmap(source,(source.width*scale).toInt(),(source.height*scale).toInt(),true)
        val renderer=GpuRenderer()
        try {
            val expected=runBlocking {renderer.render(small,base.copy(filter=FilterSelection(preset,.37f)),maxEdge=224,directory=store.directory(project.id))}
            assertTrue("Cards must show the actual prospective edit",expected.sameAs(thumb));expected.recycle()
            val revision=vm.ui.value.thumbnailRevision
            compose.runOnUiThread {vm.applyFilter(preset);vm.update(vm.ui.value.edit.copy(filter=FilterSelection(preset,.12f)))};ready()
            assertEquals(revision,vm.ui.value.thumbnailRevision);assertSame(thumb,vm.ui.value.thumbnails[preset.meta.id])
            compose.runOnUiThread {vm.update(vm.ui.value.edit.copy(adjustments=Adjustments(exposure=-.6f)))};ready()
            assertTrue(vm.ui.value.thumbnailRevision>revision)
            val changed=thumbnail(vm,preset);assertFalse(changed.sameAs(thumb))
            val saved=preset.copy(meta=preset.meta.copy(id="saved-color-test"),savedAdjustments=Adjustments(exposure=.7f),savedColor=ColorTreatment(balance=listOf(-.1f,.05f,.08f)))
            val savedThumb=thumbnail(vm,saved)
            val savedExpected=runBlocking {renderer.render(small,base.copy(adjustments=saved.savedAdjustments!!,color=saved.savedColor!!,filter=FilterSelection(saved,.37f)),maxEdge=224,directory=store.directory(project.id))}
            assertTrue(savedExpected.sameAs(savedThumb));savedExpected.recycle()
        } finally {small.recycle();runBlocking {renderer.close()}}
    }
    @Test fun rapidSelectionsWhileCardsLoadFinishOnLatestLookAndNewLooksRenderDistinctly()=withPhoto {vm,store,project ->
        val looks=vm.ui.value.filters.filter {it.meta.id.startsWith("studio-")}
        val source=vm.ui.value.source!!;val renderer=GpuRenderer()
        try {
            // Compose owns its Main test scheduler. Await worker jobs without blocking
            // that scheduler: UI selections use the rule's UI-thread action.
            val workers=CoroutineScope(Dispatchers.Default+SupervisorJob())
            try {
                val cards=looks.take(18).map {preset->workers.async {vm.thumbnail(preset)}}
                compose.runOnUiThread {looks.take(30).forEach {vm.applyFilter(it)};vm.update(vm.ui.value.edit.copy(transform=Transform(quarterTurns=1)));vm.applyFilter(looks.last())}
                compose.waitUntil(20000) {cards.all {it.isCompleted}}
                runBlocking {cards.awaitAll()}
            } finally {workers.cancel()}
            ready()
            assertEquals(looks.last().meta.id,vm.ui.value.edit.filter!!.preset.meta.id)
            val expected=runBlocking {renderer.render(source,vm.ui.value.edit,directory=store.directory(project.id))}
            assertTrue("A late card or old render must not replace the current photo",expected.sameAs(vm.ui.value.preview));expected.recycle()
            assertNull(vm.ui.value.error)
            val small=Bitmap.createScaledBitmap(source,64,80,true)
            try {
                val signatures=runBlocking {looks.map {preset->val out=renderer.render(small,EditState(filter=FilterSelection(preset)),maxEdge=80);val pixels=IntArray(out.width*out.height);out.getPixels(pixels,0,out.width,0,0,out.width,out.height);out.recycle();pixels.contentHashCode()}.toSet()}
                assertEquals("New looks must change rendered pixels, not only labels",120,signatures.size)
            } finally {small.recycle()}
        } finally {runBlocking {renderer.close()}}
    }
}
