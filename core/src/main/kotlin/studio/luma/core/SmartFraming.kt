package studio.luma.core

import kotlin.math.*

/** Analysis coordinates belong to the immutable, EXIF-oriented source, before edits. */
data class PhotoFocus(val width:Int,val height:Int,val faces:List<Crop> = emptyList(),val center:Point=Point(.5f,.5f),val person:Boolean=false) {
    init {require(width>0 && height>0);faces.forEach {it.validate()};require(center.x.isFinite() && center.y.isFinite() && center.x in 0f..1f && center.y in 0f..1f)}
    fun aspect(transform:Transform)=if(transform.quarterTurns%2==0) width.toFloat()/height else height.toFloat()/width
}

object SmartFraming {
    /** Largest crop at the requested output aspect. Null means detected faces cannot all fit. */
    fun crop(focus:PhotoFocus,outputAspect:Float,transform:Transform=Transform()):Crop? {
        require(outputAspect.isFinite() && outputAspect>0);transform.validate()
        val ratio=if(transform.quarterTurns%2==0) outputAspect else 1f/outputAspect
        val sourceRatio=focus.width.toFloat()/focus.height
        val w=min(1f,ratio/sourceRatio);val h=min(1f,sourceRatio/ratio)
        if(w<.01f || h<.01f) return null
        fun axis(size:Float,center:Float,start:Float?,end:Float?):Float? {
            val low=max(0f,(end ?: 0f)-size);val high=min(1f-size,start ?: 1f)
            if(low>high+1e-6f) return null
            return (center-size/2).coerceIn(low,min(1f-size,max(low,high)))
        }
        // Keep a small headroom margin around every face, including small/background faces.
        val faces=focus.faces.map {f->val mx=(f.right-f.left)*.12f;val my=(f.bottom-f.top)*.12f
            Crop(max(0f,f.left-mx),max(0f,f.top-my),min(1f,f.right+mx),min(1f,f.bottom+my))}
        val x=axis(w,focus.center.x,faces.minOfOrNull {it.left},faces.maxOfOrNull {it.right}) ?: return null
        val y=axis(h,focus.center.y,faces.minOfOrNull {it.top},faces.maxOfOrNull {it.bottom}) ?: return null
        return Crop(x,y,min(1f,x+w),min(1f,y+h)).also {it.validate()}
    }

    fun contentAspect(layer:StoryLayer):Float {
        val t=layer.transform;val a=layer.appearance
        val border=min(a.border,min(t.width,t.height)*.2f);val bottom=min(max(border,a.bottomBorder),t.height*.4f)
        return max(1f,t.width-border*2)/max(1f,t.height-border-bottom)
    }

    /** Keeps masks/source orientation together; zoom and pan reset only in the accepted proposal. */
    fun frame(layer:StoryLayer,focus:PhotoFocus):StoryLayer {
        val crop=crop(focus,contentAspect(layer),layer.photoTransform)
        return layer.copy(photoTransform=layer.photoTransform.copy(crop=crop ?: Crop()),photoFit=if(crop==null) "contain" else "cover",photoZoom=1f,photoPanX=0f,photoPanY=0f)
    }

    /** Assigns complete photo payloads to existing visible/unlocked frames, including empty ones. */
    fun organize(document:StoryDocument,focus:Map<String,PhotoFocus>):StoryDocument {
        document.validate()
        val slots=document.layers.filter {it.type=="photo" && it.visible && !it.locked}
        require(slots.size in 2..20) {"Escolha uma composição com 2 a 20 molduras visíveis e desbloqueadas."}
        require(slots.any {it.asset!=null}) {"Adicione fotos antes de organizar."}
        val costs=slots.map {photo->slots.map {slot->
            if(photo.asset==null) 0.0 else {
                val f=focus.getValue(requireNotNull(photo.asset));val aspect=contentAspect(slot)
                abs(ln((f.aspect(photo.photoTransform)/aspect).toDouble()))+(if(crop(f,aspect,photo.photoTransform)==null) 2.0 else 0.0)
            }
        }.toDoubleArray()}.toTypedArray()
        val assigned=assignment(costs)
        val byId=slots.indices.associate {i->
            val photo=slots[i];val slot=slots[assigned[i]]
            val payload=slot.copy(asset=photo.asset,placeholder=photo.asset==null,photoTransform=photo.photoTransform,masks=photo.masks,photoZoom=photo.photoZoom,photoPanX=photo.photoPanX,photoPanY=photo.photoPanY,photoFit=photo.photoFit)
            slot.id to if(photo.asset==null) payload else frame(payload,focus.getValue(requireNotNull(photo.asset)))
        }
        return document.copy(layers=document.layers.map {byId[it.id] ?: it}).also {it.validate()}
    }

    /** Hungarian assignment: global minimum rather than a greedy choice that strands later photos. */
    private fun assignment(cost:Array<DoubleArray>):IntArray {
        val n=cost.size;val u=DoubleArray(n+1);val v=DoubleArray(n+1);val p=IntArray(n+1);val way=IntArray(n+1)
        for(i in 1..n) {
            p[0]=i;var j0=0;val minv=DoubleArray(n+1) {Double.POSITIVE_INFINITY};val used=BooleanArray(n+1)
            do {
                used[j0]=true;val i0=p[j0];var delta=Double.POSITIVE_INFINITY;var j1=0
                for(j in 1..n) if(!used[j]) {
                    val current=cost[i0-1][j-1]-u[i0]-v[j]
                    if(current<minv[j]) {minv[j]=current;way[j]=j0}
                    if(minv[j]<delta) {delta=minv[j];j1=j}
                }
                for(j in 0..n) if(used[j]) {u[p[j]]+=delta;v[j]-=delta} else minv[j]-=delta
                j0=j1
            } while(p[j0]!=0)
            do {val j1=way[j0];p[j0]=p[j1];j0=j1} while(j0!=0)
        }
        return IntArray(n).also {result->for(j in 1..n) result[p[j]-1]=j-1}
    }
}
