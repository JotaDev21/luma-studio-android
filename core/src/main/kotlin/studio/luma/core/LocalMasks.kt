package studio.luma.core

import kotlinx.serialization.Serializable
import kotlin.math.*

@Serializable data class BrushStroke(val points:List<Point>,val radius:Float=.05f,val opacity:Float=1f,val erase:Boolean=false) {
    fun validate() {require(points.size in 1..2048 && points.all {it.valid()});require(radius.isFinite() && radius in .002f.. .5f && opacity.isFinite() && opacity in 0f..1f)}
}
@Serializable data class LocalMask(val id:String,val name:String,val type:String="brush",val center:Point=Point(.5f,.5f),val radiusX:Float=.25f,val radiusY:Float=.25f,val start:Point=Point(.3f,.4f),val end:Point=Point(.7f,.6f),val feather:Float=.5f,val strength:Float=1f,val inverted:Boolean=false,val enabled:Boolean=true,val strokes:List<BrushStroke> = emptyList(),val adjustments:Adjustments=Adjustments(),val raster:RasterMask?=null,val threshold:Float=.5f,val cutout:Boolean=false,val filter:FilterSelection?=null,val effects:List<EffectLayer> = emptyList(),val color:ColorTreatment=ColorTreatment(),val fill:GradientSpec?=null) {
    fun validate() {
        filter?.let {it.preset.validate();require(it.intensity.isFinite() && it.intensity in 0f..1f)};require(effects.size<=16);effects.forEach {it.validate()};color.validate();fill?.validate()
        require(id.isNotBlank() && id.length<=100 && name.length in 1..100 && type in setOf("brush","radial","linear","person"))
        require((type=="person")== (raster!=null));raster?.validate()
        require(!cutout || type=="person")
        require(threshold.isFinite() && threshold in .05f.. .95f)
        require(center.valid() && start.valid() && end.valid() && hypot(end.x-start.x,end.y-start.y)>=.02f)
        require(listOf(radiusX,radiusY,feather,strength).all {it.isFinite()});require(radiusX in .002f..1f && radiusY in .002f..1f && feather in 0f..1f && strength in 0f..1f)
        require(strokes.size<=100 && strokes.sumOf {it.points.size}<=10000);strokes.forEach {it.validate()};adjustments.validate()
    }
    fun geometry()=copy(filter=null,effects=emptyList(),color=ColorTreatment(),fill=null,adjustments=Adjustments(),strength=1f,inverted=false,enabled=true,name="geometry",id="geometry",cutout=false)
}
private fun Point.valid()=x.isFinite() && y.isFinite() && x in 0f..1f && y in 0f..1f
fun validateMasks(masks:List<LocalMask>) {require(masks.size<=8 && masks.map {it.id}.distinct().size==masks.size);masks.forEach {it.validate()}}

/** Coordinates are anchored to the EXIF-oriented source, before crop/flip/rotation. */
object MaskCoordinates {
    fun toDisplay(point:Point,transform:Transform):Point {
        val c=transform.crop;var x=(point.x-c.left)/(c.right-c.left);var y=(point.y-c.top)/(c.bottom-c.top)
        if(transform.flipX) x=1-x;if(transform.flipY) y=1-y
        return when(transform.quarterTurns) {1->Point(1-y,x);2->Point(1-x,1-y);3->Point(y,1-x);else->Point(x,y)}
    }
    fun toSource(point:Point,transform:Transform):Point {
        var p=when(transform.quarterTurns) {1->Point(point.y,1-point.x);2->Point(1-point.x,1-point.y);3->Point(1-point.y,point.x);else->point}
        if(transform.flipX) p=p.copy(x=1-p.x);if(transform.flipY) p=p.copy(y=1-p.y)
        val c=transform.crop;return Point((c.left+p.x*(c.right-c.left)).coerceIn(0f,1f),(c.top+p.y*(c.bottom-c.top)).coerceIn(0f,1f))
    }
}
