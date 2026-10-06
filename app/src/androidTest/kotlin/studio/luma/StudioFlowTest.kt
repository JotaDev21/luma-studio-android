package studio.luma

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import studio.luma.core.*
import studio.luma.data.*
import studio.luma.renderer.GpuRenderer
import java.io.File

@RunWith(AndroidJUnit4::class)
class StudioFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun showOnlyTestActivityAboveLockScreen() {
        // QA application only: show its own UI without dismissing the device's PIN lock.
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        compose.runOnUiThread {
            compose.activity.setShowWhenLocked(true);compose.activity.setTurnScreenOn(true)
            compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        compose.waitUntil(10000) {compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()}
    }
    private fun fixture(): File {
        val file = File(compose.activity.cacheDir,"test-original.png")
        val image = Bitmap.createBitmap(120, 160, Bitmap.Config.ARGB_8888)
        for (y in 0 until 160) for (x in 0 until 120) image.setPixel(x,y,Color.rgb(30+x,40+y,80))
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }; image.recycle()
        return file
    }
    @Test fun collageOwnTemplateFilterAndStyleAreReusableInOneEditor() {
        val vm=compose.activity.studio;compose.waitUntil(20000) {!vm.ui.value.busy}
        val context=compose.activity;val store=StudioStore(context);val file=fixture()
        val catalog=File(context.filesDir,"library/catalog.json");val before=if(catalog.exists()) catalog.readBytes() else null
        val assetDir=File(context.filesDir,"library/assets");val assetNames=assetDir.listFiles()?.map {it.name}?.toSet() ?: emptySet()
        val ids=mutableSetOf<String>()
        try {
            compose.runOnUiThread {vm.createCollage(CollageSettings(count=3,layout="polaroid",shadow=10f))}
            compose.waitUntil(20000) {vm.ui.value.preview!=null && vm.ui.value.edit.story?.collage!=null}
            ids.add(vm.ui.value.project!!.id)
            compose.runOnUiThread {vm.addStoryPhotos(List(3) {Uri.fromFile(file)})}
            compose.waitUntil(20000) {!vm.ui.value.busy && !vm.ui.value.rendering && vm.ui.value.edit.story!!.missingPhotos==0}
            compose.onNode(hasText("Colagem") and hasAnyAncestor(hasTestTag("story-tools"))).performClick()
            compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).performSemanticsAction(SemanticsActions.SetProgress) {it(40f)}
            compose.waitUntil(10000) {vm.ui.value.edit.story!!.collage!!.spacing==40f}
            compose.runOnUiThread {vm.applyStyle(vm.ui.value.styles.first())}
            compose.waitUntil(20000) {!vm.ui.value.busy && !vm.ui.value.rendering}
            compose.runOnUiThread {vm.update(vm.ui.value.edit.copy(adjustments=Adjustments(exposure=.3f,contrast=1.25f),filter=vm.ui.value.edit.filter!!.copy(intensity=.45f)))}
            compose.runOnUiThread {vm.addLayer("text","DIÁRIO VISUAL")}
            compose.onNodeWithTag("story-tools").performScrollToNode(hasText("Estilos"))
            compose.onNodeWithText("Estilos").performClick()
            compose.onNodeWithText("Salvar estilo").performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement("Meu estilo de teste")
            compose.onNodeWithText("Salvar na biblioteca").performClick()
            compose.waitUntil(20000) {!vm.ui.value.busy && vm.ui.value.library.styles.isNotEmpty()}
            val style=vm.ui.value.library.styles.last();assertEquals(1,style.layers.size);assertEquals(2,style.orderedEffects().size)
            compose.onNodeWithText("Salvar filtro").performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement("Minha cor de teste")
            compose.onNodeWithText("Salvar na biblioteca").performClick()
            compose.waitUntil(20000) {!vm.ui.value.busy && vm.ui.value.library.filters.isNotEmpty()}
            compose.onNodeWithTag("story-tools").performScrollToNode(hasText("Criar"));compose.onNodeWithTag("story-tools").performScrollToNode(hasText("Criar"));compose.onNodeWithText("Criar").performClick()
            compose.onNodeWithTag("story-create-actions").performScrollToNode(hasText("Salvar template"));compose.onNodeWithText("Salvar template").performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement("Meu template de teste")
            val firstPhoto=vm.ui.value.edit.story!!.layers.first {it.type=="photo"}
            compose.onNodeWithTag("template-photo-${firstPhoto.id}").performClick()
            compose.onNodeWithText("Salvar na biblioteca").performClick()
            compose.waitUntil(20000) {!vm.ui.value.busy && vm.ui.value.library.templates.isNotEmpty()}
            val template=vm.ui.value.library.templates.last();assertEquals(2,template.document().missingPhotos);assertNotNull(template.layers.first {it.type=="photo"}.asset)
            compose.runOnUiThread {vm.closeEditor(true)};compose.waitUntil(10000) {vm.ui.value.page==Page.HOME}
            compose.runOnUiThread {vm.createStory(template)}
            compose.waitUntil(20000) {vm.ui.value.preview!=null && !vm.ui.value.rendering}
            ids.add(vm.ui.value.project!!.id);assertEquals(2,vm.ui.value.edit.story!!.missingPhotos)
            compose.runOnUiThread {vm.addStoryPhotos(List(2) {Uri.fromFile(file)})}
            compose.waitUntil(20000) {!vm.ui.value.busy && !vm.ui.value.rendering}
            val output=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")!!.let {File(it).apply {mkdirs()}}
            File(output,"collage-editor.png").outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
            compose.runOnUiThread {vm.closeEditor(true)};compose.waitUntil(10000) {vm.ui.value.page==Page.HOME}
            val photo=runBlocking {store.import(Uri.fromFile(file))};ids.add(photo.id)
            compose.runOnUiThread {vm.open(photo)};compose.waitUntil(20000) {vm.ui.value.preview!=null}
            compose.runOnUiThread {vm.update(EditState(adjustments=style.adjustments,filter=style.filter))}
            compose.waitUntil(20000) {!vm.ui.value.rendering}
            val expectedColor=vm.ui.value.preview!!.copy(Bitmap.Config.ARGB_8888,false)
            compose.runOnUiThread {vm.update(EditState());vm.applyFilter(vm.ui.value.library.filters.last())}
            compose.waitUntil(20000) {!vm.ui.value.rendering}
            assertTrue(expectedColor.sameAs(vm.ui.value.preview));expectedColor.recycle()
            val crop=Transform(Crop(0f,0f,.5f,1f),1)
            compose.runOnUiThread {vm.update(vm.ui.value.edit.copy(transform=crop));vm.applyStyle(style)}
            compose.waitUntil(20000) {!vm.ui.value.busy && !vm.ui.value.rendering && vm.ui.value.edit.story!=null}
            assertEquals(crop,vm.ui.value.edit.story!!.layers.first {it.type=="photo"}.photoTransform)
            compose.runOnUiThread {vm.applyStyle(style)};compose.waitUntil(20000) {!vm.ui.value.busy && !vm.ui.value.rendering}
            assertEquals(1,vm.ui.value.edit.story!!.layers.count {it.styleElement})
        } finally {
            ids.forEach {store.directory(it).deleteRecursively()};file.delete()
            if(before==null) catalog.delete() else catalog.writeBytes(before)
            assetDir.listFiles()?.filter {it.name !in assetNames}?.forEach {it.delete()}
        }
    }
    @Test fun catalogPersistenceAndAllExportFormats() = runBlocking {
        val context = compose.activity; val store = StudioStore(context); val renderer = GpuRenderer()
        val savedFavorites=store.favorites()
        val file = fixture(); val imported = store.import(Uri.fromFile(file))
        val sourceBytes = store.source(imported).readBytes()
        try {
            val filters = store.catalog(); assertEquals(320, filters.size)
            filters.forEach { it.validate() }
            val updated = imported.copy(edit = EditState(transform = Transform(Crop(0f,0f,1f,.5f),1), filter = FilterSelection(filters.first(),.6f)), updatedAt = imported.updatedAt+1)
            store.save(updated)
            assertEquals(updated, store.list().first { it.id == imported.id })
            store.saveFavorites(setOf(filters.first().meta.id)); assertEquals(setOf(filters.first().meta.id), store.favorites())
            val exporter = ExportEngine(context,store,renderer)
            for (format in ExportFormat.entries) {
                val uri = exporter.export(updated,ExportOptions(format,95))
                try {
                    val decoded = android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver,uri))
                    assertEquals(80,decoded.width); assertEquals(120,decoded.height); decoded.recycle()
                } finally { context.contentResolver.delete(uri,null,null) }
            }
            assertArrayEquals(sourceBytes,store.source(imported).readBytes())
        } finally { renderer.close(); store.directory(imported.id).deleteRecursively(); file.delete(); store.saveFavorites(savedFavorites) }
    }
    @Test fun editorAppliesFilterUndoesSavesAndReopens() {
        compose.waitUntil(20000) { !compose.activity.studio.ui.value.busy }
        val store = StudioStore(compose.activity); val file = fixture()
        val project = runBlocking { store.import(Uri.fromFile(file)) }
        try {
            compose.runOnUiThread { compose.activity.studio.open(project) }
            compose.waitUntil(20000) { compose.activity.studio.ui.value.preview != null }
            compose.onNodeWithText("Amber 35").performClick()
            compose.waitUntil(10000) { compose.activity.studio.ui.value.edit.filter != null }
            compose.onNodeWithContentDescription("Desfazer").performClick()
            compose.waitUntil(10000) { compose.activity.studio.ui.value.edit.filter == null }
            compose.onNodeWithContentDescription("Refazer").performClick()
            compose.onNodeWithContentDescription("Salvar projeto").performClick()
            compose.onNodeWithText("Salvar",useUnmergedTree = true).performClick()
            compose.waitUntil(20000) { !compose.activity.studio.ui.value.busy && !compose.activity.studio.ui.value.dirty }
            val saved = runBlocking { store.list().first { it.id == project.id } }
            assertEquals("amber-35",saved.edit.filter?.preset?.meta?.id)
            compose.runOnUiThread { compose.activity.studio.closeEditor() }
            compose.waitUntil(10000) { compose.activity.studio.ui.value.page == Page.HOME }
            compose.runOnUiThread { compose.activity.studio.open(saved) }
            compose.waitUntil(20000) { compose.activity.studio.ui.value.preview != null }
            assertEquals(saved.edit,compose.activity.studio.ui.value.edit)
        } finally { store.directory(project.id).deleteRecursively(); file.delete() }
    }
    @Test fun templateStoryEditsTextAddsEffectSavesAndReopens() {
        val vm=compose.activity.studio
        compose.waitUntil(20000) {!vm.ui.value.busy}
        val store=StudioStore(compose.activity);val file=fixture()
        compose.runOnUiThread {vm.createStory(vm.ui.value.templates.first())}
        compose.waitUntil(20000) {vm.ui.value.project?.edit?.story!=null && vm.ui.value.preview!=null}
        val id=vm.ui.value.project!!.id
        try {
            val count=vm.ui.value.edit.story!!.missingPhotos
            compose.runOnUiThread {vm.addStoryPhotos(List(count) {Uri.fromFile(file)})}
            compose.waitUntil(20000) {!vm.ui.value.busy && vm.ui.value.edit.story!!.missingPhotos==0 && !vm.ui.value.rendering}
            val text=vm.ui.value.edit.story!!.layers.first {it.type=="text"}
            val document=vm.ui.value.edit.story!!;val originalTransform=text.transform
            compose.onNodeWithContentDescription("Composição do Story").performTouchInput {
                val start=Offset(width*(originalTransform.x+originalTransform.width/2)/document.width,height*(originalTransform.y+originalTransform.height/2)/document.height)
                swipe(start,start+Offset(width*.05f,height*.03f),300)
            }
            compose.waitUntil(10000) {vm.ui.value.edit.story!!.layers.first {it.id==text.id}.transform.x>originalTransform.x+20f}
            compose.onNodeWithContentDescription("Desfazer").performClick()
            compose.waitUntil(10000) {vm.ui.value.edit.story!!.layers.first {it.id==text.id}.transform==originalTransform}
            compose.runOnUiThread {vm.selectLayer(text.id)}
            compose.onNodeWithText("Editar texto").performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement("MINHAS MEMÓRIAS")
            compose.onNodeWithText("Aplicar").performClick()
            compose.waitUntil(10000) {vm.ui.value.edit.story!!.layers.any {it.content=="MINHAS MEMÓRIAS"}}
            compose.onNodeWithTag("story-tools").performScrollToNode(hasText("Efeitos"));compose.onNodeWithText("Efeitos").performClick()
            val effect=vm.ui.value.effects.first()
            compose.onNodeWithText(effect.meta.name).performClick()
            compose.waitUntil(10000) {vm.ui.value.edit.effects.size==1}
            compose.onNodeWithContentDescription("Desfazer").performClick()
            compose.waitUntil(10000) {vm.ui.value.edit.effects.isEmpty()}
            compose.onNodeWithContentDescription("Refazer").performClick()
            compose.onNodeWithTag("story-tools").performScrollToNode(hasText("Criar"));compose.onNodeWithText("Criar").performClick()
            compose.onNodeWithContentDescription("Salvar projeto").performClick()
            compose.waitUntil(20000) {!vm.ui.value.busy && !vm.ui.value.dirty}
            val saved=runBlocking {store.list().first {it.id==id}}
            assertTrue(saved.edit.story!!.layers.any {it.content=="MINHAS MEMÓRIAS"});assertEquals(1,saved.edit.effects.size)
            compose.runOnUiThread {vm.closeEditor()}
            compose.waitUntil(10000) {vm.ui.value.page==Page.HOME}
            compose.runOnUiThread {vm.open(saved)}
            compose.waitUntil(20000) {vm.ui.value.preview!=null && !vm.ui.value.rendering}
            assertEquals(saved.edit,vm.ui.value.edit)
            val image=compose.onRoot().captureToImage()
            val output=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let {File(it).apply {mkdirs()}} ?: compose.activity.getExternalFilesDir(null)!!
            File(output,"story-editor.png").outputStream().use {image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
        } finally {store.directory(id).deleteRecursively();file.delete()}
    }
}
