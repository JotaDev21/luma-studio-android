package studio.luma.renderer

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import studio.luma.core.*
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class GpuRendererTest {
    @Test fun effectStackIsDeterministicDisableableAndOrderMatters() = runBlocking {
        val renderer=GpuRenderer(); val source=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(170,140,100)) }
        fun effect(id:String,type:String,p:Map<String,Float>)=EffectLayer(id,EffectPreset(meta=AssetMeta(id,id,"Test"),type=type,parameters=p))
        val grain=effect("grain","grain",mapOf("amount" to .8f,"size" to .2f,"seed" to .3f))
        val frame=effect("frame","frame",mapOf("amount" to 1f,"width" to .6f,"style" to .5f))
        val blur=effect("blur","blur",mapOf("amount" to 1f,"radius" to .8f))
        try {
            val one=renderer.render(source,EditState(effects=listOf(grain)))
            val two=renderer.render(source,EditState(effects=listOf(grain)))
            assertTrue(one.sameAs(two)); assertFalse(one.sameAs(source))
            val disabled=renderer.render(source,EditState(effects=listOf(grain.copy(enabled=false))))
            near(source.getPixel(64,64),disabled.getPixel(64,64))
            val first=renderer.render(source,EditState(effects=listOf(frame,blur)))
            val second=renderer.render(source,EditState(effects=listOf(blur,frame)))
            assertFalse(first.sameAs(second))
            listOf(one,two,disabled,first,second).forEach { it.recycle() }
        } finally { source.recycle(); renderer.close() }
    }
    @Test fun bloomSpreadsBrightAreasWithoutChangingAlpha() = runBlocking {
        val renderer=GpuRenderer(); val source=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK); for(y in 60..67) for(x in 60..67) setPixel(x,y,Color.WHITE)
        }
        try {
            val bloom=EffectLayer("bloom",EffectPreset(meta=AssetMeta("glow","Glow","Light"),type="bloom",parameters=mapOf("amount" to 1f,"radius" to .7f,"threshold" to .6f)))
            val result=renderer.render(source,EditState(effects=listOf(bloom)))
            assertTrue(Color.red(result.getPixel(58,64))>0)
            assertEquals(255,Color.alpha(result.getPixel(58,64))); result.recycle()
        } finally { source.recycle(); renderer.close() }
    }
    private fun fixture() = Bitmap.createBitmap(8, 6, Bitmap.Config.ARGB_8888).apply {
        for (y in 0 until height) for (x in 0 until width) setPixel(x,y,Color.rgb(20+x*25, 15+y*35, 50+x*10))
    }
    private fun near(a: Int, b: Int, tolerance: Int = 2) {
        assertTrue("red $a != $b", abs(Color.red(a)-Color.red(b)) <= tolerance)
        assertTrue("green $a != $b", abs(Color.green(a)-Color.green(b)) <= tolerance)
        assertTrue("blue $a != $b", abs(Color.blue(a)-Color.blue(b)) <= tolerance)
        assertEquals(Color.alpha(a), Color.alpha(b))
    }
    @Test fun identityPreservesColorsOrientationAndDimensions() = runBlocking {
        val renderer = GpuRenderer(); val source = fixture()
        try {
            val out = renderer.render(source, EditState())
            assertEquals(8, out.width); assertEquals(6, out.height)
            for (y in 0 until 6) for (x in 0 until 8) near(source.getPixel(x,y), out.getPixel(x,y))
            out.recycle()
        } finally { source.recycle(); renderer.close() }
    }
    @Test fun zeroIntensityKeepsUserAdjustmentsAndSourceImmutable() = runBlocking {
        val renderer = GpuRenderer(); val source = fixture(); val original = source.getPixel(3,3)
        try {
            val adjustment = Adjustments(exposure = .3f, contrast = 1.1f)
            val baseline = renderer.render(source, EditState(adjustments = adjustment))
            val preset = FilterPreset(meta = AssetMeta("test", "Test", "Film"), adjustments = Adjustments(saturation = 0f, exposure = -1f))
            val zero = renderer.render(source, EditState(adjustments = adjustment, filter = FilterSelection(preset, 0f)))
            near(baseline.getPixel(3,3), zero.getPixel(3,3)); assertEquals(original, source.getPixel(3,3))
            val full = renderer.render(source, EditState(filter = FilterSelection(preset)))
            val pixel = full.getPixel(3,3); assertTrue(abs(Color.red(pixel)-Color.blue(pixel)) <= 2)
            baseline.recycle(); zero.recycle(); full.recycle()
        } finally { source.recycle(); renderer.close() }
    }
    @Test fun cropRotateAndPreviewExportUseSameShader() = runBlocking {
        val renderer = GpuRenderer(); val source = fixture()
        try {
            val state = EditState(transform = Transform(Crop(0f,0f,.5f,1f), 1, true), adjustments = Adjustments(temperature = .4f))
            val full = renderer.render(source, state)
            val preview = renderer.render(source, state, 3)
            assertEquals(6, full.width); assertEquals(4, full.height)
            assertEquals(3, preview.width); assertEquals(2, preview.height)
            val expected = ImageSource.transform(source, state.transform)
            val identity = renderer.render(source, state.copy(adjustments = Adjustments()))
            near(expected.getPixel(0,0), identity.getPixel(0,0))
            full.recycle(); preview.recycle(); identity.recycle(); if (expected !== source) expected.recycle()
        } finally { source.recycle(); renderer.close() }
    }
    @Test fun translucentPngRetainsAlphaAndStraightColor() = runBlocking {
        val renderer = GpuRenderer()
        val source = Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.argb(128,120,180,60)) }
        try { val result = renderer.render(source, EditState()); near(source.getPixel(1,1),result.getPixel(1,1),3); result.recycle() }
        finally { source.recycle(); renderer.close() }
    }
}
