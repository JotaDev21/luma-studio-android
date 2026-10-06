package studio.luma.renderer

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import studio.luma.core.*

@RunWith(AndroidJUnit4::class)
class AutomaticMaskRendererTest {
    private fun raster()=RasterMask.fromAlpha(32,32,ByteArray(1024) {if(it%32<16) 255.toByte() else 0})
    private fun source()=Bitmap.createBitmap(160,200,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.argb(160,80,100,120))}
    @Test fun automaticCoverageCorrectionsInvertAndCutoutPreserveAlpha()=runBlocking {
        val gpu=GpuRenderer();val src=source()
        val mask=LocalMask("person","Pessoa",type="person",raster=raster(),adjustments=Adjustments(exposure=1f))
        try {
            val out=gpu.render(src,EditState(masks=listOf(mask)))
            assertTrue(Color.red(out.getPixel(40,100))>100);assertTrue(Color.red(out.getPixel(140,100)) in 78..82)
            val edited=mask.copy(strokes=listOf(BrushStroke(listOf(Point(.25f,.5f)),radius=.08f,erase=true),BrushStroke(listOf(Point(.8f,.5f)),radius=.08f)))
            val correction=gpu.render(src,EditState(masks=listOf(edited)))
            assertTrue(Color.red(correction.getPixel(40,100)) in 78..82);assertTrue(Color.red(correction.getPixel(128,100))>100)
            val inverse=gpu.render(src,EditState(masks=listOf(mask.copy(inverted=true))))
            assertTrue(Color.red(inverse.getPixel(40,100)) in 78..82);assertTrue(Color.red(inverse.getPixel(140,100))>100)
            val clip=gpu.render(src,EditState(masks=listOf(mask.copy(cutout=true))))
            assertEquals(160,Color.alpha(clip.getPixel(40,100)));assertEquals(0,Color.alpha(clip.getPixel(140,100)))
            val faded=gpu.render(src,EditState(masks=listOf(mask.copy(cutout=true,strength=.5f))))
            assertEquals(80,Color.alpha(faded.getPixel(140,100)))
            listOf(out,correction,inverse,clip,faded).forEach {it.recycle()}
        } finally {src.recycle();gpu.close()}
    }
    @Test fun confidenceThresholdAndRotatedPreviewRemainSourceAligned()=runBlocking {
        val gpu=GpuRenderer();val src=source()
        try {
            val ramp=RasterMask.fromAlpha(32,32,ByteArray(1024) {((it%32)*255/31).toByte()})
            val mask=LocalMask("p","p",type="person",raster=ramp,feather=0f,adjustments=Adjustments(exposure=1f))
            val low=gpu.render(src,EditState(masks=listOf(mask)));val high=gpu.render(src,EditState(masks=listOf(mask.copy(threshold=.8f))))
            assertTrue(Color.red(low.getPixel(104,100))>100);assertTrue(Color.red(high.getPixel(104,100)) in 78..82)
            val edit=EditState(transform=Transform(Crop(.1f,.1f,.9f,.9f),1,true),masks=listOf(mask.copy(raster=raster(),cutout=true)))
            val full=gpu.render(src,edit);val preview=gpu.render(src,edit,80)
            val point=MaskCoordinates.toDisplay(Point(.25f,.5f),edit.transform)
            fun alpha(bitmap:Bitmap)=Color.alpha(bitmap.getPixel((point.x*bitmap.width).toInt(),(point.y*bitmap.height).toInt()))
            assertEquals(160,alpha(full));assertEquals(alpha(full),alpha(preview))
            listOf(low,high,full,preview).forEach {it.recycle()}
        } finally {src.recycle();gpu.close()}
    }
    @Test fun storyCutoutShowsCanvasAndLeavesOtherPhotoUnchanged()=runBlocking {
        val context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val directory=java.io.File(context.cacheDir,"automatic-story-test").apply {mkdirs()}
        val gpu=GpuRenderer();val stories=StoryRenderer(gpu)
        val file=java.io.File(directory,"photo_a1234567-1234-1234-1234-123456789abc")
        val bitmap=Bitmap.createBitmap(160,200,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.GREEN)}
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        try {
            val mask=LocalMask("p","p",type="person",raster=raster(),cutout=true)
            val photo=StoryLayer("first","photo",LayerTransform(0f,0f,100f,200f),asset=file.name,masks=listOf(mask))
            val doc=StoryDocument(200,200,Color.RED,listOf(photo,photo.copy(id="second",transform=LayerTransform(100f,0f,100f,200f),masks=emptyList())))
            val output=stories.render(doc,directory,EditState(story=doc),maxEdge=0,export=true)
            assertEquals(200,output.width);assertEquals(200,output.height)
            assertEquals(Color.GREEN,output.getPixel(25,100));assertEquals(Color.RED,output.getPixel(75,100));assertEquals(Color.GREEN,output.getPixel(150,100))
            output.recycle()
        } finally {stories.close();gpu.close();file.delete();directory.delete()}
    }
}
