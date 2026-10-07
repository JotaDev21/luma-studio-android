package studio.luma.core

import kotlin.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ModelTest {
    @Test fun effectsAndTemplatesAreValidatedFromBundledAssets() {
        val effects=ProjectJson.decodeFromString<List<EffectPreset>>(java.io.File(System.getProperty("luma.effects.file")).readText())
        assertEquals(93,effects.size);assertEquals(93,effects.map { it.meta.id }.distinct().size);effects.forEach {it.validate()}
        val flashes=effects.filter {it.meta.category=="Flash"}
        assertEquals(24,flashes.size);assertEquals(24,flashes.map {it.parameters}.distinct().size)
        assertTrue(flashes.all {it.type=="flash" && "hardness" in it.parameters})
        assertEquals((EffectTypes.all-"texture").toSet(),effects.map {it.type}.toSet())
        val templates=ProjectJson.decodeFromString<List<StoryTemplate>>(java.io.File(System.getProperty("luma.templates.file")).readText())
        assertEquals(90,templates.size);assertEquals(90,templates.map {it.meta.id}.distinct().size)
        templates.forEach {template ->template.validate();assertTrue(template.document().missingPhotos in 1..6);assertTrue(template.layers.any {it.type=="text"})}
    }
    @Test fun storyHistoryRoundTripAndOldProjectsRemainCompatible() {
        val photo=StoryLayer("photo","photo",LayerTransform(10f,20f,500f,800f),asset="photo_a1234567-1234-1234-1234-123456789abc")
        val text=StoryLayer("text","text",LayerTransform(30f,40f,500f,200f,15f,.7f),content="Memórias\n2026",locked=true,typography=Typography(family="serif",size=90f,stroke=2f))
        val state=EditState(story=StoryDocument(layers=listOf(photo,text)))
        state.validate();assertEquals(state,ProjectJson.decodeFromString<EditState>(ProjectJson.encodeToString(state)))
        val history=EditHistory(state);val reordered=state.copy(story=state.story!!.copy(layers=listOf(text,photo)))
        history.commit(reordered);assertEquals(state,history.undo());assertEquals(reordered,history.redo())
        val old=ProjectJson.decodeFromString<Project>("""{"schemaVersion":1,"id":"a1234567-1234-1234-1234-123456789abc","createdAt":1,"updatedAt":2,"edit":{}}""")
        old.validate();assertNull(old.edit.story);assertTrue(old.edit.effects.isEmpty())
        assertFailsWith<IllegalArgumentException> {photo.copy(asset="../source").validate()}
        assertFailsWith<IllegalArgumentException> {state.copy(story=state.story!!.copy(layers=listOf(photo,photo))).validate()}
    }
    @Test fun effectsRoundTripKeepOrderParametersAndHistory() {
        val grain=EffectLayer("grain",EffectPreset(meta=AssetMeta("fine","Fine","Film"),type="grain",parameters=mapOf("amount" to .5f,"seed" to .2f)))
        val blur=EffectLayer("blur",EffectPreset(meta=AssetMeta("soft","Soft","Lens"),type="blur",parameters=mapOf("radius" to .3f)))
        val state=EditState(effects=listOf(grain,blur)); state.validate()
        assertEquals(state,ProjectJson.decodeFromString<EditState>(ProjectJson.encodeToString(state)))
        val history=EditHistory(state); history.commit(state.copy(effects=listOf(blur,grain)))
        assertEquals(state,history.undo())
        assertFailsWith<IllegalArgumentException> { state.copy(effects=listOf(grain,grain)).validate() }
        assertFailsWith<IllegalArgumentException> { grain.copy(preset=grain.preset.copy(type="script")).validate() }
        assertFailsWith<IllegalArgumentException> { grain.copy(preset=grain.preset.copy(parameters=mapOf("amount" to Float.NaN))).validate() }
        assertTrue(ProjectJson.decodeFromString<EditState>("{}").effects.isEmpty())
    }
    @Test fun bundledCatalogHas320ValidUniqueOriginalPresets() {
        val presets = ProjectJson.decodeFromString<List<FilterPreset>>(java.io.File(System.getProperty("luma.presets.file")).readText())
        assertEquals(320, presets.size)
        assertEquals(320, presets.map { it.meta.id }.distinct().size)
        assertEquals(setOf("Film", "CCD", "Cinema", "IG", "Y2K", "Portrait", "BW", "Editorial", "Vintage", "Nature", "Neon", "Matte", "Soft", "Golden", "Cool", "Noir", "Coastal", "Urban", "Nordic", "JDM", "Instagram"), presets.map { it.meta.category }.toSet())
        presets.forEach { it.validate() }
        val jdm=presets.filter {it.meta.category=="JDM"}
        assertEquals(32,jdm.size)
        assertEquals(32,jdm.map {listOf(it.curves,it.color,it.adjustments,it.effects)}.distinct().size)
        assertTrue(jdm.all {it.effects.map {layer->layer.preset.type}.take(2)==listOf("bloom","grain") && it.meta.tags.contains("verde")})
        val newLooks=presets.filter {it.meta.id.startsWith("studio-")}
        assertEquals(120,newLooks.size)
        assertEquals(120,newLooks.map {listOf(it.curves,it.color,it.adjustments,it.effects)}.distinct().size)
        assertTrue(newLooks.all {it.curves.red.size>2 && it.curves.blue.size>2})
        assertEquals(12,newLooks.groupBy {it.meta.category}.size)
    }
    @Test fun historyBranchesAndKeepsOnlyParameters() {
        val history = EditHistory(EditState(), 2)
        val a = EditState(adjustments = Adjustments(exposure = 1f))
        history.commit(a); history.commit(a)
        assertEquals(EditState(), history.undo()); assertFalse(history.canUndo)
        assertEquals(a, history.redo())
        history.undo(); history.commit(EditState(adjustments = Adjustments(contrast = 1.3f)))
        assertFalse(history.canRedo)
    }
    @Test fun projectRoundTripPreservesEmbeddedFilterAndCrop() {
        val p = Project(id = "a1234567-1234-1234-1234-123456789abc", createdAt = 1, updatedAt = 2,
            edit = EditState(transform = Transform(Crop(.1f, .2f, .9f, .8f), 1, true),
                filter = FilterSelection(FilterPreset(meta = AssetMeta("film", "Film", "Film")), .6f)))
        val copy = ProjectJson.decodeFromString<Project>(ProjectJson.encodeToString(p))
        copy.validate(); assertEquals(p, copy)
    }
    @Test fun malformedValuesAndFutureVersionsAreRejected() {
        assertFailsWith<IllegalArgumentException> { EditState(adjustments = Adjustments(exposure = Float.NaN)).validate() }
        assertFailsWith<IllegalArgumentException> { Crop(.7f, 0f, .6f, 1f).validate() }
        assertFailsWith<IllegalArgumentException> { Curves(rgb = listOf(Point(0f, 0f), Point(0f, .5f), Point(1f, 1f))).validate() }
        assertFailsWith<IllegalArgumentException> { FilterPreset(schemaVersion = 2, meta = AssetMeta("x", "x", "x")).validate() }
    }
    @Test fun curveInterpolatesEndpointsAndMidtones() {
        val points = listOf(Point(0f, .1f), Point(.5f, .6f), Point(1f, .95f))
        assertEquals(.35f, sampleCurve(points, .25f), .00001f)
        assertEquals(.95f, sampleCurve(points, 2f), .00001f)
    }
}
