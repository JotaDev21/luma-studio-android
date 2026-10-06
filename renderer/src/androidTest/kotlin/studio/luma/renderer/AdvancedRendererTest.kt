package studio.luma.renderer
import android.graphics.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import studio.luma.core.*
import studio.luma.core.Point
import java.io.File
import kotlin.math.abs
@RunWith(AndroidJUnit4::class)
class AdvancedRendererTest {
    private fun fixture()=Bitmap.createBitmap(192,256,Bitmap.Config.ARGB_8888).apply {for(y in 0 until height) for(x in 0 until width) setPixel(x,y,Color.rgb((x*255/width+y%13).coerceAtMost(255),y*255/height,(x+y)%256))}
    @Test fun allEffectFamiliesRenderDeterministicallyAndZeroIntensityIsNeutral()=runBlocking {
        val gpu=GpuRenderer();val source=fixture()
        try {
            val baseline=gpu.render(source,EditState())
            for(type in EffectTypes.all.filter {it!="texture"}) {
                val preset=EffectPreset(meta=AssetMeta(type,type,"Test"),type=type,parameters=mapOf("amount" to 1f,"size" to .7f,"radius" to .7f,"seed" to .37f),text="05 10 2026")
                val layer=EffectLayer("e",preset);val edit=EditState(effects=listOf(layer))
                val first=gpu.render(source,edit);val second=gpu.render(source,edit)
                assertTrue("$type deterministic",first.sameAs(second));assertFalse("$type changes pixels",first.sameAs(baseline))
                val zero=gpu.render(source,edit.copy(effects=listOf(layer.copy(intensity=0f))));assertTrue("$type neutral",zero.sameAs(baseline));assertEquals(255,Color.alpha(first.getPixel(96,128)))
                listOf(first,second,zero).forEach {it.recycle()}
            };baseline.recycle()
        } finally {source.recycle();gpu.close()}
    }
    @Test fun lutCurvesHslDetailAndLocalMotionSharePreviewExport()=runBlocking {
        val gpu=GpuRenderer();val source=fixture()
        try {
            val values=mutableListOf<Float>();for(b in 0..1) for(g in 0..1) for(r in 0..1) values.addAll(listOf(b.toFloat(),g.toFloat(),r.toFloat()))
            val lut=CubeLut("Swap R B",2,values);val edit=EditState(color=ColorTreatment(lut=lut))
            val swapped=gpu.render(source,edit);val input=source.getPixel(150,90);val output=swapped.getPixel(150,90)
            assertTrue(abs(Color.red(input)-Color.blue(output))<=2);assertTrue(abs(Color.blue(input)-Color.red(output))<=2)
            val curved=gpu.render(source,EditState(color=ColorTreatment(curves=Curves(rgb=listOf(Point(0f,0f),Point(.5f,.75f),Point(1f,1f))))));assertTrue(Color.green(curved.getPixel(80,100))>Color.green(source.getPixel(80,100)))
            val hsl=gpu.render(source,EditState(color=ColorTreatment(hsl=List(8) {HslBand(saturation=-1f)})));val p=hsl.getPixel(80,100);assertTrue(abs(Color.red(p)-Color.blue(p))<=2)
            val detail=gpu.render(source,EditState(adjustments=Adjustments(clarity=.3f,texture=.2f,sharpness=.4f,dehaze=.1f,denoise=.2f)));assertFalse(detail.sameAs(source))
            val motion=EffectLayer("m",EffectPreset(meta=AssetMeta("motion","Motion","Test"),type="motionblur",parameters=mapOf("amount" to 1f,"size" to 1f)))
            val local=EditState(masks=listOf(LocalMask("a","Area",type="linear",start=Point(.45f,.5f),end=Point(.55f,.5f),effects=listOf(motion))))
            val masked=gpu.render(source,local);val neutral=gpu.render(source,EditState());assertEquals(neutral.getPixel(185,125),masked.getPixel(185,125));assertTrue((0 until 80).any {x->neutral.getPixel(x,240)!=masked.getPixel(x,240)})
            listOf(swapped,curved,hsl,detail,masked,neutral).forEach {it.recycle()}
        } finally {source.recycle();gpu.close()}
    }
    @Test fun textureGradientDrawingAndImportedTypefaceRenderWithoutChangingSource()=runBlocking {
        val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"advanced-renderer").apply {mkdirs()};val gpu=GpuRenderer();val stories=StoryRenderer(gpu)
        val asset="photo_a1234567-1234-1234-1234-123456789abc";val font="font_a1234567-1234-1234-1234-123456789abc";val source=fixture()
        source.copy(Bitmap.Config.ARGB_8888,true).apply {eraseColor(Color.RED);File(dir,asset).outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()};File("/system/fonts/Roboto-Regular.ttf").copyTo(File(dir,font),overwrite=true)
        try {
            val texture=EffectLayer("e",EffectPreset(meta=AssetMeta("t","Texture","Test"),type="texture",parameters=mapOf("blend" to 0f),asset=asset))
            val textured=gpu.render(source,EditState(effects=listOf(texture)),directory=dir);assertEquals(0,Color.green(textured.getPixel(70,70)));textured.recycle()
            val draw=StoryLayer("d","drawing",LayerTransform(0f,0f,400f,600f),strokes=listOf(DrawingStroke(listOf(Point(.1f,.2f),Point(.9f,.2f)),.02f,Color.GREEN)))
            val text=StoryLayer("t","text",LayerTransform(20f,250f,360f,120f),content="LUMA",typography=Typography(size=64f,fontAsset=font,gradient=GradientSpec(listOf(Color.RED,Color.BLUE)),curvature=.3f,glow=3f))
            val doc=StoryDocument(400,600,layers=listOf(draw,text),gradient=GradientSpec(listOf(Color.BLACK,Color.WHITE)))
            val preview=stories.render(doc,dir,maxEdge=300);val full=stories.render(doc,dir,maxEdge=0,export=true)
            assertTrue(Color.green(full.getPixel(200,120))>240);assertTrue(Color.green(preview.getPixel(100,60))>240);assertTrue(Color.red(full.getPixel(10,570))>Color.red(full.getPixel(10,20)))
            preview.recycle();full.recycle()
        } finally {source.recycle();stories.close();gpu.close();dir.deleteRecursively()}
    }
}
