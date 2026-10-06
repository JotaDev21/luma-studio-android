package studio.luma

import android.graphics.Bitmap
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
import java.io.File

@RunWith(AndroidJUnit4::class)
class LibraryStorageTest {
    @Test fun ownTemplateFixedAssetsPackRoundTripAndFailedImportAreAtomic()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext;val store=StudioStore(context)
        val catalog=File(context.filesDir,"library/catalog.json");val before=if(catalog.exists()) catalog.readBytes() else null
        val assetDir=File(context.filesDir,"library/assets");val existing=assetDir.listFiles()?.map {it.name}?.toSet() ?: emptySet()
        val projects=mutableListOf<Project>();val input=File(context.cacheDir,"own-library-source.png");val pack=File(context.cacheDir,"own-library.storypack");val invalid=File(context.cacheDir,"bad-library.storypack")
        Bitmap.createBitmap(120,160,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.rgb(100,170,80));input.outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)};recycle()}
        try {
            val doc=CollageEngine.create(CollageSettings(count=2,layout="polaroid"))
            val project=store.createStory(StoryTemplate(meta=AssetMeta("diagnostic","Diagnostic","Test"),layers=doc.layers,collage=doc.collage));projects.add(project)
            val mask=LocalMask("fixed_mask","Retoque fixo",type="person",raster=RasterMask.fromAlpha(16,16,ByteArray(256) {if(it%16<8) 255.toByte() else 0}),adjustments=Adjustments(exposure=.4f))
            val photo=store.addPhoto(project,Uri.fromFile(input));val filled=doc.copy(layers=doc.layers.map {it.copy(asset=photo,placeholder=false,masks=listOf(mask),photoFit="contain")})
            val edit=EditState(story=filled,filter=FilterSelection(store.catalog().first(),.5f),effects=listOf(EffectLayer("grain",store.effects().first(),.4f,false)))
            val template=store.saveTemplate(project.copy(edit=edit),"Meu teste",setOf(filled.layers.first().id),emptySet())
            assertEquals("cover",template.layers.first().photoFit);assertEquals("contain",template.layers.last().photoFit);
            assertEquals(1,template.document().missingPhotos);assertEquals(edit.effects,template.effects)
            assertTrue(template.layers.first().masks.isEmpty());assertEquals(listOf(mask),template.layers.last().masks)
            val created=store.createStory(template);projects.add(created);val fixed=created.edit.story!!.layers.last().asset!!
            assertArrayEquals(input.readBytes(),File(store.directory(created.id),fixed).readBytes())
            assertEquals(template.filter,created.edit.filter)
            assertEquals(listOf(mask),created.edit.story!!.layers.last().masks)
            store.saveStyle(StylePreset(meta=AssetMeta("qa_style","QA style","Test"),filter=edit.filter,effectLayers=edit.effects))
            store.saveFilter(edit.filter!!.preset.copy(meta=AssetMeta("qa_filter","QA filter","Custom"),savedAdjustments=Adjustments(exposure=.3f),savedIntensity=.5f))
            store.exportLibrary(Uri.fromFile(pack));val oldSnapshot=store.snapshot()
            val installed=store.importPack(Uri.fromFile(pack));assertEquals(3,installed.itemIds.size)
            val snapshot=store.snapshot();assertEquals(oldSnapshot.templates.size+1,snapshot.templates.size)
            val imported=snapshot.templates.last();assertNotEquals(template.meta.id,imported.meta.id);assertNotEquals(template.layers.last().asset,imported.layers.last().asset)
            assertEquals(listOf(mask),imported.layers.last().masks);assertEquals("contain",imported.layers.last().photoFit)
            val importedProject=store.createStory(imported);projects.add(importedProject)
            assertArrayEquals(input.readBytes(),File(store.directory(importedProject.id),imported.layers.last().asset!!).readBytes())
            val bad=LibraryPack(meta=AssetMeta("bad","Bad","Test"),templates=listOf(template))
            invalid.outputStream().use {PackCodec.write(bad,mapOf(template.layers.last().asset!! to byteArrayOf(1,2,3)),it)}
            try {store.importPack(Uri.fromFile(invalid));fail("Invalid image should fail")} catch(expected:Exception) {assertEquals(snapshot,store.snapshot())}
            assertArrayEquals(input.readBytes(),File(store.directory(project.id),photo).readBytes())
        } finally {
            projects.forEach {store.directory(it.id).deleteRecursively()};listOf(input,pack,invalid).forEach {it.delete()}
            if(before==null) catalog.delete() else catalog.writeBytes(before)
            assetDir.listFiles()?.filter {it.name !in existing}?.forEach {it.delete()}
        }
    }
}
