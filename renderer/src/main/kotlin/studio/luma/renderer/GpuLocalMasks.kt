package studio.luma.renderer

import android.opengl.GLES20.*
import android.opengl.GLUtils
import studio.luma.core.*
import java.nio.FloatBuffer
import java.io.File
import android.graphics.Color

internal class GpuLocalMasks {
    private val textures=IntArray(3)
    private var program=0
    private val advanced=GpuAdvancedColor()
    private val effects=GpuEffects()
    private var width=0
    private var height=0
    private var cached:Key?=null
    private data class Key(val geometry:LocalMask,val transform:Transform,val sw:Int,val sh:Int,val w:Int,val h:Int)
    fun render(input:Int,w:Int,h:Int,sw:Int,sh:Int,state:EditState,framebuffer:Int,vertices:FloatBuffer,directory:File?=null):Int {
        val active=state.masks.filter {it.enabled && it.strength>0f}
        if(active.isEmpty()) return input
        initialize()
        if(width!=w || height!=h) {
            textures.take(2).forEach {glBindTexture(GL_TEXTURE_2D,it);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,w,h,0,GL_RGBA,GL_UNSIGNED_BYTE,null)}
            width=w;height=h;cached=null
        }
        var current=input
        for(mask in active) {
            val key=Key(mask.geometry(),state.transform,sw,sh,w,h)
            glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,textures[2])
            if(cached!=key) {
                val raster=MaskRasterizer.create(mask,state.transform,sw,sh,w,h)
                try {GLUtils.texImage2D(GL_TEXTURE_2D,0,raster,0)} finally {raster.recycle()};cached=key
            }
            val filtered=mask.filter
            val treatment=effects.render(current,w,h,(filtered?.preset?.effects?.map {it.copy(intensity=it.intensity*filtered.intensity)} ?: emptyList())+mask.effects,framebuffer,vertices,directory)
            val output=textures.take(2).first {it!=current && it!=treatment}
            glBindFramebuffer(GL_FRAMEBUFFER,framebuffer);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,output,0)
            check(glCheckFramebufferStatus(GL_FRAMEBUFFER)==GL_FRAMEBUFFER_COMPLETE) {"GPU sem memória para as máscaras."}
            glViewport(0,0,w,h);glUseProgram(program)
            glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,current)
            glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,textures[2])
            glActiveTexture(GL_TEXTURE2);glBindTexture(GL_TEXTURE_2D,treatment)
            glUniform1i(u("uImage"),0);glUniform1i(u("uMask"),1);glUniform1i(u("uTreated"),2)
            advanced.bind(program,mask.color,filtered?.preset,intArrayOf(3,4,5,6))
            glUniform1fv(u("uFilter[0]"),14,(filtered?.preset?.adjustments ?: Adjustments()).uniforms(),0)
            glUniform1f(u("uFilterAmount"),filtered?.intensity ?: 0f)
            glUniform3fv(u("uShadow"),1,(filtered?.preset?.shadowTint ?: List(3) {0f}).toFloatArray(),0);glUniform3fv(u("uHighlight"),1,(filtered?.preset?.highlightTint ?: List(3) {0f}).toFloatArray(),0);glUniform1f(u("uGrading"),filtered?.preset?.grading ?: 0f)
            val d=mask.adjustments;glUniform1fv(u("uDetail[0]"),5,floatArrayOf(d.clarity,d.texture,d.sharpness,d.dehaze,d.denoise),0);glUniform2f(u("uPixel"),1f/w,1f/h)
            val fill=mask.fill
            fun rgb(value:Int)=floatArrayOf(Color.red(value)/255f,Color.green(value)/255f,Color.blue(value)/255f,Color.alpha(value)/255f)
            glUniform4fv(u("uFillA"),1,rgb(fill?.colors?.first() ?: 0),0);glUniform4fv(u("uFillB"),1,rgb(fill?.colors?.last() ?: 0),0)
            glUniform1f(u("uFillAngle"),(fill?.angle ?: 0f)*Math.PI.toFloat()/180)
            glUniform1fv(u("uLocal[0]"),14,mask.adjustments.uniforms(),0)
            glUniform1f(u("uStrength"),mask.strength);glUniform1f(u("uInvert"),if(mask.inverted) 1f else 0f)
            glUniform1f(u("uCutout"),if(mask.cutout) 1f else 0f)
            val a=glGetAttribLocation(program,"aPosition");vertices.position(0)
            glEnableVertexAttribArray(a);glVertexAttribPointer(a,2,GL_FLOAT,false,0,vertices);glDrawArrays(GL_TRIANGLE_STRIP,0,4)
            current=output
        }
        return current
    }
    private fun u(name:String)=glGetUniformLocation(program,name)
    private fun initialize() {
        if(program!=0) return
        fun compile(type:Int,source:String):Int {
            val shader=glCreateShader(type);glShaderSource(shader,source);glCompileShader(shader)
            val status=IntArray(1);glGetShaderiv(shader,GL_COMPILE_STATUS,status,0);check(status[0]!=0) {glGetShaderInfoLog(shader)};return shader
        }
        val v=compile(GL_VERTEX_SHADER,"attribute vec2 aPosition;varying vec2 vUV;void main(){gl_Position=vec4(aPosition,0.0,1.0);vUV=(aPosition+1.0)*0.5;}")
        val f=compile(GL_FRAGMENT_SHADER,SHADER)
        program=glCreateProgram();glAttachShader(program,v);glAttachShader(program,f);glLinkProgram(program)
        val linked=IntArray(1);glGetProgramiv(program,GL_LINK_STATUS,linked,0);check(linked[0]!=0) {glGetProgramInfoLog(program)}
        glDeleteShader(v);glDeleteShader(f);glGenTextures(3,textures,0)
        textures.forEach {glBindTexture(GL_TEXTURE_2D,it);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE)}
    }
    fun close() {if(program!=0) {advanced.close();effects.close();glDeleteTextures(3,textures,0);glDeleteProgram(program);program=0;cached=null}}
    companion object {
        private val SHADER="""
            precision highp float;
            varying vec2 vUV;
            uniform sampler2D uImage;
            uniform sampler2D uMask;
            uniform sampler2D uTreated;
            uniform float uFilter[14];uniform float uFilterAmount;
            uniform vec3 uShadow;uniform vec3 uHighlight;uniform float uGrading;
            uniform float uDetail[5];uniform vec2 uPixel;
            uniform vec4 uFillA;uniform vec4 uFillB;uniform float uFillAngle;
            uniform float uLocal[14];
            uniform float uStrength;
            uniform float uInvert;
            uniform float uCutout;
            ${ColorShader.functions}
            ${GpuAdvancedColor.shader}
            void main(){
                vec4 src=texture2D(uImage,vUV);
                vec3 base=src.a>0.0?src.rgb/src.a:vec3(0.0);
                vec3 treated=detail(uTreated,vUV,uPixel,uDetail[0],uDetail[1],uDetail[2],uDetail[3],uDetail[4]);
                vec3 local=adjust(treated,uLocal[0],uLocal[1],uLocal[2],uLocal[3],uLocal[4],uLocal[5],uLocal[6],uLocal[7],uLocal[8],uLocal[9],uLocal[10],uLocal[11],uLocal[12]);
                local*=1.0-smoothstep(0.22,0.72,length(vUV-0.5))*uLocal[13]*0.65;
                local=manualColor(local);
                vec3 f=adjust(local,uFilter[0],uFilter[1],uFilter[2],uFilter[3],uFilter[4],uFilter[5],uFilter[6],uFilter[7],uFilter[8],uFilter[9],uFilter[10],uFilter[11],uFilter[12]);
                f=presetColor(f);float l=lum(f);f+=(uShadow*(1.0-smoothstep(0.0,0.6,l))+uHighlight*smoothstep(0.4,1.0,l))*uGrading*.22;
                f*=1.0-smoothstep(.22,.72,length(vUV-.5))*uFilter[13]*.65;
                local=mix(local,clamp(f,0.0,1.0),uFilterAmount);
                vec4 fill=mix(uFillA,uFillB,clamp(.5+dot(vUV-.5,vec2(cos(uFillAngle),sin(uFillAngle))),0.0,1.0));local=mix(local,fill.rgb,fill.a);
                float alpha=texture2D(uMask,vUV).a;
                float weight=mix(alpha,1.0-alpha,uInvert)*uStrength;
                float outAlpha=src.a*mix(1.0,1.0-uStrength+weight,uCutout);
                gl_FragColor=vec4(mix(base,clamp(local,0.0,1.0),weight)*outAlpha,outAlpha);
            }
        """.trimIndent()
    }
}
