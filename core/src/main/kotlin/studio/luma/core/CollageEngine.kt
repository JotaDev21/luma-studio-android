package studio.luma.core

import kotlinx.serialization.Serializable
import kotlin.math.*

@Serializable data class CollageSettings(val layout:String="grid",val count:Int=4,val spacing:Float=24f,val border:Float=0f,val cornerRadius:Float=0f,val shadow:Float=0f,val rotation:Float=6f,val borderColor:Int=-1) {
    fun validate() {require(layout in layouts && count in 2..20);require(listOf(spacing,border,cornerRadius,shadow,rotation).all {it.isFinite()});require(spacing in 0f..100f && border in 0f..100f && cornerRadius in 0f..200f && shadow in 0f..60f && rotation in 0f..30f)}
    companion object {val layouts=listOf("grid","freeform","photo-dump","magazine","polaroid","scrapbook","film-strip")}
}

/** Reflows photos while retaining their IDs, source assets, crops and non-photo layers. */
object CollageEngine {
    fun create(settings:CollageSettings,width:Int=1080,height:Int=1920):StoryDocument {
        settings.validate()
        val photos=List(settings.count) {StoryLayer("collage_photo_$it","photo",LayerTransform(0f,0f,100f,100f),placeholder=true)}
        return reflow(StoryDocument(width,height,layers=photos,collage=settings),settings)
    }
    fun reflow(document:StoryDocument,settings:CollageSettings):StoryDocument {
        document.validate();settings.validate()
        val photos=document.layers.filter {it.type=="photo"};require(photos.size in 2..20) {"Colagens precisam de 2 a 20 fotos."}
        val count=photos.size;val w=document.width.toFloat();val h=document.height.toFloat()
        val gap=settings.spacing*min(w,h)/1080f;val margin=max(gap,12f*min(w,h)/1080f)
        val columns=ceil(sqrt(count*w/h)).toInt().coerceIn(1,count);val rows=ceil(count.toFloat()/columns).toInt()
        fun grid(index:Int,n:Int,x:Float,y:Float,gw:Float,gh:Float,cols:Int):LayerTransform {
            val r=ceil(n.toFloat()/cols).toInt();val cw=((gw-gap*(cols-1))/cols).coerceAtLeast(8f);val ch=((gh-gap*(r-1))/r).coerceAtLeast(8f)
            return LayerTransform(x+(index%cols)*(cw+gap),y+(index/cols)*(ch+gap),cw,ch)
        }
        val changed=photos.mapIndexed {i,layer ->
            if(layer.locked) return@mapIndexed layer
            val base=when(settings.layout) {
                "magazine"->if(i==0) LayerTransform(margin,margin,w-2*margin,(h-3*margin)*.55f) else grid(i-1,count-1,margin,margin*2+(h-3*margin)*.55f,w-2*margin,(h-3*margin)*.45f,min(2,count-1))
                "film-strip"->grid(i,count,w*.14f,margin,w*.72f,h-2*margin,1)
                "freeform","photo-dump","scrapbook"->{val bw=w*.65f;val bh=min(h*.38f,h/(ceil(count/2f)+.5f));val x=if(i%2==0) margin else w-bw-margin;val y=margin+(i/2)*(h-bh-2*margin)/max(1,(count-1)/2);LayerTransform(x,y,bw,bh)}
                else->grid(i,count,margin,margin,w-2*margin,h-2*margin,columns)
            }
            val angle=if(settings.layout in listOf("freeform","photo-dump","polaroid","scrapbook")) settings.rotation*(if(i%2==0) -1 else 1) else 0f
            val border=if(settings.layout=="polaroid") max(settings.border,20f) else settings.border
            layer.copy(transform=base.copy(rotation=angle,opacity=layer.transform.opacity),appearance=LayerAppearance(settings.cornerRadius,border,if(settings.layout=="polaroid") border*3 else border,settings.borderColor,settings.shadow))
        }.associateBy {it.id}
        return document.copy(layers=document.layers.map {changed[it.id] ?: it},collage=settings.copy(count=count)).also {it.validate()}
    }
}
