package studio.luma

import android.graphics.*
import android.text.StaticLayout
import android.text.TextPaint
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import studio.luma.core.*
import studio.luma.data.*
import studio.luma.renderer.*
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExpansionStorageTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private fun sourceFile()=File(instrumentation.targetContext.cacheDir,"expansion-portrait.jpg").apply {
        instrumentation.context.assets.open("portrait-nasa.jpg").use {input->outputStream().use {input.copyTo(it)}}
    }
    private fun output()=File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")!!).apply {mkdirs()}
    private fun signature(image:Bitmap):Int {
        val pixels=IntArray(image.width*image.height);image.getPixels(pixels,0,image.width,0,0,image.width,image.height)
        return pixels.contentHashCode()
    }
    private fun assertTextFits(layer:StoryLayer) {
        if(layer.type!="text") return
        val style=layer.typography
        val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {textSize=style.size;typeface=Typeface.create(Typeface.create(style.family,Typeface.NORMAL),style.weight,false);letterSpacing=style.spacing}
        val layout=StaticLayout.Builder.obtain(layer.content,0,layer.content.length,paint,layer.transform.width.toInt())
            .setIncludePad(false).setLineSpacing(style.size*(style.lineSpacing-1f),1f).build()
        assertTrue("Clipped ${layer.id}: ${layer.content}, ${layout.height} > ${layer.transform.height}",layout.height<=layer.transform.height)
    }
    private fun sheet(name:String,cards:List<Pair<String,Bitmap>>,portrait:Boolean=true) {
        val cw=310;val pictureHeight=if(portrait) 510 else 365;val ch=pictureHeight+66
        val bitmap=Bitmap.createBitmap(cw*4,ch*((cards.size+3)/4)+38,Bitmap.Config.ARGB_8888)
        try {
            val canvas=Canvas(bitmap);canvas.drawColor(Color.rgb(17,20,24))
            val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.rgb(243,238,229);textSize=17f;typeface=Typeface.create("sans-serif",Typeface.BOLD)}
            cards.forEachIndexed {i,(title,image)->val x=(i%4)*cw.toFloat();val y=(i/4)*ch.toFloat()
                canvas.drawText(title,x+12,y+25,paint)
                canvas.drawBitmap(image,null,RectF(x+12,y+42,x+cw-12,y+42+pictureHeight),Paint(Paint.FILTER_BITMAP_FLAG))
            }
            paint.textSize=15f;paint.color=Color.LTGRAY
            canvas.drawText("Retrato de teste: NASA · composição e filtros renderizados pelo app",12f,bitmap.height-12f,paint)
            File(output(),name).outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        } finally {bitmap.recycle()}
    }
    @Test fun all48SocialFiltersAreDistinctAndMonochromeStaysNeutral()=runBlocking {
        val store=StudioStore(instrumentation.targetContext);val gpu=GpuRenderer()
        val original=Bitmap.createBitmap(96,128,Bitmap.Config.ARGB_8888).apply {
            for(y in 0 until height) for(x in 0 until width) setPixel(x,y,Color.rgb(20+x*210/width,30+y*200/height,15+(x+y)*215/(width+height)))
        }
        val portraits=instrumentation.context.assets.open("portrait-nasa.jpg").use {BitmapFactory.decodeStream(it)}
        val cards=mutableListOf<Pair<String,Bitmap>>()
        try {
            val looks=store.catalog().filter {it.meta.category=="Instagram"};assertEquals(48,looks.size)
            val signatures=looks.map {preset->
                val image=gpu.render(original,EditState(filter=FilterSelection(preset)))
                try {
                    if(preset.meta.id.startsWith("social-moon-") || preset.meta.id.startsWith("social-tokyo-")) {
                        for(y in 5 until image.height step 7) for(x in 5 until image.width step 7) {
                            val pixel=image.getPixel(x,y);assertTrue("Neutral ${preset.meta.name}",kotlin.math.abs(Color.red(pixel)-Color.green(pixel))<=1 && kotlin.math.abs(Color.green(pixel)-Color.blue(pixel))<=1)
                        }
                    }
                    signature(image)
                } finally {image.recycle()}
            }
            assertEquals(48,signatures.toSet().size)
            cards.add("Original" to gpu.render(portraits,EditState(),maxEdge=400))
            looks.filter {it.meta.id.endsWith("-classic")}.forEach {preset->cards.add(preset.meta.name+" · 85%" to gpu.render(portraits,EditState(filter=FilterSelection(preset,preset.savedIntensity)),maxEdge=400))}
            sheet("instagram-portrait-looks.png",cards,portrait=false)
        } finally {cards.forEach {it.second.recycle()};portraits.recycle();original.recycle();gpu.close()}
    }
    @Test fun all48TemplatesRenderFilledAndSixPhotoStoryExportsAndReopens()=runBlocking {
        val context=instrumentation.targetContext;val store=StudioStore(context);val gpu=GpuRenderer();val renderer=StoryRenderer(gpu)
        val templates=store.templates().filter {it.meta.id.startsWith("social-template-")};assertEquals(48,templates.size)
        val project=store.createStory(templates.first {it.document().missingPhotos==6});val file=sourceFile()
        val cards=mutableListOf<Pair<String,Bitmap>>()
        try {
            val asset=store.addPhoto(project,Uri.fromFile(file));val bytes=File(store.directory(project.id),asset).readBytes()
            val signatures=mutableSetOf<Int>()
            for(template in templates) {
                template.layers.forEach(::assertTextFits)
                val document=template.document().copy(layers=template.layers.map {if(it.type=="photo") it.copy(asset=asset,placeholder=false) else it})
                assertEquals(0,document.missingPhotos)
                val image=renderer.render(document,store.directory(project.id),template.editState(),maxEdge=512)
                signatures.add(signature(image));cards.add(template.meta.name to image)
            }
            assertEquals(48,signatures.size)
            cards.chunked(8).forEachIndexed {index,group->sheet("new-story-templates-${index+1}.png",group)}
            val doc=project.edit.story!!.copy(layers=project.edit.story!!.layers.map {if(it.type=="photo") it.copy(asset=asset,placeholder=false) else it})
            val updated=project.copy(edit=project.edit.copy(story=doc))
            val thumbnail=renderer.render(doc,store.directory(project.id),updated.edit,maxEdge=320)
            try {store.save(updated,thumbnail)} finally {thumbnail.recycle()}
            assertEquals(updated,store.list().first {it.id==project.id})
            val uri=ExportEngine(context,store,gpu,renderer).export(updated,ExportOptions(ExportFormat.PNG,95,960))
            try {context.contentResolver.openInputStream(uri)!!.use {stream->val image=BitmapFactory.decodeStream(stream);try {assertEquals(540,image.width);assertEquals(960,image.height)} finally {image.recycle()}}}
            finally {context.contentResolver.delete(uri,null,null)}
            assertArrayEquals(bytes,File(store.directory(project.id),asset).readBytes())
        } finally {cards.forEach {it.second.recycle()};renderer.close();gpu.close();store.deleteProject(project.id);file.delete()}
    }
    @Test fun all48StylesRenderIncludingEditableGraphicsWithoutExternalAssets()=runBlocking {
        val store=StudioStore(instrumentation.targetContext);val gpu=GpuRenderer();val renderer=StoryRenderer(gpu)
        val project=store.createStory();val file=sourceFile();val cards=mutableListOf<Pair<String,Bitmap>>()
        try {
            val asset=store.addPhoto(project,Uri.fromFile(file));val bytes=File(store.directory(project.id),asset).readBytes()
            val styles=store.styles().filter {it.meta.id.startsWith("luma-style-social-")};assertEquals(48,styles.size)
            assertEquals(24,styles.count {it.layers.isEmpty()});assertEquals(24,styles.count {it.layers.isNotEmpty()})
            val signatures=mutableSetOf<Int>()
            for((index,style) in styles.withIndex()) {
                style.layers.forEach(::assertTextFits)
                assertTrue(style.assetNames().isEmpty())
                val document=StoryDocument(layers=listOf(StoryLayer("SOURCE","photo",LayerTransform(0f,0f,1080f,1920f),asset=asset))+style.layers)
                val image=renderer.render(document,store.directory(project.id),EditState(filter=style.filter,adjustments=style.adjustments,effects=style.orderedEffects(),color=style.color),maxEdge=512)
                signatures.add(signature(image))
                if(index in listOf(0,2,13,15,24,27,32,35)) cards.add(style.meta.name to image) else image.recycle()
            }
            assertEquals(48,signatures.size);sheet("new-styles-portrait.png",cards)
            assertArrayEquals(bytes,File(store.directory(project.id),asset).readBytes())
        } finally {cards.forEach {it.second.recycle()};renderer.close();gpu.close();store.deleteProject(project.id);file.delete()}
    }
}
