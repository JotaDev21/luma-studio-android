package studio.luma

import android.graphics.*
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
import studio.luma.data.StudioStore
import studio.luma.renderer.GpuRenderer
import java.io.File

@RunWith(AndroidJUnit4::class)
class JdmFilterFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun show() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        compose.runOnUiThread {compose.activity.setShowWhenLocked(true);compose.activity.setTurnScreenOn(true);compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
        compose.waitUntil(10000) {compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()};ready()
    }
    private fun ready()=compose.waitUntil(45000) {!compose.activity.studio.ui.value.busy && !compose.activity.studio.ui.value.rendering}
    private fun output()=File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")!!).apply {mkdirs()}
    private fun capture(name:String,browser:Boolean=false) {
        File(output(),name).outputStream().use {(if(browser) compose.onNodeWithTag("look-browser") else compose.onNode(isRoot())).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun allRetroLooksRenderDistinctReferencePalettesAndZeroIntensityPreservesTheOriginal()=runBlocking {
        val looks=compose.activity.studio.ui.value.filters.filter {it.meta.category=="JDM"};assertEquals(32,looks.size)
        val source=Bitmap.createBitmap(80,100,Bitmap.Config.ARGB_8888).apply {
            for(y in 0 until height) for(x in 0 until width) setPixel(x,y,if(y<4) Color.TRANSPARENT else Color.rgb(20+x*210/width,25+y*200/height,40+(x+y)*175/(width+height)))
            Canvas(this).drawRect(30f,40f,50f,60f,Paint().apply {color=Color.rgb(128,128,128)})
        }
        val immutable=source.copy(Bitmap.Config.ARGB_8888,false);val renderer=GpuRenderer();val signatures=mutableSetOf<Int>()
        try {
            val baseline=renderer.render(source,EditState())
            try {for(preset in looks) {
                val image=renderer.render(source,EditState(filter=FilterSelection(preset)))
                try {
                    val pixels=IntArray(image.width*image.height);image.getPixels(pixels,0,image.width,0,0,image.width,image.height);signatures.add(pixels.contentHashCode())
                    assertEquals("Transparency ${preset.meta.id}",0,Color.alpha(image.getPixel(5,1)))
                    val center=(44..55).flatMap {y->(34..45).map {x->image.getPixel(x,y)}}
                    val red=center.map {Color.red(it)}.average();val green=center.map {Color.green(it)}.average();val blue=center.map {Color.blue(it)}.average()
                    val matches=when {
                        preset.meta.id.startsWith("jdm-flash") -> green>red+6 && green>blue+4
                        preset.meta.id.startsWith("jdm-negative") -> green>blue+8
                        preset.meta.id.startsWith("jdm-night") -> blue>red+8 && green>red+4
                        else -> blue>green+5 && blue>red+4
                    }
                    assertTrue("Reference palette ${preset.meta.name}: $red,$green,$blue",matches)
                } finally {image.recycle()}
                val zero=renderer.render(source,EditState(filter=FilterSelection(preset,0f)))
                try {assertTrue("Intensity zero ${preset.meta.id}",baseline.sameAs(zero))} finally {zero.recycle()}
            }} finally {baseline.recycle()}
            assertEquals(32,signatures.size);assertTrue(source.sameAs(immutable))
        } finally {source.recycle();immutable.recycle();renderer.close()}
    }
    @Test fun flashCollectionSearchIntensitySaveAndReopenWorkOnPortrait() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity);val file=File(compose.activity.cacheDir,"flash-portrait.jpg")
        InstrumentationRegistry.getInstrumentation().context.assets.open("portrait-nasa.jpg").use {input->file.outputStream().use {input.copyTo(it)}}
        val project=runBlocking {store.import(Uri.fromFile(file))};val original=store.source(project).readBytes()
        try {
            compose.runOnUiThread {vm.open(project)};ready()
            compose.onNodeWithText("Flash 80/90").performClick()
            compose.onNodeWithTag("look-jdm-flash-01").performClick();ready()
            assertEquals("jdm-flash-01",vm.ui.value.edit.filter!!.preset.meta.id)
            assertEquals(.75f,vm.ui.value.edit.filter!!.intensity,.001f)
            compose.onNodeWithTag("look-intensity").performSemanticsAction(SemanticsActions.SetProgress) {it(.72f)}
            compose.runOnUiThread {vm.finishGesture()};ready();capture("flash-portrait-editor.png")
            compose.onNodeWithText("Explorar looks").performClick()
            compose.onNodeWithTag("look-search").performTextReplacement("flash azul")
            compose.onNode(hasTestTag("look-jdm-night-01") and hasAnyAncestor(hasTestTag("look-grid"))).performClick();ready()
            assertEquals("jdm-night-01",vm.ui.value.edit.filter!!.preset.meta.id)
            compose.onNodeWithTag("look-search").performTextClearance()
            compose.onNode(hasText("Flash 80/90") and hasAnyAncestor(hasTestTag("look-collections"))).performClick();ready()
            capture("flash-portrait-gallery.png",true)
            compose.onNodeWithText("Usar este look").performClick();ready()
            compose.runOnUiThread {vm.save("JDM QA")};ready()
            val saved=runBlocking {store.list().first {it.id==project.id}}
            compose.runOnUiThread {vm.closeEditor()};ready();compose.runOnUiThread {vm.open(saved)};ready()
            assertEquals(saved.edit,vm.ui.value.edit);assertArrayEquals(original,store.source(project).readBytes());assertNull(vm.ui.value.error)
            val renderer=GpuRenderer()
            try {runBlocking {
                val source=vm.ui.value.source!!;val looks=vm.ui.value.filters
                val cards=listOf<Pair<String,FilterPreset?>>("Original" to null)+listOf("jdm-chrome-01","jdm-negative-01","jdm-flash-01","jdm-night-01").map {id->val preset=looks.first {it.meta.id==id};"${preset.meta.name} · 75%" to preset}
                val sheet=Bitmap.createBitmap(480*3,656*2,Bitmap.Config.ARGB_8888);val canvas=Canvas(sheet);canvas.drawColor(Color.rgb(17,20,24))
                val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.rgb(243,238,229);textSize=25f;typeface=Typeface.create("sans-serif",Typeface.BOLD)}
                try {cards.forEachIndexed {i,(name,preset)->
                    val image=renderer.render(source,EditState(filter=preset?.let {FilterSelection(it,.75f)}),maxEdge=600)
                    try {val x=(i%3)*480f;val y=(i/3)*656f;canvas.drawBitmap(image,null,RectF(x+12,y+44,x+468,y+614),null);canvas.drawText(name,x+12,y+30,paint)} finally {image.recycle()}
                };paint.textSize=17f;paint.color=Color.LTGRAY;canvas.drawText("Retrato: NASA · filtros renderizados pelo app",492f,1280f,paint)
                File(output(),"flash-portrait-comparison.png").outputStream().use {sheet.compress(Bitmap.CompressFormat.PNG,100,it)}} finally {sheet.recycle()}
            }} finally {runBlocking {renderer.close()}}
        } finally {compose.runOnUiThread {vm.closeEditor(true)};ready();runBlocking {store.deleteProject(project.id)};file.delete()}
    }
}
