package studio.luma.data

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.luma.core.RasterMask
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

/** Bundled on-device model. A single-photo client is closed only after its task finishes. */
class PeopleSegmenter {
    suspend fun detect(source:Bitmap,requirePerson:Boolean=true):RasterMask=withContext(Dispatchers.Default) {
        val client=Segmentation.getClient(SelfieSegmenterOptions.Builder().setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE).enableRawSizeMask().build())
        try {
            val result=suspendCoroutine<SegmentationMask> {continuation->
                client.process(InputImage.fromBitmap(source,0))
                    .addOnSuccessListener {continuation.resume(it)}
                    .addOnFailureListener {continuation.resumeWithException(IllegalStateException("Não foi possível selecionar a pessoa. Tente novamente ou use o pincel.",it))}
            }
            require(result.width in 1..256 && result.height in 1..256) {"O modelo retornou uma máscara incompatível."}
            val buffer=result.buffer.duplicate().order(ByteOrder.nativeOrder());buffer.rewind()
            val alpha=ByteArray(result.width*result.height) {
                val confidence=buffer.float
                require(confidence.isFinite()) {"Resultado de seleção inválido."}
                (confidence.coerceIn(0f,1f)*255).roundToInt().toByte()
            }
            require(!requirePerson || alpha.count {(it.toInt() and 255)>=160}>=maxOf(16,(alpha.size*.003f).roundToInt())) {"Não encontrei uma pessoa com confiança suficiente. Tente um retrato mais nítido ou use o pincel."}
            RasterMask.fromAlpha(result.width,result.height,alpha)
        } finally {client.close()}
    }
}
