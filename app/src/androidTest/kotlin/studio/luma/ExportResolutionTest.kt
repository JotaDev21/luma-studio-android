package studio.luma

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
class ExportResolutionTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun assertDimensions(uri: Uri, width: Int, height: Int) {
        try {
            val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
            context.contentResolver.openInputStream(uri).use {BitmapFactory.decodeStream(it,null,bounds)}
            assertEquals(width,bounds.outWidth);assertEquals(height,bounds.outHeight)
        } finally {context.contentResolver.delete(uri,null,null)}
    }

    @Test fun smallerPhotosExportAtTheChosenResolutionInEveryFormat()=runBlocking {
        val store=StudioStore(context);val gpu=GpuRenderer();val stories=StoryRenderer(gpu)
        val file=File(context.cacheDir,"resolution-fixture.png")
        Bitmap.createBitmap(360,640,Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(100,140,180));file.outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()
        }
        val project=store.import(Uri.fromFile(file));val source=store.source(project).readBytes()
        try {
            val exporter=ExportEngine(context,store,gpu,stories)
            assertDimensions(exporter.export(project,ExportOptions(ExportFormat.PNG)),360,640)
            for(format in ExportFormat.entries) for(edge in listOf(2560,3840)) {
                val size=ExportSize(1080,1920).atLongEdge(edge)
                assertDimensions(exporter.export(project,ExportOptions(format,maxEdge=edge)),size.width,size.height)
                assertDimensions(exporter.export(project,ExportOptions(format,maxEdge=edge,width=size.width,height=size.height)),size.width,size.height)
            }
            val rotated=project.copy(edit=project.edit.copy(transform=Transform(quarterTurns=1)))
            assertDimensions(exporter.export(rotated,ExportOptions(ExportFormat.PNG,maxEdge=2560)),2560,1440)
            assertDimensions(exporter.export(project,ExportOptions(ExportFormat.PNG,maxEdge=3840,width=512,height=512)),512,512)
            assertArrayEquals(source,store.source(project).readBytes())
        } finally {stories.close();gpu.close();store.deleteProject(project.id);file.delete()}
    }

    @Test fun storiesExportAt1440pAnd4KWithAndWithoutPublicationDimensions()=runBlocking {
        val store=StudioStore(context);val gpu=GpuRenderer();val stories=StoryRenderer(gpu)
        val project=store.createStory(StoryTemplate(meta=AssetMeta("qa_resolution","Resolution","Custom"),width=1080,height=1920,layers=emptyList()))
        try {
            val exporter=ExportEngine(context,store,gpu,stories)
            for(format in ExportFormat.entries) for(edge in listOf(2560,3840)) {
                val size=ExportSize(1080,1920).atLongEdge(edge)
                assertDimensions(exporter.export(project,ExportOptions(format,maxEdge=edge)),size.width,size.height)
                assertDimensions(exporter.export(project,ExportOptions(format,maxEdge=edge,width=size.width,height=size.height)),size.width,size.height)
            }
            assertDimensions(exporter.export(project,ExportOptions(ExportFormat.PNG,width=512,height=512)),512,512)
        } finally {stories.close();gpu.close();store.deleteProject(project.id)}
    }
}
