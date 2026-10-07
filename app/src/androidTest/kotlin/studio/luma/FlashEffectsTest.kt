package studio.luma

import android.graphics.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import studio.luma.core.*
import studio.luma.data.StudioStore
import studio.luma.data.ExportEngine
import studio.luma.data.ExportOptions
import studio.luma.data.ExportFormat
import studio.luma.renderer.*
import java.io.File
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class FlashEffectsTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private fun signature(image:Bitmap):Int {
        val pixels=IntArray(image.width*image.height)
        image.getPixels(pixels,0,image.width,0,0,image.width,image.height)
        return pixels.contentHashCode()
    }
    @Test fun flashCatalogRendersDistinctDeterministicPortraitsAndPreservesExistingEffects()=runBlocking {
        val presets=StudioStore(instrumentation.targetContext).effects()
        val flashes=presets.filter {it.meta.category=="Flash"}
        assertEquals(24,flashes.size)
        val file=File(instrumentation.targetContext.cacheDir,"flash-portrait.jpg")
        instrumentation.context.assets.open("portrait-nasa.jpg").use {input->file.outputStream().use {input.copyTo(it)}}
        val source=ImageSource.decode(file,480);val original=signature(source);val gpu=GpuRenderer()
        val output=File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")!!).apply {mkdirs()}
        val sheet=Bitmap.createBitmap(4*250,7*355,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(sheet);canvas.drawColor(Color.rgb(22,22,25))
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.WHITE;textSize=16f}
        try {
            val baseline=gpu.render(source,EditState());val hashes=mutableSetOf<Int>()
            val samples=listOf("Original" to baseline)+flashes.map {preset->
                val layer=EffectLayer("flash",preset)
                val first=gpu.render(source,EditState(effects=listOf(layer)))
                val repeat=gpu.render(source,EditState(effects=listOf(layer)))
                val zero=gpu.render(source,EditState(effects=listOf(layer.copy(intensity=0f))))
                assertTrue(preset.meta.name,first.sameAs(repeat));assertTrue(zero.sameAs(baseline))
                assertTrue("Distinct ${preset.meta.name}",hashes.add(signature(first)))
                assertFalse(first.sameAs(baseline));repeat.recycle();zero.recycle()
                preset.meta.name to first
            }
            samples.forEachIndexed {index,(name,image)->
                val x=(index%4)*250f;val y=(index/4)*355f
                canvas.drawBitmap(image,null,RectF(x+5,y+5,x+245,y+305),null)
                canvas.drawText(name,x+8,y+328,paint);image.recycle()
            }
            File(output,"flash-effects-portrait.png").outputStream().use {sheet.compress(Bitmap.CompressFormat.PNG,100,it)}
            // Exercise the existing GPU branches with their actual bundled parameters too.
            for(preset in presets.filter {it.meta.category!="Flash"}) {
                val rendered=gpu.render(source,EditState(effects=listOf(EffectLayer("legacy",preset))))
                assertEquals(source.width,rendered.width);rendered.recycle()
            }
            assertEquals(original,signature(source))
        } finally {sheet.recycle();source.recycle();gpu.close();file.delete()}
    }
    @Test fun flashHasAdjustableRoundLightNeutralZeroIntensityAndPreservesTransparency()=runBlocking {
        val base=StudioStore(instrumentation.targetContext).effects().first {it.meta.id=="flash-effect-spot"}
        val preset=base.copy(parameters=base.parameters+mapOf("amount" to 1f,"grain" to 0f,"x" to .5f,"y" to .5f,"warmth" to .5f,"tint" to .5f))
        val gpu=GpuRenderer()
        try {
            for((w,h) in listOf(240 to 360,360 to 240)) {
                val source=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.rgb(90,90,90))}
                val layer=EffectLayer("flash",preset);val neutral=gpu.render(source,EditState())
                val full=gpu.render(source,EditState(effects=listOf(layer)))
                val half=gpu.render(source,EditState(effects=listOf(layer.copy(intensity=.5f))))
                val center=full.getPixel(w/2,h/2);val edge=full.getPixel(2,2)
                assertTrue(Color.red(center)>Color.red(neutral.getPixel(w/2,h/2)))
                assertTrue(Color.red(edge)<Color.red(neutral.getPixel(2,2)))
                for(channel in listOf<(Int)->Int>(Color::red,Color::green,Color::blue)) {
                    val expected=(channel(neutral.getPixel(w/2,h/2))+channel(center))/2
                    assertTrue(abs(channel(half.getPixel(w/2,h/2))-expected)<=2)
                }
                // The same distance on either axis has the same light response.
                assertTrue(abs(Color.red(full.getPixel(w/2+50,h/2))-Color.red(full.getPixel(w/2,h/2+50)))<=2)
                val moved=gpu.render(source,EditState(effects=listOf(layer.copy(preset=preset.copy(parameters=preset.parameters+("x" to .2f))))))
                assertFalse(full.sameAs(moved))
                listOf(neutral,full,half,moved,source).forEach {it.recycle()}
            }
            val transparent=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.argb(100,90,120,160));setPixel(0,0,Color.TRANSPARENT)}
            val result=gpu.render(transparent,EditState(effects=listOf(EffectLayer("flash",preset))))
            assertEquals(100,Color.alpha(result.getPixel(64,64)));assertEquals(0,Color.alpha(result.getPixel(0,0)))
            result.recycle();transparent.recycle()
        } finally {gpu.close()}
    }
    @Test fun storyFlashSurvivesSaveReopenAndPngExportAtAnotherResolution()=runBlocking {
        val context=instrumentation.targetContext;val store=StudioStore(context)
        val preset=store.effects().first {it.meta.id=="flash-effect-verde-90"}.let {it.copy(parameters=it.parameters+("grain" to 0f))}
        val project=store.createStory();val asset="photo_a1234567-1234-1234-1234-123456789abc"
        val file=File(store.directory(project.id),asset)
        Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(100,95,90));file.outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()
        }
        val bytes=file.readBytes();val gpu=GpuRenderer();val stories=StoryRenderer(gpu)
        var uri:android.net.Uri?=null
        try {
            val document=StoryDocument(layers=listOf(StoryLayer("photo","photo",LayerTransform(0f,0f,1080f,1920f),asset=asset)))
            val edit=EditState(story=document,effects=listOf(EffectLayer("flash",preset,intensity=.7f)))
            store.save(project.copy(edit=edit));val reopened=store.list().first {it.id==project.id}
            assertEquals(edit,reopened.edit)
            val preview=stories.render(document,store.directory(project.id),edit,192)
            val export=ExportEngine(context,store,gpu,stories).export(reopened,ExportOptions(ExportFormat.PNG,maxEdge=384))
            uri=export
            val decoded=context.contentResolver.openInputStream(export).use {BitmapFactory.decodeStream(it)}!!
            assertEquals(384,decoded.height)
            val p=preview.getPixel(preview.width/2,preview.height/2);val e=decoded.getPixel(decoded.width/2,decoded.height/2)
            assertTrue(abs(Color.red(p)-Color.red(e))<=2);assertTrue(abs(Color.green(p)-Color.green(e))<=2);assertTrue(abs(Color.blue(p)-Color.blue(e))<=2)
            assertArrayEquals(bytes,file.readBytes());preview.recycle();decoded.recycle()
        } finally {uri?.let {context.contentResolver.delete(it,null,null)};store.deleteProject(project.id);stories.close();gpu.close()}
    }
}
