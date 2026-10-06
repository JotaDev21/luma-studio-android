package studio.luma.renderer

import android.graphics.*
import studio.luma.core.*
import studio.luma.core.Point
import kotlin.math.*

/** Vector masks stay in source coordinates; rasterization follows the current photo transform. */
object MaskRasterizer {
    fun create(mask:LocalMask,transform:Transform,sourceWidth:Int,sourceHeight:Int,w:Int,h:Int,invert:Boolean=false):Bitmap {
        mask.validate();transform.validate()
        val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        val a=MaskCoordinates.toDisplay(Point(0f,0f),transform)
        val b=MaskCoordinates.toDisplay(Point(1f,0f),transform)
        val c=MaskCoordinates.toDisplay(Point(0f,1f),transform)
        canvas.concat(Matrix().apply {setValues(floatArrayOf((b.x-a.x)*w,(c.x-a.x)*w,a.x*w,(b.y-a.y)*h,(c.y-a.y)*h,a.y*h,0f,0f,1f))})
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        fun dab(x:Float,y:Float,rx:Float,ry:Float,opacity:Float,erase:Boolean) {
            canvas.save();canvas.translate(x,y);canvas.scale(rx,ry)
            paint.shader=RadialGradient(0f,0f,1f,intArrayOf(Color.WHITE,Color.WHITE,Color.TRANSPARENT),floatArrayOf(0f,(1-mask.feather).coerceIn(.001f,.999f),1f),Shader.TileMode.CLAMP)
            paint.alpha=(opacity*255).roundToInt();paint.xfermode=if(erase) PorterDuffXfermode(PorterDuff.Mode.DST_OUT) else null
            canvas.drawCircle(0f,0f,1f,paint);canvas.restore()
        }
        when(mask.type) {
            "radial"->dab(mask.center.x,mask.center.y,mask.radiusX,mask.radiusY,1f,false)
            "linear"->{
                paint.shader=LinearGradient(mask.start.x,mask.start.y,mask.end.x,mask.end.y,intArrayOf(Color.WHITE,Color.WHITE,Color.TRANSPARENT,Color.TRANSPARENT),floatArrayOf(0f,(1-mask.feather.coerceAtLeast(.001f))*.5f,(1+mask.feather.coerceAtLeast(.001f))*.5f,1f),Shader.TileMode.CLAMP)
                canvas.drawPaint(paint)
            }
            "brush","person"->{
                mask.raster?.let {raster->
                    val alpha=raster.decode();val transition=.01f+mask.feather*.49f
                    val pixels=IntArray(alpha.size) {index->
                        val t=(((alpha[index].toInt() and 255)/255f-mask.threshold+transition*.5f)/transition).coerceIn(0f,1f)
                        val coverage=(t*t*(3-2*t)*255).roundToInt()
                        Color.argb(coverage,255,255,255)
                    }
                    val small=Bitmap.createBitmap(pixels,raster.width,raster.height,Bitmap.Config.ARGB_8888)
                    try {canvas.drawBitmap(small,null,RectF(0f,0f,1f,1f),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))} finally {small.recycle()}
                }
                val sx=sourceWidth.toFloat()/min(sourceWidth,sourceHeight);val sy=sourceHeight.toFloat()/min(sourceWidth,sourceHeight)
                for(stroke in mask.strokes) {
                    var previous=stroke.points.first()
                    dab(previous.x,previous.y,stroke.radius/sx,stroke.radius/sy,stroke.opacity,stroke.erase)
                    for(point in stroke.points.drop(1)) {
                        val distance=hypot((point.x-previous.x)*sx,(point.y-previous.y)*sy)
                        val steps=ceil(distance/(stroke.radius*.3f)).toInt().coerceAtLeast(1)
                        for(i in 1..steps) {val t=i.toFloat()/steps;dab(previous.x+(point.x-previous.x)*t,previous.y+(point.y-previous.y)*t,stroke.radius/sx,stroke.radius/sy,stroke.opacity,stroke.erase)}
                        previous=point
                    }
                }
            }
        }
        if(!invert) return bitmap
        val inverse=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        Canvas(inverse).apply {drawColor(Color.WHITE);drawBitmap(bitmap,0f,0f,Paint().apply {xfermode=PorterDuffXfermode(PorterDuff.Mode.DST_OUT)})}
        bitmap.recycle();return inverse
    }
}
