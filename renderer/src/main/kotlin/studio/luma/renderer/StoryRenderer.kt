package studio.luma.renderer

import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import studio.luma.core.*
import java.io.File
import kotlin.math.*

/** Composition is shared by manual Stories, templates, previews and export. */
class StoryRenderer(private val gpu:GpuRenderer) {
    private data class CacheKey(val asset:String,val edge:Int,val edit:EditState)
    private val mutex=Mutex()
    private val cache=linkedMapOf<CacheKey,Bitmap>()
    suspend fun render(document:StoryDocument,directory:File,edit:EditState=EditState(),maxEdge:Int=1280,export:Boolean=false):Bitmap=mutex.withLock { withContext(Dispatchers.Default) {
        document.validate()
        require(!export || document.missingPhotos==0) { "Preencha as ${document.missingPhotos} fotos do template antes de exportar." }
        val scale=if(maxEdge==0) 1f else if(export) maxEdge.toFloat()/maxOf(document.width,document.height) else minOf(1f,maxEdge.toFloat()/maxOf(document.width,document.height))
        val w=(document.width*scale).roundToInt().coerceAtLeast(1); val h=(document.height*scale).roundToInt().coerceAtLeast(1)
        require(w.toLong()*h*12<Runtime.getRuntime().maxMemory()*.7) { "Memória insuficiente. Escolha uma resolução menor." }
        val result=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        try {
            val canvas=Canvas(result); canvas.drawColor(document.background); canvas.scale(scale,scale)
            document.gradient?.let {canvas.drawRect(0f,0f,document.width.toFloat(),document.height.toFloat(),Paint().apply {shader=gradient(it,document.width.toFloat(),document.height.toFloat())})}
            document.backgroundAsset?.let {name ->
                val original=ImageSource.decode(File(directory,name),maxOf(w,h))
                val blur=EffectLayer("background_blur",EffectPreset(meta=AssetMeta("background_blur","Fundo desfocado","Light"),type="blur",parameters=mapOf("radius" to document.backgroundBlur)),document.backgroundBlur)
                val background=try {gpu.render(original,EditState(effects=listOf(blur)),directory=directory)} finally {original.recycle()}
                try {val f=max(document.width.toFloat()/background.width,document.height.toFloat()/background.height);val bw=background.width*f;val bh=background.height*f;canvas.drawBitmap(background,null,RectF((document.width-bw)/2,(document.height-bh)/2,(document.width+bw)/2,(document.height+bh)/2),Paint(Paint.FILTER_BITMAP_FLAG))} finally {background.recycle()}
            }
            for(layer in document.layers.filter { it.visible }) {
                val t=layer.transform; val save=canvas.save()
                canvas.translate(t.x+t.width/2,t.y+t.height/2); canvas.rotate(t.rotation); canvas.translate(-t.width/2,-t.height/2)
                val blend=when(layer.blendMode) { "multiply"->BlendMode.MULTIPLY; "screen"->BlendMode.SCREEN; "overlay"->BlendMode.OVERLAY; else->BlendMode.SRC_OVER }
                val layerPaint=Paint().apply { alpha=(t.opacity*255).roundToInt(); blendMode=blend }
                val padding=if(layer.type=="photo") layer.appearance.shadow*3 else 0f
                val group=canvas.saveLayer(-padding,-padding,t.width+padding,t.height+padding,layerPaint)
                if(layer.type!="photo") canvas.clipRect(0f,0f,t.width,t.height)
                when(layer.type) {
                    "photo" -> {
                        val appearance=layer.appearance
                        val border=min(appearance.border,min(t.width,t.height)*.2f);val bottom=min(max(border,appearance.bottomBorder),t.height*.4f)
                        val radius=min(appearance.cornerRadius,min(t.width,t.height)/2)
                        if(border>0f || bottom>0f || appearance.shadow>0f) {
                            val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=appearance.borderColor;if(appearance.shadow>0f) setShadowLayer(appearance.shadow,0f,appearance.shadow*.25f,appearance.shadowColor)}
                            canvas.drawRoundRect(RectF(0f,0f,t.width,t.height),radius,radius,paint)
                        }
                        val content=canvas.save();canvas.translate(border,border)
                        val pw=(t.width-border*2).coerceAtLeast(1f);val ph=(t.height-border-bottom).coerceAtLeast(1f)
                        val innerRadius=max(0f,radius-border)
                        canvas.clipPath(Path().apply {addRoundRect(RectF(0f,0f,pw,ph),innerRadius,innerRadius,Path.Direction.CW)})
                        if(layer.asset==null) placeholder(canvas,pw,ph) else {
                            val file=File(directory,requireNotNull(layer.asset))
                            val dimensions=ImageSource.dimensions(file)
                            val crop=layer.photoTransform.crop
                            val cw=dimensions.first*(crop.right-crop.left);val ch=dimensions.second*(crop.bottom-crop.top)
                            val photoWidth=if(layer.photoTransform.quarterTurns%2==0) cw else ch;val photoHeight=if(layer.photoTransform.quarterTurns%2==0) ch else cw
                            val needed=(if(layer.photoFit=="contain") min(pw/photoWidth,ph/photoHeight) else max(pw/photoWidth,ph/photoHeight))*scale*layer.photoZoom
                            val edge=maxOf(64,(maxOf(dimensions.first,dimensions.second)*min(1f,needed)).roundToInt()).coerceAtMost(8192)
                            val photoEdit=edit.copy(transform=layer.photoTransform,story=null,masks=layer.masks)
                            val key=CacheKey(file.path,edge,photoEdit)
                            val photo=if(!export) cache[key] else null
                            val bitmap=photo ?: run {
                                val original=ImageSource.decode(file,edge)
                                try { gpu.render(original,photoEdit,directory=directory) } finally { original.recycle() }
                            }
                            val factor=(if(layer.photoFit=="contain") min(pw/bitmap.width,ph/bitmap.height) else max(pw/bitmap.width,ph/bitmap.height))*layer.photoZoom
                            val dw=bitmap.width*factor; val dh=bitmap.height*factor
                            val x=(pw-dw)*(.5f+layer.photoPanX*.5f); val y=(ph-dh)*(.5f+layer.photoPanY*.5f)
                            canvas.drawBitmap(bitmap,null,RectF(x,y,x+dw,y+dh),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                            if(export) bitmap.recycle() else if(photo==null) {
                                cache[key]=bitmap
                                while(cache.size>8 || cache.values.sumOf { it.allocationByteCount.toLong() }>48_000_000L) {
                                    val first=cache.entries.first(); cache.remove(first.key); first.value.recycle()
                                }
                            }
                        }
                        canvas.restoreToCount(content)
                    }
                    "text","sticker" -> drawText(canvas,layer,directory)
                    "drawing" -> {
                        for(stroke in layer.strokes) {
                            val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=stroke.color;alpha=(Color.alpha(stroke.color)*stroke.opacity).roundToInt();strokeWidth=stroke.radius*min(t.width,t.height)*2;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND;style=Paint.Style.STROKE;if(stroke.erase) blendMode=BlendMode.CLEAR}
                            if(stroke.points.size==1) {paint.style=Paint.Style.FILL;canvas.drawCircle(stroke.points[0].x*t.width,stroke.points[0].y*t.height,paint.strokeWidth/2,paint)} else canvas.drawPath(Path().apply {stroke.points.forEachIndexed {i,p->if(i==0) moveTo(p.x*t.width,p.y*t.height) else lineTo(p.x*t.width,p.y*t.height)}},paint)
                        }
                    }
                    "shape" -> {
                        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=layer.color; strokeWidth=max(3f,t.height*.08f);layer.gradient?.let {shader=gradient(it,t.width,t.height)} }
                        when(layer.shape) {
                            "circle"->canvas.drawOval(RectF(0f,0f,t.width,t.height),paint)
                            "line"->canvas.drawLine(0f,t.height/2,t.width,t.height/2,paint)
                            "arrow"->{ canvas.drawLine(0f,t.height/2,t.width*.9f,t.height/2,paint); canvas.drawPath(Path().apply { moveTo(t.width*.7f,0f); lineTo(t.width,t.height/2); lineTo(t.width*.7f,t.height); close() },paint) }
                            else->canvas.drawRoundRect(RectF(0f,0f,t.width,t.height),12f,12f,paint)
                        }
                    }
                }
                canvas.restoreToCount(group); canvas.restoreToCount(save)
            }
            result
        } catch(e:Throwable) { result.recycle(); throw e }
    } }
    private fun gradient(spec:GradientSpec,w:Float,h:Float):Shader {
        val a=spec.angle*PI.toFloat()/180;val dx=cos(a)*w/2;val dy=sin(a)*h/2
        return LinearGradient(w/2-dx,h/2-dy,w/2+dx,h/2+dy,spec.colors.toIntArray(),null,Shader.TileMode.CLAMP)
    }
    private fun drawText(canvas:Canvas,layer:StoryLayer,directory:File) {
        val s=layer.typography; val t=layer.transform
        if(Color.alpha(s.background)>0) canvas.drawRoundRect(RectF(0f,0f,t.width,t.height),12f,12f,Paint().apply { color=s.background })
        val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize=s.size; typeface=Typeface.create(Typeface.create(s.family,Typeface.NORMAL),s.weight,false)
            s.fontAsset?.let {name->typeface=Typeface.create(Typeface.createFromFile(File(directory,name)),s.weight,false)}
            color=s.color; letterSpacing=s.spacing
            s.gradient?.let {shader=gradient(it,t.width,t.height)}
            if(s.glow>0f) setShadowLayer(s.glow,0f,0f,s.color)
            if(s.blur>0f) maskFilter=BlurMaskFilter(s.blur,BlurMaskFilter.Blur.NORMAL)
            if(s.shadow>0f) setShadowLayer(s.shadow,0f,s.shadow*.25f,Color.BLACK)
        }
        if(kotlin.math.abs(s.curvature)>.01f) {
            val path=Path().apply {moveTo(0f,t.height*.5f);quadTo(t.width/2,t.height*(.5f-s.curvature*.45f),t.width,t.height*.5f)}
            paint.textAlign=Paint.Align.CENTER;canvas.drawTextOnPath(layer.content.replace("\n"," "),path,t.width/2,0f,paint);return
        }
        val align=when(s.alignment) { "left"->Layout.Alignment.ALIGN_NORMAL; "right"->Layout.Alignment.ALIGN_OPPOSITE; else->Layout.Alignment.ALIGN_CENTER }
        val layout=StaticLayout.Builder.obtain(layer.content,0,layer.content.length,paint,t.width.toInt().coerceAtLeast(1))
            .setAlignment(align).setIncludePad(false).setLineSpacing(s.size*(s.lineSpacing-1f),1f).build()
        if(s.stroke>0f) { paint.style=Paint.Style.STROKE; paint.strokeWidth=s.stroke; paint.color=s.strokeColor; layout.draw(canvas); paint.style=Paint.Style.FILL; paint.color=s.color }
        layout.draw(canvas)
    }
    private fun placeholder(canvas:Canvas,w:Float,h:Float) {
        canvas.drawColor(Color.rgb(62,65,61))
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.rgb(137,147,123); style=Paint.Style.STROKE; strokeWidth=2f }
        for(i in -8..16) canvas.drawLine(i*w/8,0f,i*w/8+h,h,paint)
        paint.style=Paint.Style.FILL; paint.color=Color.WHITE; paint.textAlign=Paint.Align.CENTER; paint.textSize=min(w,h)*.12f
        canvas.drawText("FOTO",w/2,h/2,paint)
    }
    suspend fun close()=mutex.withLock { cache.values.forEach { it.recycle() }; cache.clear() }
}
