package studio.luma
import android.graphics.*
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
class AdvancedStorageTest {
    @Test fun importedFontLutTexturePackAndDeletionLeaveExistingProjectsSelfContained()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext;val store=StudioStore(context)
        val catalog=File(context.filesDir,"library/catalog.json");val before=if(catalog.exists()) catalog.readBytes() else null
        val assetDir=File(context.filesDir,"library/assets");val existing=assetDir.listFiles()?.map {it.name}?.toSet() ?: emptySet()
        val files=mutableListOf<File>();val projects=mutableListOf<Project>();val gpu=GpuRenderer();val renderer=StoryRenderer(gpu)
        try {
            val font=store.importResource(Uri.fromFile(File("/system/fonts/Roboto-Regular.ttf")),"font","My font")
            val lutFile=File(context.cacheDir,"identity.cube").also {files.add(it)};lutFile.writeText("LUT_3D_SIZE 2\n0 0 0\n1 0 0\n0 1 0\n1 1 0\n0 0 1\n1 0 1\n0 1 1\n1 1 1")
            val lut=store.importResource(Uri.fromFile(lutFile),"lut","Identity")
            val picture=File(context.cacheDir,"pack-texture.png").also {files.add(it)}
            Bitmap.createBitmap(80,100,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.RED);picture.outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()}
            val texture=store.importResource(Uri.fromFile(picture),"texture","Red texture")
            val text=StoryLayer("text","text",LayerTransform(10f,10f,250f,100f),content="LUMA",typography=Typography(size=60f,fontAsset=font.asset))
            val template=StoryTemplate(meta=AssetMeta("qa_adv","Advanced","Custom"),width=300,height=400,layers=listOf(text),color=ColorTreatment(lut=CubeParser.parse(lutFile.readText())),backgroundAsset=texture.asset)
            store.duplicateTemplate(template)
            val project=store.createStory(template);projects.add(project)
            val rendered=renderer.render(project.edit.story!!,store.directory(project.id),project.edit);assertTrue(rendered.width>0);rendered.recycle()
            val pack=File(context.cacheDir,"advanced.lumapack").also {files.add(it)};store.exportLibrary(Uri.fromFile(pack));val imported=store.importPack(Uri.fromFile(pack));assertTrue(imported.itemIds.size>=4)
            val snapshot=store.snapshot();val importedTemplate=snapshot.templates.last();val copied=store.createStory(importedTemplate);projects.add(copied)
            assertNotEquals(template.backgroundAsset,importedTemplate.backgroundAsset);assertNotEquals(font.asset,importedTemplate.layers.single().typography.fontAsset)
            store.removeLibrary(setOf(imported.id,font.meta.id,texture.meta.id,lut.meta.id,template.meta.id))
            for(p in projects) {val output=renderer.render(p.edit.story!!,store.directory(p.id),p.edit);assertTrue(output.width>0);output.recycle()}
            val exported=ExportEngine(context,store,gpu,renderer).export(project,ExportOptions(ExportFormat.PNG,width=512,height=512))
            try {context.contentResolver.openInputStream(exported).use {input->val image=BitmapFactory.decodeStream(input);assertEquals(512,image.width);assertEquals(512,image.height);assertEquals(0,Color.alpha(image.getPixel(1,1)));image.recycle()}} finally {context.contentResolver.delete(exported,null,null)}
        } finally {renderer.close();gpu.close();projects.forEach {store.deleteProject(it.id)};files.forEach {it.delete()};if(before==null) catalog.delete() else catalog.writeBytes(before);assetDir.listFiles()?.filter {it.name !in existing}?.forEach {it.delete()}}
    }
    @Test fun referenceColorStatisticsAreLocalAndBounded() {
        val source=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.rgb(80,100,120))};val reference=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.rgb(110,110,100))}
        try {val result=ReferenceMatcher.match(source,reference);result.validate();assertTrue(result.matrix[3]>0f);assertTrue(result.matrix[11]<0f);assertNull(result.lut)} finally {source.recycle();reference.recycle()}
    }
}
