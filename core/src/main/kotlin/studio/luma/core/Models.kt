package studio.luma.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val ProjectJson = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = false }

@Serializable data class AssetMeta(val id: String, val name: String, val category: String, val version: Int = 1, val tags: List<String> = emptyList()) {
    fun validate() { require(id.matches(Regex("[a-zA-Z0-9_-]{1,100}")) && name.isNotBlank() && name.length<=100 && category.length in 1..60 && version>0);require(tags.size<=30 && tags.all {it.length<=60}) }
}
@Serializable data class Point(val x: Float, val y: Float)
@Serializable data class Curves(
    val rgb: List<Point> = listOf(Point(0f, 0f), Point(1f, 1f)),
    val red: List<Point> = listOf(Point(0f, 0f), Point(1f, 1f)),
    val green: List<Point> = listOf(Point(0f, 0f), Point(1f, 1f)),
    val blue: List<Point> = listOf(Point(0f, 0f), Point(1f, 1f))
) {
    fun validate() { listOf(rgb, red, green, blue).forEach { points ->
        require(points.size in 2..32 && points.first().x == 0f && points.last().x == 1f)
        require(points.all { it.x.isFinite() && it.y.isFinite() && it.x in 0f..1f && it.y in 0f..1f })
        require(points.zipWithNext().all { (a, b) -> a.x < b.x })
    } }
}

@Serializable data class Adjustments(
    val exposure: Float = 0f, val brightness: Float = 0f, val contrast: Float = 1f,
    val saturation: Float = 1f, val vibrance: Float = 0f, val temperature: Float = 0f,
    val tint: Float = 0f, val highlights: Float = 0f, val shadows: Float = 0f,
    val whites: Float = 0f, val blacks: Float = 0f, val gamma: Float = 1f,
    val fade: Float = 0f, val vignette: Float = 0f, val clarity:Float=0f,val texture:Float=0f,val sharpness:Float=0f,val dehaze:Float=0f,val denoise:Float=0f
) {
    fun validate() {
        require(listOf(clarity,texture,dehaze).all {it.isFinite() && it in -1f..1f});require(sharpness.isFinite() && sharpness in 0f..2f && denoise.isFinite() && denoise in 0f..1f)
        require(exposure.isFinite() && exposure in -3f..3f)
        require(contrast.isFinite() && contrast in 0f..2f && saturation.isFinite() && saturation in 0f..2f)
        require(gamma.isFinite() && gamma in .2f..3f)
        require(listOf(brightness, vibrance, temperature, tint, highlights, shadows, whites, blacks).all { it.isFinite() && it in -1f..1f })
        require(fade.isFinite() && fade in 0f..1f && vignette.isFinite() && vignette in 0f..1f)
    }
    fun uniforms() = floatArrayOf(exposure, brightness, contrast, saturation, vibrance, temperature, tint, highlights, shadows, whites, blacks, gamma, fade, vignette)
}

@Serializable data class FilterPreset(
    val schemaVersion: Int = 1, val meta: AssetMeta, val adjustments: Adjustments = Adjustments(),
    val curves: Curves = Curves(), val shadowTint: List<Float> = listOf(0f, 0f, 0f),
    val highlightTint: List<Float> = listOf(0f, 0f, 0f), val grading: Float = 0f,
    val savedAdjustments: Adjustments? = null, val savedIntensity: Float = 1f,val color:ColorTreatment=ColorTreatment(),val savedColor:ColorTreatment?=null,val effects:List<EffectLayer> = emptyList()
) {
    fun validate() {
        meta.validate();require(schemaVersion == 1)
        require(meta.name.isNotBlank() && meta.name.length <= 100)
        adjustments.validate(); curves.validate();color.validate();savedColor?.validate();require(effects.size<=16);effects.forEach {it.validate()}
        require(shadowTint.size == 3 && highlightTint.size == 3)
        require((shadowTint + highlightTint).all { it.isFinite() && it in -1f..1f })
        require(grading.isFinite() && grading in 0f..1f)
        savedAdjustments?.validate();require(savedIntensity.isFinite() && savedIntensity in 0f..1f)
    }
}
@Serializable data class Crop(val left: Float = 0f, val top: Float = 0f, val right: Float = 1f, val bottom: Float = 1f) {
    fun validate() { require(listOf(left, top, right, bottom).all { it.isFinite() && it in 0f..1f }); require(right - left >= .01f && bottom - top >= .01f) }
}
@Serializable data class Transform(val crop: Crop = Crop(), val quarterTurns: Int = 0, val flipX: Boolean = false, val flipY: Boolean = false) {
    fun validate() { crop.validate(); require(quarterTurns in 0..3) }
}
@Serializable data class FilterSelection(val preset: FilterPreset, val intensity: Float = 1f)
@Serializable data class EditState(val transform: Transform = Transform(), val adjustments: Adjustments = Adjustments(), val filter: FilterSelection? = null, val effects: List<EffectLayer> = emptyList(), val story: StoryDocument? = null,val masks:List<LocalMask> = emptyList(),val color:ColorTreatment=ColorTreatment()) {
    fun validate() {
        color.validate();transform.validate(); adjustments.validate(); filter?.let { it.preset.validate(); require(it.intensity.isFinite() && it.intensity in 0f..1f) }
        require(effects.size <= 16 && effects.map { it.id }.distinct().size == effects.size)
        effects.forEach { it.validate() }
        validateMasks(masks)
        story?.validate()
        require(story==null || masks.isEmpty()) {"As máscaras de Stories devem pertencer a uma foto."}
    }
}
@Serializable data class Project(
    val schemaVersion: Int = 8, val id: String, val name: String = "Sem título",
    val createdAt: Long, val updatedAt: Long, val source: String = "source", val edit: EditState = EditState(),val history:HistorySnapshot?=null
) {
    fun validate() {
        require(schemaVersion in 1..8 && id.matches(Regex("[a-f0-9-]{36}")) && source == "source")
        require(name.length in 1..100); edit.validate();history?.validate()
    }
}

@Serializable data class EffectPreset(val schemaVersion: Int = 1, val meta: AssetMeta, val type: String, val parameters: Map<String, Float>,val asset:String?=null,val text:String="") {
    fun validate() {
        meta.validate();require(schemaVersion == 1)
        require(type in EffectTypes.all);require(asset==null || asset.matches(Regex("photo_[a-f0-9-]{36}")));require(text.length<=120)
        require(parameters.keys.all { it in setOf("amount", "size", "seed", "threshold", "radius", "angle", "warmth", "width", "style", "blend", "color", "offset") })
        require(parameters.values.all { it.isFinite() && it in 0f..1f })
    }
}
@Serializable data class EffectLayer(val id: String, val preset: EffectPreset, val intensity: Float = 1f, val enabled: Boolean = true) {
    fun validate() { require(id.isNotBlank() && id.length <= 100); preset.validate(); require(intensity.isFinite() && intensity in 0f..1f) }
}
@Serializable data class LayerAppearance(val cornerRadius:Float=0f,val border:Float=0f,val bottomBorder:Float=0f,val borderColor:Int=-1,val shadow:Float=0f,val shadowColor:Int=1426063360) {
    fun validate() {require(listOf(cornerRadius,border,bottomBorder,shadow).all {it.isFinite()});require(cornerRadius in 0f..1000f && border in 0f..200f && bottomBorder in 0f..400f && shadow in 0f..100f)}
}
@Serializable data class LayerTransform(val x: Float, val y: Float, val width: Float, val height: Float, val rotation: Float = 0f, val opacity: Float = 1f) {
    fun validate() { require(listOf(x,y,width,height,rotation,opacity).all { it.isFinite() }); require(width in 8f..16384f && height in 8f..16384f && x in -16384f..16384f && y in -16384f..16384f && rotation in -360f..360f && opacity in 0f..1f) }
}
@Serializable data class Typography(val family:String="sans-serif", val size:Float=72f, val weight:Int=400, val color:Int=-1, val alignment:String="center", val spacing:Float=0f, val lineSpacing:Float=1.15f, val stroke:Float=0f, val strokeColor:Int=-16777216, val shadow:Float=0f, val background:Int=0,val fontAsset:String?=null,val gradient:GradientSpec?=null,val glow:Float=0f,val blur:Float=0f,val curvature:Float=0f) {
    fun validate() { gradient?.validate();require(fontAsset==null || fontAsset.matches(Regex("font_[a-f0-9-]{36}")));require(glow.isFinite() && glow in 0f..40f && blur.isFinite() && blur in 0f..20f && curvature.isFinite() && curvature in -1f..1f);require(family in setOf("sans-serif","serif","monospace","sans-serif-condensed","cursive")); require(size.isFinite() && size in 8f..400f && weight in 100..900); require(alignment in setOf("left","center","right")); require(spacing.isFinite() && spacing in -.1f.. .5f && lineSpacing.isFinite() && lineSpacing in .7f..2f && stroke.isFinite() && stroke in 0f..12f && shadow.isFinite() && shadow in 0f..30f) }
}
@Serializable data class StoryLayer(val id: String, val type: String, val transform: LayerTransform, val placeholder: Boolean = false, val content: String = "", val locked: Boolean = false, val visible: Boolean = true, val blendMode: String = "normal", val asset:String?=null, val typography:Typography=Typography(), val color:Int=-1, val shape:String="rect", val photoZoom:Float=1f, val photoPanX:Float=0f, val photoPanY:Float=0f, val appearance:LayerAppearance=LayerAppearance(),val photoTransform:Transform=Transform(),val styleElement:Boolean=false,val masks:List<LocalMask> = emptyList(),val photoFit:String="cover",val gradient:GradientSpec?=null,val strokes:List<DrawingStroke> = emptyList()) {
    fun validate() {
        gradient?.validate();require(strokes.size<=200 && strokes.sumOf {it.points.size}<=20000);strokes.forEach {it.validate()};require(type=="drawing" || strokes.isEmpty())
        require(photoFit in setOf("cover","contain"))
        require(id.isNotBlank() && id.length<=100 && type in setOf("photo","text","shape","sticker","drawing") && content.length<=2000)
        require(blendMode in setOf("normal","multiply","screen","overlay")); transform.validate(); typography.validate();appearance.validate();photoTransform.validate()
        require(asset==null || asset.matches(Regex("photo_[a-f0-9-]{36}")))
        require(type=="photo" || asset==null)
        validateMasks(masks);require(type=="photo" || masks.isEmpty())
        require(shape in setOf("rect","circle","line","arrow"))
        require(photoZoom.isFinite() && photoZoom in 1f..4f && photoPanX.isFinite() && photoPanX in -1f..1f && photoPanY.isFinite() && photoPanY in -1f..1f)
    }
}
@Serializable data class StoryDocument(val width:Int=1080,val height:Int=1920,val background:Int=-15724528,val layers:List<StoryLayer> = emptyList(),val templateId:String?=null,val collage:CollageSettings?=null,val gradient:GradientSpec?=null,val backgroundAsset:String?=null,val backgroundBlur:Float=.5f) {
    fun validate() { gradient?.validate();require(backgroundAsset==null || backgroundAsset.matches(Regex("photo_[a-f0-9-]{36}")));require(backgroundBlur.isFinite() && backgroundBlur in 0f..1f);require(width in 64..8192 && height in 64..8192 && width.toLong()*height<=32_000_000); require(layers.size<=80 && layers.map { it.id }.distinct().size==layers.size); layers.forEach { it.validate() };collage?.validate() }
    val missingPhotos get()=layers.count { it.type=="photo" && it.asset==null && it.visible }
}
@Serializable data class StoryTemplate(val schemaVersion: Int = 1, val meta: AssetMeta, val width: Int = 1080, val height: Int = 1920, val layers: List<StoryLayer>, val background:Int=-15724528,val adjustments:Adjustments=Adjustments(),val filter:FilterSelection?=null,val effects:List<EffectLayer> = emptyList(),val collage:CollageSettings?=null,val color:ColorTreatment=ColorTreatment(),val gradient:GradientSpec?=null,val backgroundAsset:String?=null,val backgroundBlur:Float=.5f) {
    fun document()=StoryDocument(width,height,background,layers,meta.id,collage,gradient,backgroundAsset,backgroundBlur)
    fun editState()=EditState(adjustments=adjustments,filter=filter,effects=effects,story=document(),color=color)
    fun validate() {meta.validate();require(schemaVersion==1);editState().validate()}
}
@Serializable data class StylePreset(val schemaVersion: Int = 1, val meta: AssetMeta, val filter: FilterSelection? = null, val adjustments: Adjustments = Adjustments(), val effects: List<EffectPreset> = emptyList(), val layers: List<StoryLayer> = emptyList(),val effectLayers:List<EffectLayer> = emptyList(),val width:Int=1080,val height:Int=1920,val color:ColorTreatment=ColorTreatment()) {
    fun orderedEffects()=effectLayers.ifEmpty {effects.mapIndexed {index,preset->EffectLayer("style_effect_$index",preset)}}
    fun validate() {meta.validate();require(schemaVersion==1 && effects.size<=16);effects.forEach {it.validate()};require(layers.all {it.type!="photo" && it.asset==null});EditState(adjustments=adjustments,filter=filter,effects=orderedEffects(),color=color,story=StoryDocument(width,height,layers=layers)).validate()}
}

fun sampleCurve(points: List<Point>, x: Float): Float {
    val v = x.coerceIn(0f, 1f)
    val i = points.indexOfFirst { it.x >= v }.coerceAtLeast(1)
    val a = points[i - 1]; val b = points[i]
    return a.y + (b.y - a.y) * ((v - a.x) / (b.x - a.x))
}

/** Parameter snapshots only. A slider gesture is committed once, on release. */
class EditHistory(initial: EditState, private val limit: Int = 80, restored:HistorySnapshot?=null) {
    private val undo = ArrayDeque<EditState>()
    private val redo = ArrayDeque<EditState>()
    var current = initial; private set
    init {restored?.validate();restored?.undo?.let {undo.addAll(it)};restored?.redo?.let {redo.addAll(it)}}
    fun snapshot()=HistorySnapshot(undo.toList().takeLast(20),redo.toList().takeLast(20))
    val canUndo get() = undo.isNotEmpty()
    val canRedo get() = redo.isNotEmpty()
    fun commit(next: EditState) {
        next.validate(); if (next == current) return
        undo.addLast(current); if (undo.size > limit) undo.removeFirst()
        current = next; redo.clear()
    }
    fun undo(): EditState { if (canUndo) { redo.addLast(current); current = undo.removeLast() }; return current }
    fun redo(): EditState { if (canRedo) { undo.addLast(current); current = redo.removeLast() }; return current }
}
