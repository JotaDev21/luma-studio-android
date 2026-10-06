package studio.luma.core

import kotlinx.serialization.Serializable
import kotlin.math.*

@Serializable data class HslBand(val hue:Float=0f,val saturation:Float=0f,val luminance:Float=0f) {
    fun validate()=require(listOf(hue,saturation,luminance).all {it.isFinite() && it in -1f..1f})
}
@Serializable data class ColorTreatment(val curves:Curves=Curves(),val hsl:List<HslBand> = List(8) {HslBand()},val shadows:List<Float> = List(3) {0f},val midtones:List<Float> = List(3) {0f},val highlights:List<Float> = List(3) {0f},val balance:List<Float> = List(3) {0f},val rolloff:Float=0f,val luminance:Float=0f,val matrix:List<Float> = listOf(1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f,0f),val lut:CubeLut?=null,val lutIntensity:Float=1f) {
    fun validate() {
        curves.validate();require(hsl.size==8);hsl.forEach {it.validate()}
        listOf(shadows,midtones,highlights,balance).forEach {require(it.size==3 && it.all {v->v.isFinite() && v in -1f..1f})}
        require(rolloff.isFinite() && rolloff in 0f..1f && luminance.isFinite() && luminance in -1f..1f)
        require(matrix.size==12 && matrix.all {it.isFinite() && it in -4f..4f});lut?.validate();require(lutIntensity.isFinite() && lutIntensity in 0f..1f)
    }
}
@Serializable data class CubeLut(val name:String,val size:Int,val values:List<Float>,val domainMin:List<Float> = List(3) {0f},val domainMax:List<Float> = List(3) {1f}) {
    fun validate() {
        require(name.length in 1..100 && size in 2..32 && values.size==size*size*size*3 && values.all {it.isFinite() && it in 0f..1f})
        require(domainMin.size==3 && domainMax.size==3 && (domainMin+domainMax).all {it.isFinite() && it in -16f..16f})
        require((0..2).all {domainMax[it]-domainMin[it]>.0001f})
    }
    fun sample(rgb:List<Float>):List<Float> {
        validate();require(rgb.size==3)
        val c=rgb.indices.map {((rgb[it]-domainMin[it])/(domainMax[it]-domainMin[it])).coerceIn(0f,1f)*(size-1)}
        return List(3) {channel->var value=0f
            for(b in 0..1) for(g in 0..1) for(r in 0..1) {
                val bits=listOf(r,g,b);val indices=c.indices.map {(floor(c[it]).toInt()+bits[it]).coerceAtMost(size-1)}
                val weight=c.indices.map {val fraction=c[it]-floor(c[it]);if(bits[it]==1) fraction else 1-fraction}.reduce(Float::times)
                value+=values[(indices[0]+indices[1]*size+indices[2]*size*size)*3+channel]*weight
            };value}
    }
}
object CubeParser {
    fun parse(text:String):CubeLut {
        require(text.length<=4*1024*1024) {"LUT maior que 4 MB."}
        var name="LUT importado";var size=0;var min=List(3) {0f};var max=List(3) {1f};val data=mutableListOf<Float>()
        for(raw in text.lineSequence()) {
            val line=raw.substringBefore('#').trim();if(line.isEmpty()) continue
            val parts=line.split(Regex("\\s+"))
            when(parts[0]) {
                "TITLE"->name=line.removePrefix("TITLE").trim().trim('"').take(100).ifEmpty {"LUT importado"}
                "LUT_3D_SIZE"->{require(size==0 && parts.size==2);size=parts[1].toInt();require(size in 2..32) {"Use um LUT 3D de 2 a 32 pontos por lado."}}
                "DOMAIN_MIN"->{require(parts.size==4);min=parts.drop(1).map(String::toFloat)}
                "DOMAIN_MAX"->{require(parts.size==4);max=parts.drop(1).map(String::toFloat)}
                "LUT_1D_SIZE"->error("Este importador aceita LUT 3D .cube. Curvas 1D estão no editor de curvas.")
                else->{require(size>0 && parts.size==3 && data.size<size*size*size*3) {"Linha inválida no LUT."};data.addAll(parts.map(String::toFloat))}
            }
        }
        return CubeLut(name,size,data,min,max).also {it.validate()}
    }
}

@Serializable data class GradientSpec(val colors:List<Int> = listOf(-15724528,-5207563),val angle:Float=90f) {
    fun validate() {require(colors.size in 2..8 && angle.isFinite() && angle in -360f..360f)}
}
@Serializable data class DrawingStroke(val points:List<Point>,val radius:Float=.01f,val color:Int=-1,val opacity:Float=1f,val erase:Boolean=false) {
    fun validate() {require(points.size in 1..2048 && points.all {it.x.isFinite() && it.y.isFinite() && it.x in 0f..1f && it.y in 0f..1f});require(radius.isFinite() && radius in .001f.. .2f && opacity.isFinite() && opacity in 0f..1f)}
}
@Serializable data class HistorySnapshot(val undo:List<EditState> = emptyList(),val redo:List<EditState> = emptyList()) {
    fun validate() {require(undo.size<=20 && redo.size<=20);(undo+redo).forEach {it.validate()}}
}
@Serializable data class LocalAsset(val meta:AssetMeta,val kind:String,val asset:String) {
    fun validate() {meta.validate();require(kind in setOf("font","lut","sticker","texture","frame"));require(asset.matches(Regex("${if(kind=="font") "font" else if(kind=="lut") "lut" else "photo"}_[a-f0-9-]{36}")))}
}

object EffectTypes {
    val all=listOf("grain","bloom","blur","chromatic","leak","dust","frame","fisheye","wide","barrel","lensdirt","flare","halation","gateweave","flicker","vhs","crt","rgbshift","pixel","glitch","scanlines","digitalnoise","compression","datamosh","glow","neon","softlight","flash","overexposure","coloredlight","prism","rainbow","motionblur","zoomblur","radialblur","directionalblur","shake","paper","canvas","plastic","glass","metal","photocopy","newspaper","rain","stars","hearts","scribble","tape","timestamp","texture","polaroid","filmstrip","disposable","camcorder","digicam","magazineframe","paperframe","scrapbookframe")
}
