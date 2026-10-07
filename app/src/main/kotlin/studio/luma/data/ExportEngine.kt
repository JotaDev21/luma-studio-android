package studio.luma.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.luma.core.Project
import studio.luma.core.ExportSize
import studio.luma.renderer.GpuRenderer
import studio.luma.renderer.ImageSource
import studio.luma.renderer.StoryRenderer
import kotlin.math.roundToInt

enum class ExportFormat(val mime: String, val extension: String) { JPEG("image/jpeg", "jpg"), PNG("image/png", "png"), WEBP("image/webp", "webp") }
data class ExportOptions(val format: ExportFormat = ExportFormat.JPEG, val quality: Int = 95, val maxEdge: Int = 0, val metadata: Boolean = false,val width:Int=0,val height:Int=0)

class ExportEngine(private val context: Context, private val store: StudioStore, private val renderer: GpuRenderer, private val stories:StoryRenderer=StoryRenderer(renderer)) {
    suspend fun export(project: Project, options: ExportOptions): Uri = withContext(Dispatchers.IO) {
        require(options.quality in 1..100 && options.maxEdge in 0..8192)
        require(options.width==0 && options.height==0 || options.width in 64..8192 && options.height in 64..8192 && options.width.toLong()*options.height<=32_000_000) {"Dimensões inválidas: máximo 32 MP."}
        val targetSize=if(options.width==0) null else ExportSize(options.width,options.height)
        val renderEdge=targetSize?.longEdge ?: options.maxEdge
        val original = store.source(project)
        val story=project.edit.story
        var bitmap=if(story!=null) stories.render(story,store.directory(project.id),project.edit,targetSize?.let {ExportSize(story.width,story.height).fittedLongEdge(it)} ?: renderEdge,true) else {
        val dims = ImageSource.dimensions(original)
        val crop = project.edit.transform.crop
        val croppedMax = maxOf(dims.first * (crop.right - crop.left), dims.second * (crop.bottom - crop.top))
        val sourceEdge = if (renderEdge == 0) 0 else (maxOf(dims.first, dims.second) * minOf(1f, renderEdge / croppedMax)).roundToInt().coerceAtLeast(1)
        val decoded = ImageSource.decode(original, sourceEdge)
        try { renderer.render(decoded, project.edit, renderEdge,store.directory(project.id)) } finally { decoded.recycle() }
        }
        var result: Uri? = null
        try {
            val outputSize=targetSize ?: ExportSize(bitmap.width,bitmap.height).atLongEdge(options.maxEdge)
            require(outputSize.width in 1..8192 && outputSize.height in 1..8192 && outputSize.width.toLong()*outputSize.height<=32_000_000) {"Dimensões inválidas: máximo 32 MP e 8192 px por lado."}
            if(bitmap.width!=outputSize.width || bitmap.height!=outputSize.height) {
                require(bitmap.allocationByteCount.toLong()+outputSize.width.toLong()*outputSize.height*8<Runtime.getRuntime().maxMemory()*.75) {"Memória insuficiente. Escolha dimensões menores."}
                val resized=Bitmap.createBitmap(outputSize.width,outputSize.height,Bitmap.Config.ARGB_8888)
                try {
                    val factor=minOf(outputSize.width.toFloat()/bitmap.width,outputSize.height.toFloat()/bitmap.height)
                    val dw=bitmap.width*factor;val dh=bitmap.height*factor
                    Canvas(resized).drawBitmap(bitmap,null,android.graphics.RectF((outputSize.width-dw)/2,(outputSize.height-dh)/2,(outputSize.width+dw)/2,(outputSize.height+dh)/2),android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
                } catch(e:Throwable) {resized.recycle();throw e}
                bitmap.recycle();bitmap=resized
            }
            val filename = "Luma_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS", java.util.Locale.ROOT).format(java.util.Date())}.${options.format.extension}"
            result = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename); put(MediaStore.Images.Media.MIME_TYPE, options.format.mime)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Luma Studio"); put(MediaStore.Images.Media.IS_PENDING, 1)
            }) ?: error("Não foi possível criar o arquivo na galeria.")
            val format = when (options.format) {
                ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
                ExportFormat.PNG -> Bitmap.CompressFormat.PNG
                ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= 30) { if (options.quality == 100) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP_LOSSY } else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
            }
            val flattened=if(options.format==ExportFormat.JPEG && bitmap.hasAlpha()) Bitmap.createBitmap(bitmap.width,bitmap.height,Bitmap.Config.ARGB_8888).apply {
                Canvas(this).apply {drawColor(Color.WHITE);drawBitmap(bitmap,0f,0f,null)}
            } else null
            try {context.contentResolver.openOutputStream(result)?.use {check((flattened ?: bitmap).compress(format,options.quality,it)) {"Falha ao codificar a imagem."}} ?: error("Falha ao gravar o arquivo.")}
            finally {flattened?.recycle()}
            if (options.metadata && options.format == ExportFormat.JPEG && project.edit.story==null) {
                val srcExif = ExifInterface(original)
                context.contentResolver.openFileDescriptor(result, "rw")?.use { descriptor ->
                    val outExif = ExifInterface(descriptor.fileDescriptor)
                    listOf(ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_DATETIME_ORIGINAL,
                        ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, ExifInterface.TAG_FOCAL_LENGTH)
                        .forEach { tag -> srcExif.getAttribute(tag)?.let { outExif.setAttribute(tag, it) } }
                    outExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString()); outExif.saveAttributes()
                }
            }
            context.contentResolver.update(result, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            result
        } catch (e: Exception) { result?.let { context.contentResolver.delete(it, null, null) }; throw e }
        finally { bitmap.recycle() }
    }
}
