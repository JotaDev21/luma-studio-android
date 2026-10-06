package studio.luma.core

import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater

/** Small source-aligned confidence map. Bounded zlib data lives with the editable project. */
@Serializable data class RasterMask(val width:Int,val height:Int,val data:String) {
    fun decode():ByteArray {
        require(width in 1..256 && height in 1..256 && data.length in 1..100_000) {"Máscara automática inválida."}
        val packed=Base64.getDecoder().decode(data)
        val inflater=Inflater()
        try {
            inflater.setInput(packed)
            val expected=width*height;val result=ByteArray(expected+1);var count=0
            while(!inflater.finished() && count<result.size) {
                val n=inflater.inflate(result,count,result.size-count)
                require(n>0 || inflater.finished()) {"Máscara automática incompleta."};count+=n
            }
            require(inflater.finished() && count==expected && inflater.remaining==0) {"Tamanho da máscara automática inválido."}
            return result.copyOf(expected)
        } catch(e:java.util.zip.DataFormatException) {throw IllegalArgumentException("Máscara automática corrompida.",e)} finally {inflater.end()}
    }
    fun validate() {decode()}
    companion object {
        fun fromAlpha(width:Int,height:Int,alpha:ByteArray):RasterMask {
            require(width in 1..256 && height in 1..256 && alpha.size==width*height)
            val bytes=ByteArrayOutputStream()
            DeflaterOutputStream(bytes).use {it.write(alpha)}
            return RasterMask(width,height,Base64.getEncoder().encodeToString(bytes.toByteArray()))
        }
    }
}
