package studio.luma.data

import android.graphics.Bitmap
import android.graphics.Color
import studio.luma.core.*
import kotlin.math.*

/** Bounded statistical suggestion. No network, object recognition or claimed preset recovery. */
object ReferenceMatcher {
    fun match(source:Bitmap,reference:Bitmap):ColorTreatment {
        fun statistics(bitmap:Bitmap):Pair<DoubleArray,DoubleArray> {
            val sums=DoubleArray(3);val squares=DoubleArray(3);var n=0
            val step=max(1,max(bitmap.width,bitmap.height)/128)
            for(y in 0 until bitmap.height step step) for(x in 0 until bitmap.width step step) {
                val p=bitmap.getPixel(x,y);if(Color.alpha(p)<128) continue
                val channels=intArrayOf(Color.red(p),Color.green(p),Color.blue(p))
                for(c in 0..2) {val v=channels[c]/255.0;sums[c]+=v;squares[c]+=v*v};n++
            }
            require(n>0) {"A referência precisa conter pixels visíveis."}
            val mean=DoubleArray(3) {sums[it]/n};val deviation=DoubleArray(3) {sqrt(max(.0025,squares[it]/n-mean[it]*mean[it]))};return mean to deviation
        }
        val (sm,sd)=statistics(source);val (rm,rd)=statistics(reference)
        val matrix=MutableList(12) {0f}
        for(c in 0..2) {val gain=(rd[c]/sd[c]).coerceIn(.65,1.55);matrix[c*4+c]=gain.toFloat();matrix[c*4+3]=(rm[c]-sm[c]*gain).coerceIn(-.25,.25).toFloat()}
        return ColorTreatment(matrix=matrix,rolloff=.08f)
    }
}
