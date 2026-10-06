package studio.luma

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.graphics.ImageDecoder
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
class StoryStorageTest {
    @Test fun templatesPhotoAssetsPersistenceAndAllStoryExportFormats()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext;val store=StudioStore(context)
        val gpu=GpuRenderer();val stories=StoryRenderer(gpu)
        val templates=store.templates();assertEquals(90,templates.size)
        val project=store.createStory(templates.first());assertTrue(store.list().any {it.id==project.id})
        val file=File(context.cacheDir,"story-test-original.png")
        Bitmap.createBitmap(120,160,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.rgb(180,100,60));file.outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()}
        try {
            val asset=store.addPhoto(project,Uri.fromFile(file));val originalBytes=File(store.directory(project.id),asset).readBytes()
            val doc=project.edit.story!!;val filled=doc.copy(layers=doc.layers.map {if(it.type=="photo") it.copy(asset=asset,placeholder=false) else it})
            val updated=project.copy(edit=project.edit.copy(story=filled))
            val thumb=stories.render(filled,store.directory(project.id),maxEdge=320);store.save(updated,thumb);thumb.recycle()
            assertEquals(updated,store.list().first {it.id==project.id});assertTrue(store.thumbnail(updated).isFile)
            val exporter=ExportEngine(context,store,gpu,stories)
            for(format in ExportFormat.entries) {
                val uri=exporter.export(updated,ExportOptions(format,95,960))
                try {val decoded=ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver,uri));assertEquals(540,decoded.width);assertEquals(960,decoded.height);decoded.recycle()}
                finally {context.contentResolver.delete(uri,null,null)}
            }
            assertArrayEquals(originalBytes,File(store.directory(project.id),asset).readBytes())
        } finally {stories.close();gpu.close();store.directory(project.id).deleteRecursively();file.delete()}
    }
}
