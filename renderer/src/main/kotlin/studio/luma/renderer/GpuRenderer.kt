package studio.luma.renderer

import android.graphics.Bitmap
import java.io.File
import android.opengl.EGL14
import android.opengl.GLES20.*
import android.opengl.GLUtils
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import studio.luma.core.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/** All EGL/GL calls, including destruction, are confined to this dedicated dispatcher. */
class GpuRenderer {
    private val dispatcher = Executors.newSingleThreadExecutor { r -> Thread(r, "Luma-GPU") }.asCoroutineDispatcher()
    private var display = EGL14.EGL_NO_DISPLAY
    private var context = EGL14.EGL_NO_CONTEXT
    private var surface = EGL14.EGL_NO_SURFACE
    private var program = 0
    private val textures = IntArray(3)
    private var framebuffer = 0
    private var lastSource: Bitmap? = null
    private var closed = false
    private val effects = GpuEffects()
    private val masks = GpuLocalMasks()
    private val color = GpuAdvancedColor()
    private val vertices = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)); position(0)
    }

    suspend fun render(source: Bitmap, state: EditState, maxEdge: Int = 0, directory:File?=null): Bitmap = withContext(dispatcher) {
        check(!closed); state.validate(); initialize()
        val crop = state.transform.crop
        val cropPixels = (source.width * (crop.right - crop.left)).toLong().coerceAtLeast(1) * (source.height * (crop.bottom - crop.top)).toLong().coerceAtLeast(1)
        val outputPixels = if (maxEdge > 0) minOf(cropPixels, maxEdge.toLong() * maxEdge) else cropPixels
        val estimatedBytes = source.width.toLong() * source.height * 8 + outputPixels * (8 + (if ((state.effects+(state.filter?.preset?.effects ?: emptyList())).any { it.enabled && it.intensity > 0f }) 12 else 0) + (if(state.masks.any {it.enabled && it.strength>0f}) 28 else 0))
        require(estimatedBytes < Runtime.getRuntime().maxMemory() * .85) { "Memória insuficiente para esta resolução. Escolha um tamanho menor." }
        var transformed: Bitmap? = null
        try {
            transformed = ImageSource.transform(source, state.transform)
            val image = transformed
            val scale = if (maxEdge > 0) minOf(1f, maxEdge.toFloat() / maxOf(image.width, image.height)) else 1f
            val w = (image.width * scale).roundToInt().coerceAtLeast(1)
            val h = (image.height * scale).roundToInt().coerceAtLeast(1)
            val maxTexture = IntArray(1); glGetIntegerv(GL_MAX_TEXTURE_SIZE, maxTexture, 0)
            require(maxOf(image.width, image.height, w, h) <= maxTexture[0]) { "Esta GPU aceita até ${maxTexture[0]} px por lado. Escolha uma resolução menor." }
            glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, textures[0])
            if (lastSource !== image) { GLUtils.texImage2D(GL_TEXTURE_2D, 0, image, 0); lastSource = if (image === source) source else null }
            glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, textures[1])
            // Color treatment is bound after selecting the program.
            glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_2D, textures[2])
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, null)
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textures[2], 0)
            check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE) { "GPU sem memória para esta resolução." }
            glViewport(0, 0, w, h); glUseProgram(program)
            glUniform1i(uniform("uImage"), 0)
            color.bind(program,state.color,state.filter?.preset)
            glUniform2f(uniform("uPixel"),1f/image.width,1f/image.height)
            val da=state.adjustments;val df=state.filter?.preset?.adjustments ?: Adjustments();val fi=state.filter?.intensity ?: 0f
            glUniform1fv(uniform("uDetail[0]"),5,floatArrayOf((da.clarity+df.clarity*fi).coerceIn(-1f,1f),(da.texture+df.texture*fi).coerceIn(-1f,1f),(da.sharpness+df.sharpness*fi).coerceIn(0f,2f),(da.dehaze+df.dehaze*fi).coerceIn(-1f,1f),(da.denoise+df.denoise*fi).coerceIn(0f,1f)),0)
            glUniform1fv(uniform("uBase[0]"), 14, state.adjustments.uniforms(), 0)
            glUniform1fv(uniform("uFilter[0]"), 14, (state.filter?.preset?.adjustments ?: Adjustments()).uniforms(), 0)
            glUniform1f(uniform("uIntensity"), state.filter?.intensity ?: 0f)
            val preset = state.filter?.preset
            glUniform3fv(uniform("uShadow"), 1, (preset?.shadowTint ?: listOf(0f, 0f, 0f)).toFloatArray(), 0)
            glUniform3fv(uniform("uHighlight"), 1, (preset?.highlightTint ?: listOf(0f, 0f, 0f)).toFloatArray(), 0)
            glUniform1f(uniform("uGrading"), preset?.grading ?: 0f)
            val position = glGetAttribLocation(program, "aPosition")
            vertices.position(0); glEnableVertexAttribArray(position); glVertexAttribPointer(position, 2, GL_FLOAT, false, 0, vertices)
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
            val masked=masks.render(textures[2],w,h,source.width,source.height,state,framebuffer,vertices,directory)
            effects.render(masked,w,h,(state.filter?.preset?.effects ?: emptyList()).map {it.copy(intensity=it.intensity*(state.filter?.intensity ?: 0f))}+state.effects,framebuffer,vertices,directory)
            val buffer = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
            glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
            val error = glGetError(); check(error == GL_NO_ERROR) { "Falha na GPU (código $error)." }
            buffer.position(0)
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { copyPixelsFromBuffer(buffer) }
        } finally { if (transformed !== source) transformed?.recycle() }
    }

    private fun uniform(name: String) = glGetUniformLocation(program, name)
    private fun initialize() {
        if (program != 0) return
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(EGL14.eglInitialize(display, IntArray(2), 0, IntArray(2), 0)) { "EGL indisponível." }
        val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
        val attrs = intArrayOf(EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8, EGL14.EGL_NONE)
        val count = IntArray(1)
        check(EGL14.eglChooseConfig(display, attrs, 0, configs, 0, 1, count, 0) && count[0] > 0)
        context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
        surface = EGL14.eglCreatePbufferSurface(display, configs[0], intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0)
        check(EGL14.eglMakeCurrent(display, surface, surface, context)) { "Não foi possível iniciar a GPU." }
        val vertex = compile(GL_VERTEX_SHADER, "attribute vec2 aPosition; varying vec2 vUV; void main(){gl_Position=vec4(aPosition,0.0,1.0);vUV=(aPosition+1.0)*0.5;}")
        val fragment = compile(GL_FRAGMENT_SHADER, FRAGMENT)
        program = glCreateProgram(); glAttachShader(program, vertex); glAttachShader(program, fragment); glLinkProgram(program)
        val linked = IntArray(1); glGetProgramiv(program, GL_LINK_STATUS, linked, 0)
        check(linked[0] != 0) { glGetProgramInfoLog(program) }; glDeleteShader(vertex); glDeleteShader(fragment)
        glGenTextures(3, textures, 0)
        textures.forEach { glBindTexture(GL_TEXTURE_2D, it); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE) }
        val ids = IntArray(1); glGenFramebuffers(1, ids, 0); framebuffer = ids[0]
    }
    private fun compile(type: Int, source: String): Int {
        val shader = glCreateShader(type); glShaderSource(shader, source); glCompileShader(shader)
        val result = IntArray(1); glGetShaderiv(shader, GL_COMPILE_STATUS, result, 0)
        check(result[0] != 0) { glGetShaderInfoLog(shader) }; return shader
    }
    private fun uploadCurves(curves: Curves) {
        val buffer = ByteBuffer.allocateDirect(256 * 4)
        for (i in 0..255) {
            val master = sampleCurve(curves.rgb, i / 255f)
            listOf(curves.red, curves.green, curves.blue).forEach { buffer.put((sampleCurve(it, master) * 255).roundToInt().toByte()) }
            buffer.put(255.toByte())
        }
        buffer.position(0); glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 256, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
    }
    suspend fun close() {
        withContext(dispatcher) {
            closed = true
            if (program != 0) { masks.close(); effects.close();color.close(); glDeleteTextures(3, textures, 0); glDeleteFramebuffers(1, intArrayOf(framebuffer), 0); glDeleteProgram(program) }
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(display, surface); EGL14.eglDestroyContext(display, context); EGL14.eglTerminate(display)
            }
            lastSource = null
        }
        dispatcher.close()
    }

    companion object {
        // SDR sRGB working space. Luminance-dependent shadows/highlights and per-channel curves.
        private val FRAGMENT = """
            precision highp float;
            varying vec2 vUV;
            uniform sampler2D uImage;
            uniform vec2 uPixel;uniform float uDetail[5];
            uniform float uBase[14];
            uniform float uFilter[14];
            uniform float uIntensity;
            uniform vec3 uShadow;
            uniform vec3 uHighlight;
            uniform float uGrading;
            ${ColorShader.functions}
            ${GpuAdvancedColor.shader}
            void main(){
                vec4 src = texture2D(uImage,vUV);
                vec3 straight = detail(uImage,vUV,uPixel,uDetail[0],uDetail[1],uDetail[2],uDetail[3],uDetail[4]);
                vec3 base = adjust(straight,uBase[0],uBase[1],uBase[2],uBase[3],uBase[4],uBase[5],uBase[6],uBase[7],uBase[8],uBase[9],uBase[10],uBase[11],uBase[12]);
                base=manualColor(base);
                vec3 f = adjust(base,uFilter[0],uFilter[1],uFilter[2],uFilter[3],uFilter[4],uFilter[5],uFilter[6],uFilter[7],uFilter[8],uFilter[9],uFilter[10],uFilter[11],uFilter[12]);
                f = presetColor(f);
                float l = lum(f);
                f += uGrading*(uShadow*(1.0-smoothstep(0.0,0.6,l))+uHighlight*smoothstep(0.4,1.0,l))*0.22;
                vec3 result = mix(base,clamp(f,0.0,1.0),uIntensity);
                float edge = smoothstep(0.22,0.72,length(vUV-0.5));
                result *= 1.0-edge*(uBase[13]+uFilter[13]*uIntensity)*0.65;
                gl_FragColor = vec4(clamp(result,0.0,1.0)*src.a,src.a);
            }
        """.trimIndent()
    }
}
