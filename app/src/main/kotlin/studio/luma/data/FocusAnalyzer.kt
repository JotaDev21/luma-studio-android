package studio.luma.data

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.luma.core.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/** Both models are bundled. No recognition, tracking or network request is used. */
class FocusAnalyzer {
    suspend fun analyze(source:Bitmap,personMask:RasterMask?=null):PhotoFocus=withContext(Dispatchers.Default) {
        val detector=FaceDetection.getClient(FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).setMinFaceSize(.05f).build())
        val faces=try {
            suspendCoroutine<List<Face>> {continuation->
                detector.process(InputImage.fromBitmap(source,0))
                    .addOnSuccessListener {continuation.resume(it)}
                    .addOnFailureListener {continuation.resumeWithException(IllegalStateException("Não foi possível analisar os rostos. Tente novamente.",it))}
            }.map {face->val b=face.boundingBox
                val l=(b.left.toFloat()/source.width).coerceIn(0f,.99f);val t=(b.top.toFloat()/source.height).coerceIn(0f,.99f)
                Crop(l,t,(b.right.toFloat()/source.width).coerceIn(l+.01f,1f),(b.bottom.toFloat()/source.height).coerceIn(t+.01f,1f))}
        } finally {detector.close()}
        if(faces.isNotEmpty()) {
            return@withContext PhotoFocus(source.width,source.height,faces,Point(faces.map {(it.left+it.right)/2}.average().toFloat(),faces.map {(it.top+it.bottom)/2}.average().toFloat()))
        }
        val mask=personMask ?: PeopleSegmenter().detect(source,requirePerson=false)
        val alpha=mask.decode();var mass=0.0;var x=0.0;var y=0.0;var count=0
        alpha.forEachIndexed {i,byte->val value=byte.toInt() and 255
            if(value>=160) {count++;mass+=value;x+=((i%mask.width+.5)/mask.width)*value;y+=((i/mask.width+.5)/mask.height)*value}}
        val person=count>=maxOf(16,(alpha.size*.003).toInt())
        PhotoFocus(source.width,source.height,center=if(person) Point((x/mass).toFloat(),(y/mass).toFloat()) else Point(.5f,.5f),person=person)
    }
}
