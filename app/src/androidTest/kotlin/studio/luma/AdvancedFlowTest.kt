package studio.luma
import android.graphics.Bitmap
import android.graphics.Color
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
import java.io.File
@RunWith(AndroidJUnit4::class)
class AdvancedFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun show() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        compose.runOnUiThread {compose.activity.setShowWhenLocked(true);compose.activity.setTurnScreenOn(true);compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
        compose.waitUntil(10000) {compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()};ready()
    }
    private fun ready()=compose.waitUntil(45000) {!compose.activity.studio.ui.value.busy && !compose.activity.studio.ui.value.rendering}
    private fun capture(name:String,dialog:Boolean=true) {
        val directory=File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")!!).apply {mkdirs()}
        File(directory,name).outputStream().use {compose.onNode(if(dialog) isDialog() else isRoot()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun proControlsReferenceAndSavedUndoWorkTogether() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity);val file=File(compose.activity.cacheDir,"advanced-flow.png")
        Bitmap.createBitmap(300,400,Bitmap.Config.ARGB_8888).apply {for(y in 0 until height) for(x in 0 until width) setPixel(x,y,Color.rgb(30+x*180/width,40+y*180/height,100));file.outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()}
        val project=runBlocking {store.import(Uri.fromFile(file))}
        try {
            compose.runOnUiThread {vm.open(project)};ready()
            compose.onNodeWithText("Modo rápido").performClick();compose.onNodeWithTag("photo-tools").performScrollToNode(hasText("Avançado"));compose.onNodeWithText("Avançado").performClick();compose.onNodeWithText("Cor Pro").performClick()
            compose.onNodeWithText("HSL").performClick();compose.onNodeWithTag("Saturação").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) {it(-.4f)};ready()
            capture("pro-hsl.png");compose.onNodeWithText("Pronto").performClick()
            val manual=vm.ui.value.edit;assertEquals(-.4f,manual.color.hsl[0].saturation,.02f)
            compose.runOnUiThread {vm.reference(Uri.fromFile(file))};ready();assertNotNull(vm.ui.value.smartProposal);capture("reference-proposal.png")
            compose.onNodeWithText("Ver referência").performClick();compose.onNodeWithText("Cancelar").performClick();ready();assertEquals(manual,vm.ui.value.edit)
            compose.runOnUiThread {vm.save("Advanced QA")};ready();val saved=runBlocking {store.list().first {it.id==project.id}};assertNotNull(saved.history);assertEquals(8,saved.schemaVersion)
            compose.runOnUiThread {vm.closeEditor()};ready();compose.runOnUiThread {vm.open(saved)};ready();assertTrue(vm.ui.value.canUndo);compose.runOnUiThread {vm.undo()};ready();assertEquals(ColorTreatment(),vm.ui.value.edit.color)
        } finally {compose.runOnUiThread {vm.closeEditor(true)};ready();runBlocking {store.deleteProject(project.id)};file.delete()}
    }
    @Test fun musicAndDrawingAreEditableWithGradientBackground() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity)
        compose.runOnUiThread {vm.createStory()};ready();val project=vm.ui.value.project!!
        try {
            compose.runOnUiThread {vm.music("Minha música","Meu artista","01:20 / 03:00",.4f)};ready();assertEquals(1,vm.ui.value.edit.story!!.missingPhotos)
            compose.runOnUiThread {vm.changeStory {doc->doc.copy(layers=doc.layers.filterNot {it.type=="photo"},gradient=GradientSpec())};vm.drawing(DrawingStroke(listOf(Point(.1f,.2f),Point(.9f,.3f)),.01f,Color.GREEN))};ready()
            assertTrue(vm.ui.value.edit.story!!.layers.any {it.type=="drawing"});capture("music-and-drawing.png",false)
            val drawn=vm.ui.value.edit;compose.runOnUiThread {vm.undo()};ready();assertFalse(vm.ui.value.edit.story!!.layers.any {it.type=="drawing"});compose.runOnUiThread {vm.redo()};ready();assertEquals(drawn,vm.ui.value.edit)
        } finally {compose.runOnUiThread {vm.closeEditor(true)};ready();runBlocking {store.deleteProject(project.id)}}
    }
    @Test fun batchExportsTwoFilesWithStyleAndKeepsEditableSources() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity);val before=vm.ui.value.projects.map {it.id}.toSet()
        val file=File(compose.activity.cacheDir,"batch-fixture.png")
        Bitmap.createBitmap(80,100,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.rgb(70,100,160));file.outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()}
        var added=emptyList<Project>();val start=System.currentTimeMillis()
        try {
            val style=vm.ui.value.styles.first {it.layers.isEmpty()}
            compose.runOnUiThread {vm.batch(listOf(Uri.fromFile(file),Uri.fromFile(file)),style,ExportOptions(ExportFormat.PNG,width=128,height=128))};ready()
            assertNull(vm.ui.value.error);assertEquals(0,vm.ui.value.batchTotal);added=runBlocking {store.list().filter {it.id !in before}};assertEquals(2,added.size)
            added.forEach {p->assertEquals(style.filter,p.edit.filter);assertNotNull(p.history);assertArrayEquals(file.readBytes(),store.source(p).readBytes())}
        } finally {
            added.ifEmpty {runBlocking {store.list().filter {it.id !in before}}}.forEach {p->runBlocking {store.deleteProject(p.id)}};file.delete()
            val resolver=compose.activity.contentResolver
            resolver.query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,arrayOf(android.provider.MediaStore.Images.Media._ID,android.provider.MediaStore.Images.Media.DISPLAY_NAME,android.provider.MediaStore.Images.Media.DATE_ADDED),"relative_path=? AND date_added>=?",arrayOf("Pictures/Luma Studio/",(start/1000).toString()),null)?.use {cursor->while(cursor.moveToNext()) {if(cursor.getString(1).startsWith("Luma_")) resolver.delete(android.content.ContentUris.withAppendedId(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cursor.getLong(0)),null,null)}}
        }
    }

}
