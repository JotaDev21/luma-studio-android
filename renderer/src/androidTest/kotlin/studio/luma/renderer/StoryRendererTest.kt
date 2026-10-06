package studio.luma.renderer

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import studio.luma.core.*
import java.io.File

@RunWith(AndroidJUnit4::class)
class StoryRendererTest {
    @Test fun entirePhotoModePreservesEdgesAndMarginsInPreviewAndExport()=runBlocking {
        val gpu=GpuRenderer();val renderer=StoryRenderer(gpu)
        val directory=File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"entire-photo-test").apply {mkdirs()}
        val asset="photo_a1234567-1234-1234-1234-123456789abc";val file=File(directory,asset)
        Bitmap.createBitmap(160,80,Bitmap.Config.ARGB_8888).apply {
            for(y in 0 until height) for(x in 0 until width) setPixel(x,y,when {x<40->Color.RED;x>=120->Color.GREEN;else->Color.BLUE})
            file.outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()
        }
        try {
            val layer=StoryLayer("photo","photo",LayerTransform(0f,0f,400f,400f),asset=asset,photoFit="contain")
            val doc=StoryDocument(400,400,Color.YELLOW,listOf(layer))
            val preview=renderer.render(doc,directory,maxEdge=200);val exported=renderer.render(doc,directory,maxEdge=0,export=true)
            assertEquals(Color.YELLOW,preview.getPixel(100,25));assertEquals(Color.YELLOW,exported.getPixel(200,50))
            assertEquals(Color.RED,preview.getPixel(10,100));assertEquals(Color.RED,exported.getPixel(20,200))
            assertEquals(Color.GREEN,preview.getPixel(190,100));assertEquals(Color.GREEN,exported.getPixel(380,200))
            val cover=renderer.render(doc.copy(layers=listOf(layer.copy(photoFit="cover"))),directory,maxEdge=0,export=true)
            assertEquals(Color.BLUE,cover.getPixel(20,200));assertEquals(Color.BLUE,cover.getPixel(380,200))
            listOf(preview,exported,cover).forEach {it.recycle()}
        } finally {renderer.close();gpu.close();file.delete();directory.delete()}
    }
    @Test fun photoFrameRoundedCornersShadowAndSourceCropAreRendered()=runBlocking {
        val gpu=GpuRenderer();val renderer=StoryRenderer(gpu);val directory=File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"appearance-test").apply {mkdirs()}
        val asset="photo_a1234567-1234-1234-1234-123456789abc";val file=File(directory,asset)
        Bitmap.createBitmap(160,100,Bitmap.Config.ARGB_8888).apply {for(y in 0 until height) for(x in 0 until width) setPixel(x,y,if(x<80) Color.RED else Color.BLUE);file.outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()}
        val bytes=file.readBytes()
        try {
            val layer=StoryLayer("photo","photo",LayerTransform(50f,100f,200f,100f),asset=asset,appearance=LayerAppearance(cornerRadius=30f,border=10f,bottomBorder=10f,shadow=8f),photoTransform=Transform(Crop(0f,0f,.5f,1f),1))
            val output=renderer.render(StoryDocument(300,300,Color.TRANSPARENT,listOf(layer)),directory,maxEdge=0,export=true)
            assertEquals(Color.WHITE,output.getPixel(150,104));assertEquals(Color.RED,output.getPixel(150,150))
            assertTrue(Color.alpha(output.getPixel(50,100))<255);assertTrue(Color.alpha(output.getPixel(45,150))>0);assertEquals(0,Color.alpha(output.getPixel(0,0)))
            assertArrayEquals(bytes,file.readBytes());output.recycle()
        } finally {renderer.close();gpu.close();file.delete();directory.delete()}
    }
    @Test fun layersRespectOrderOpacityVisibilityAndPreviewExportScale()=runBlocking {
        val gpu=GpuRenderer();val renderer=StoryRenderer(gpu)
        val directory=InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val bottom=StoryLayer("bottom","shape",LayerTransform(0f,0f,200f,200f),color=Color.RED)
        val top=StoryLayer("top","shape",LayerTransform(50f,50f,100f,100f,opacity=.5f),color=Color.BLUE)
        val doc=StoryDocument(200,200,Color.TRANSPARENT,listOf(bottom,top))
        try {
            val preview=renderer.render(doc,directory,maxEdge=100)
            val exported=renderer.render(doc,directory,maxEdge=400,export=true)
            assertEquals(100,preview.width);assertEquals(400,exported.width)
            val pixel=preview.getPixel(50,50);assertTrue(Color.red(pixel) in 125..130);assertTrue(Color.blue(pixel) in 125..130)
            assertEquals(pixel,exported.getPixel(200,200))
            val hidden=renderer.render(doc.copy(layers=listOf(bottom,top.copy(visible=false))),directory)
            assertEquals(Color.RED,hidden.getPixel(100,100))
            val swapped=renderer.render(doc.copy(layers=listOf(top,bottom)),directory)
            assertEquals(Color.RED,swapped.getPixel(100,100))
            val blended=renderer.render(doc.copy(layers=listOf(bottom,top.copy(blendMode="multiply",transform=top.transform.copy(opacity=1f)))),directory)
            assertEquals(Color.BLACK,blended.getPixel(100,100))
            listOf(preview,exported,hidden,swapped,blended).forEach {it.recycle()}
        } finally {renderer.close();gpu.close()}
    }
    @Test fun missingPhotosArePreviewedButNeverExportedAndTextKeepsAlpha()=runBlocking {
        val gpu=GpuRenderer();val renderer=StoryRenderer(gpu);val directory=InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val placeholder=StoryLayer("photo","photo",LayerTransform(0f,0f,200f,200f))
        try {
            val doc=StoryDocument(200,200,layers=listOf(placeholder))
            val preview=renderer.render(doc,directory);assertEquals(200,preview.width);preview.recycle()
            try {renderer.render(doc,directory,export=true);fail("Missing photo should block export")} catch(expected:IllegalArgumentException) {assertTrue(expected.message!!.contains("fotos"))}
            val text=StoryLayer("text","text",LayerTransform(10f,10f,180f,80f),content="LUMA",typography=Typography(size=38f))
            val image=renderer.render(StoryDocument(200,200,Color.TRANSPARENT,listOf(text)),directory,maxEdge=0,export=true)
            assertEquals(0,Color.alpha(image.getPixel(199,199)))
            assertTrue((10..80).any {y->(10..189).any {x->Color.alpha(image.getPixel(x,y))>0}});image.recycle()
        } finally {renderer.close();gpu.close()}
    }
    @Test fun photoLayersShareGpuAdjustmentsAndPreserveOriginalBytes()=runBlocking {
        val gpu=GpuRenderer();val renderer=StoryRenderer(gpu);val directory=File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"story-render-test").apply {mkdirs()}
        val asset="photo_a1234567-1234-1234-1234-123456789abc";val file=File(directory,asset)
        Bitmap.createBitmap(160,100,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.rgb(160,80,30));file.outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()}
        val bytes=file.readBytes()
        try {
            val layer=StoryLayer("photo","photo",LayerTransform(0f,0f,200f,200f),asset=asset)
            val doc=StoryDocument(200,200,layers=listOf(layer))
            val edit=EditState(adjustments=Adjustments(saturation=0f),story=doc)
            val preview=renderer.render(doc,directory,edit,maxEdge=100);val output=renderer.render(doc,directory,edit,maxEdge=400,export=true)
            val p=preview.getPixel(50,50);val e=output.getPixel(200,200)
            assertTrue(kotlin.math.abs(Color.red(p)-Color.blue(p))<=2);assertEquals(p,e)
            assertArrayEquals(bytes,file.readBytes());preview.recycle();output.recycle()
        } finally {renderer.close();gpu.close();file.delete();directory.delete()}
    }
}
