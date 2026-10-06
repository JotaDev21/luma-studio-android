package studio.luma.core

import kotlin.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.*
import java.util.zip.*

class LibraryTest {
    private fun zip(vararg files:Pair<String,ByteArray>):ByteArray=ByteArrayOutputStream().also {out->ZipOutputStream(out).use {z->files.forEach {(name,bytes)->z.putNextEntry(ZipEntry(name));z.write(bytes);z.closeEntry()}}}.toByteArray()
    private fun manifest()=LibraryPack(meta=AssetMeta("own","Own","Personal"),filters=listOf(FilterPreset(meta=AssetMeta("filter","Filter","Custom"))))
    @Test fun allCollageLayoutsKeepSourcesCropsAndLockedLayers() {
        val config=CollageSettings(count=6)
        val initial=CollageEngine.create(config).copy(layers=CollageEngine.create(config).layers.mapIndexed {i,l->l.copy(asset="photo_a1234567-1234-1234-1234-123456789abc",photoPanX=.2f,photoTransform=Transform(Crop(.1f,0f,.9f,1f)),locked=i==0)})
        CollageSettings.layouts.forEach {layout ->
            val doc=CollageEngine.reflow(initial,config.copy(layout=layout,border=20f,cornerRadius=35f,shadow=10f));doc.validate()
            assertEquals(initial.layers.first(),doc.layers.first())
            assertEquals(initial.layers.map {it.id},doc.layers.map {it.id});assertEquals(initial.layers.map {it.asset},doc.layers.map {it.asset})
            assertTrue(doc.layers.all {it.photoPanX==.2f && it.photoTransform==initial.layers.first().photoTransform})
            assertEquals(doc,ProjectJson.decodeFromString<StoryDocument>(ProjectJson.encodeToString(doc)))
        }
        assertFailsWith<IllegalArgumentException> {CollageSettings(count=21).validate()}
    }
    @Test fun packRoundTripPreservesEmbeddedLookAndFixedPhotos() {
        val asset="photo_a1234567-1234-1234-1234-123456789abc"
        val effect=EffectLayer("effect",EffectPreset(meta=AssetMeta("grain","Grain","Film"),type="grain",parameters=mapOf("amount" to .5f)),.3f,false)
        val filter=manifest().filters.first().copy(savedAdjustments=Adjustments(exposure=.4f),savedIntensity=.6f)
        val template=StoryTemplate(meta=AssetMeta("template","Template","Personal"),layers=listOf(StoryLayer("photo","photo",LayerTransform(0f,0f,100f,100f),asset=asset)),effects=listOf(effect),filter=FilterSelection(filter,.6f))
        val style=StylePreset(meta=AssetMeta("style","Style","Personal"),effectLayers=listOf(effect),filter=template.filter)
        val pack=manifest().copy(filters=listOf(filter),templates=listOf(template),styles=listOf(style));val bytes=byteArrayOf(1,2,3)
        val output=ByteArrayOutputStream();PackCodec.write(pack,mapOf(asset to bytes),output)
        val read=PackCodec.read(ByteArrayInputStream(output.toByteArray()))
        assertEquals(pack,read.manifest);assertContentEquals(bytes,read.assets.getValue(asset));assertFalse(read.manifest.styles.first().orderedEffects().first().enabled)
    }
    @Test fun packRejectsTraversalUnexpectedEntriesFutureVersionsAndMissingAssets() {
        val json=ProjectJson.encodeToString(manifest()).toByteArray()
        listOf("../escape","/absolute","assets/../../escape","assets\\escape","script.js").forEach {name->assertFailsWith<IllegalArgumentException> {PackCodec.read(ByteArrayInputStream(zip("manifest.json" to json,name to byteArrayOf(1))))}}
        val future=ProjectJson.encodeToString(manifest().copy(schemaVersion=2)).toByteArray()
        assertFailsWith<IllegalArgumentException> {PackCodec.read(ByteArrayInputStream(zip("manifest.json" to future)))}
        val extra="assets/photo_a1234567-1234-1234-1234-123456789abc"
        assertFailsWith<IllegalArgumentException> {PackCodec.read(ByteArrayInputStream(zip("manifest.json" to json,extra to byteArrayOf(1))))}
        val template=StoryTemplate(meta=AssetMeta("template","Template","Custom"),layers=listOf(StoryLayer("photo","photo",LayerTransform(0f,0f,100f,100f),asset=extra.removePrefix("assets/"))))
        val missing=ProjectJson.encodeToString(manifest().copy(templates=listOf(template))).toByteArray()
        assertFailsWith<IllegalArgumentException> {PackCodec.read(ByteArrayInputStream(zip("manifest.json" to missing)))}
    }
    @Test fun highlyCompressedManifestIsBoundedBeforeParsing() {
        val expanded=ByteArray(2*1024*1024+1) {32};val compressed=zip("manifest.json" to expanded)
        assertTrue(compressed.size<10000)
        assertFailsWith<IllegalArgumentException> {PackCodec.read(ByteArrayInputStream(compressed))}
    }
    @Test fun bundledStylesAreValidAndKeepRealOrderedEffects() {
        val styles=ProjectJson.decodeFromString<List<StylePreset>>(File(System.getProperty("luma.styles.file")).readText())
        assertEquals(72,styles.size);assertEquals(72,styles.map {it.meta.id}.distinct().size)
        styles.forEach {it.validate();assertNotNull(it.filter);assertEquals(2,it.orderedEffects().size)}
        val illegal=styles.first().copy(layers=listOf(StoryLayer("photo","photo",LayerTransform(0f,0f,100f,100f))))
        assertFailsWith<IllegalArgumentException> {illegal.validate()}
    }
}
