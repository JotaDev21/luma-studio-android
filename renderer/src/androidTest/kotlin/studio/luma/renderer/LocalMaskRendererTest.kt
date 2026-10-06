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
class LocalMaskRendererTest {
    private fun source(alpha:Int=255)=Bitmap.createBitmap(160,200,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.argb(alpha,80,100,120))}
    private fun red(bitmap:Bitmap,x:Float,y:Float)=Color.red(bitmap.getPixel((x*bitmap.width).toInt().coerceIn(0,bitmap.width-1),(y*bitmap.height).toInt().coerceIn(0,bitmap.height-1)))
    @Test fun radialInvertStrengthAndVisibilityOnlyAffectChosenArea()=runBlocking {
        val gpu=GpuRenderer();val src=source(160)
        val m=LocalMask("radial","Radial",type="radial",radiusX=.25f,radiusY=.25f,feather=.4f,adjustments=Adjustments(exposure=1f))
        try {
            val base=gpu.render(src,EditState());val out=gpu.render(src,EditState(masks=listOf(m)))
            assertTrue(red(out,.5f,.5f)>red(base,.5f,.5f)+20);assertEquals(red(base,.05f,.05f),red(out,.05f,.05f))
            assertEquals(160,Color.alpha(out.getPixel(80,100)))
            val inverse=gpu.render(src,EditState(masks=listOf(m.copy(inverted=true))))
            assertEquals(red(base,.5f,.5f),red(inverse,.5f,.5f));assertTrue(red(inverse,.05f,.05f)>red(base,.05f,.05f)+20)
            for(mask in listOf(m.copy(enabled=false),m.copy(strength=0f))) {val off=gpu.render(src,EditState(masks=listOf(mask)));assertTrue(base.sameAs(off));off.recycle()}
            val half=gpu.render(src,EditState(masks=listOf(m.copy(strength=.5f))))
            assertTrue(red(half,.5f,.5f) in (red(base,.5f,.5f)+1) until red(out,.5f,.5f))
            listOf(base,out,inverse,half).forEach {it.recycle()}
        } finally {src.recycle();gpu.close()}
    }
    @Test fun brushEraseAndLinearRampRenderOnGpu()=runBlocking {
        val gpu=GpuRenderer();val src=source()
        try {
            val brush=LocalMask("brush","Brush",feather=.1f,strokes=listOf(BrushStroke(listOf(Point(.2f,.5f),Point(.8f,.5f)),.15f),BrushStroke(listOf(Point(.5f,.5f)),.07f,erase=true)),adjustments=Adjustments(exposure=1f))
            val out=gpu.render(src,EditState(masks=listOf(brush)))
            assertTrue(red(out,.25f,.5f)>100);assertTrue(abs(red(out,.5f,.5f)-80)<=2);assertTrue(abs(red(out,.25f,.1f)-80)<=2)
            val linear=brush.copy(type="linear",strokes=emptyList(),start=Point(.1f,.5f),end=Point(.9f,.5f),feather=1f)
            val ramp=gpu.render(src,EditState(masks=listOf(linear)))
            assertTrue(red(ramp,.05f,.5f)>red(ramp,.5f,.5f));assertTrue(red(ramp,.5f,.5f)>red(ramp,.95f,.5f))
            out.recycle();ramp.recycle()
        } finally {src.recycle();gpu.close()}
    }
    @Test fun cropFlipRotationKeepMaskAnchoredAndPreviewMatchesExport()=runBlocking {
        val gpu=GpuRenderer();val src=source()
        try {
            val m=LocalMask("a","a",type="radial",center=Point(.3f,.35f),radiusX=.08f,radiusY=.08f,feather=.2f,adjustments=Adjustments(exposure=1f))
            for(turn in 0..3) {
                val transform=Transform(Crop(.1f,.1f,.9f,.9f),turn,true,turn%2==0)
                val p=MaskCoordinates.toDisplay(m.center,transform);val edit=EditState(transform=transform,masks=listOf(m))
                val out=gpu.render(src,edit);val preview=gpu.render(src,edit,80)
                assertTrue("rotation $turn",red(out,p.x,p.y)>100);assertTrue(abs(red(out,p.x,p.y)-red(preview,p.x,p.y))<=3)
                assertTrue(abs(red(out,.95f,.95f)-80)<=2);out.recycle();preview.recycle()
            }
        } finally {src.recycle();gpu.close()}
    }
    @Test fun localStackOrderMattersAndEffectsFollowLocalEdits()=runBlocking {
        val gpu=GpuRenderer();val src=source()
        try {
            val a=LocalMask("a","a",type="radial",radiusX=1f,radiusY=1f,feather=0f,adjustments=Adjustments(exposure=1f))
            val b=a.copy(id="b",adjustments=Adjustments(brightness=.3f))
            val first=gpu.render(src,EditState(masks=listOf(a,b)));val second=gpu.render(src,EditState(masks=listOf(b,a)))
            assertFalse(first.sameAs(second))
            val effect=EffectLayer("frame",EffectPreset(meta=AssetMeta("frame","Frame","Test"),type="frame",parameters=mapOf("amount" to 1f,"width" to .5f,"style" to 0f)))
            val framed=gpu.render(src,EditState(masks=listOf(a),effects=listOf(effect)))
            assertTrue(red(framed,.5f,.01f)<10);assertTrue(red(framed,.5f,.5f)>100)
            listOf(first,second,framed).forEach {it.recycle()}
        } finally {src.recycle();gpu.close()}
    }
}
