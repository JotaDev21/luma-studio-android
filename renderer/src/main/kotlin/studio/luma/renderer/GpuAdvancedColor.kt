package studio.luma.renderer

import android.opengl.GLES20.*
import studio.luma.core.*
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/** Same bindings and shader are used by the whole photograph and local masks. */
internal class GpuAdvancedColor {
    private val textures=IntArray(4)
    private var initialized=false
    private val keys=arrayOfNulls<Any>(4)
    fun bind(program:Int,manual:ColorTreatment,preset:FilterPreset?,units:IntArray=intArrayOf(1,3,4,5)) {
        if(!initialized) {
            glActiveTexture(GL_TEXTURE0+units[0]);glGenTextures(4,textures,0);initialized=true
            textures.forEach {glBindTexture(GL_TEXTURE_2D,it);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE)}
        }
        fun u(name:String)=glGetUniformLocation(program,name)
        fun curve(index:Int,curves:Curves,prior:Curves?=null) {
            glActiveTexture(GL_TEXTURE0+units[index]);glBindTexture(GL_TEXTURE_2D,textures[index])
            val key=curves to prior
            if(keys[index]!=key) {
                val data=ByteBuffer.allocateDirect(1024)
                for(i in 0..255) {
                    val channels=listOf(curves.red,curves.green,curves.blue)
                    val old=prior?.let {listOf(it.red,it.green,it.blue)}
                    for(c in 0..2) {
                        val x=if(prior==null) i/255f else sampleCurve(old!![c],sampleCurve(prior.rgb,i/255f))
                        data.put((sampleCurve(channels[c],sampleCurve(curves.rgb,x))*255).roundToInt().coerceIn(0,255).toByte())
                    };data.put(255.toByte())
                }
                data.position(0);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,256,1,0,GL_RGBA,GL_UNSIGNED_BYTE,data);keys[index]=key
            }
        }
        val filter=preset?.color ?: ColorTreatment()
        curve(0,manual.curves);curve(1,filter.curves,preset?.curves)
        glUniform1i(u("uManualCurve"),units[0]);glUniform1i(u("uPresetCurve"),units[1])
        fun treatment(prefix:String,c:ColorTreatment,index:Int) {
            glUniform3fv(u("${prefix}Hsl[0]"),8,c.hsl.flatMap {listOf(it.hue,it.saturation,it.luminance)}.toFloatArray(),0)
            glUniform3fv(u("${prefix}Tones[0]"),3,(c.shadows+c.midtones+c.highlights).toFloatArray(),0)
            glUniform3fv(u("${prefix}Balance"),1,c.balance.toFloatArray(),0)
            glUniform4fv(u("${prefix}Matrix[0]"),3,c.matrix.toFloatArray(),0)
            glUniform2f(u("${prefix}RollLuma"),c.rolloff,c.luminance)
            glActiveTexture(GL_TEXTURE0+units[index]);glBindTexture(GL_TEXTURE_2D,textures[index])
            if(keys[index]!=(c.lut ?: "neutral")) {
                val lut=c.lut;val data=ByteBuffer.allocateDirect((lut?.values?.size?.div(3) ?: 1)*4)
                if(lut==null) data.put(byteArrayOf(0,0,0,-1)) else {
                    // LUT rows are green; blue slices run horizontally, red is the fast axis.
                    for(g in 0 until lut.size) for(b in 0 until lut.size) for(r in 0 until lut.size) {
                        val offset=(r+g*lut.size+b*lut.size*lut.size)*3
                        for(channel in 0..2) data.put((lut.values[offset+channel]*255).roundToInt().toByte());data.put(255.toByte())
                    }
                }
                data.position(0);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,(lut?.size ?: 1)*(lut?.size ?: 1),lut?.size ?: 1,0,GL_RGBA,GL_UNSIGNED_BYTE,data);keys[index]=lut ?: "neutral"
            }
            glUniform1i(u("${prefix}Lut"),units[index]);glUniform2f(u("${prefix}LutInfo"),c.lut?.size?.toFloat() ?: 0f,c.lutIntensity)
            glUniform3fv(u("${prefix}DomainMin"),1,(c.lut?.domainMin ?: listOf(0f,0f,0f)).toFloatArray(),0)
            glUniform3fv(u("${prefix}DomainMax"),1,(c.lut?.domainMax ?: listOf(1f,1f,1f)).toFloatArray(),0)
        }
        treatment("uManual",manual,2);treatment("uPreset",filter,3)
    }
    fun close() {if(initialized) glDeleteTextures(4,textures,0);initialized=false;keys.fill(null)}
    companion object {
        val shader="""
            uniform sampler2D uManualCurve;uniform sampler2D uPresetCurve;
            uniform vec3 uManualHsl[8];uniform vec3 uPresetHsl[8];
            uniform vec3 uManualTones[3];uniform vec3 uPresetTones[3];
            uniform vec3 uManualBalance;uniform vec3 uPresetBalance;
            uniform vec4 uManualMatrix[3];uniform vec4 uPresetMatrix[3];
            uniform vec2 uManualRollLuma;uniform vec2 uPresetRollLuma;
            uniform sampler2D uManualLut;uniform sampler2D uPresetLut;
            uniform vec2 uManualLutInfo;uniform vec2 uPresetLutInfo;
            uniform vec3 uManualDomainMin;uniform vec3 uManualDomainMax;
            uniform vec3 uPresetDomainMin;uniform vec3 uPresetDomainMax;
            vec3 toHsv(vec3 c) {
                vec4 K=vec4(0.0,-1.0/3.0,2.0/3.0,-1.0);
                vec4 p=mix(vec4(c.bg,K.wz),vec4(c.gb,K.xy),step(c.b,c.g));
                vec4 q=mix(vec4(p.xyw,c.r),vec4(c.r,p.yzx),step(p.x,c.r));
                float d=q.x-min(q.w,q.y);return vec3(abs(q.z+(q.w-q.y)/(6.0*d+0.000001)),d/(q.x+0.000001),q.x);
            }
            vec3 fromHsv(vec3 c) {vec3 p=abs(fract(c.xxx+vec3(0.0,2.0/3.0,1.0/3.0))*6.0-3.0);return c.z*mix(vec3(1.0),clamp(p-1.0,0.0,1.0),c.y);}
            vec3 advanced(vec3 c,vec3 hsl[8],vec3 tones[3],vec3 balance,vec4 matrix[3],vec2 rl) {
                vec3 hsv=toHsv(clamp(c,0.0,1.0));vec3 shift=vec3(0.0);float sum=0.0;
                for(int i=0;i<8;i++) {float center=i==0?0.0:(i==1?0.083333:(i==2?0.166667:(i==3?0.333333:(i==4?0.5:(i==5?0.666667:(i==6?0.75:0.833333))))));float d=abs(hsv.x-center);d=min(d,1.0-d);float weight=exp(-d*d*140.0);shift+=hsl[i]*weight;sum+=weight;}
                shift/=max(sum,0.0001);hsv.x=fract(hsv.x+shift.x*0.125);hsv.y=clamp(hsv.y*(1.0+shift.y),0.0,1.0);hsv.z*=exp2(shift.z);
                c=fromHsv(hsv);float l=clamp(lum(c),0.0,1.0);
                float sh=1.0-smoothstep(0.05,0.6,l);float hi=smoothstep(0.4,0.95,l);float mid=max(0.0,1.0-sh-hi);
                c+=(tones[0]*sh+tones[1]*mid+tones[2]*hi)*0.22+balance*0.12;
                c=vec3(dot(matrix[0],vec4(c,1.0)),dot(matrix[1],vec4(c,1.0)),dot(matrix[2],vec4(c,1.0)));
                c*=exp2(rl.y);c-=rl.x*pow(max(c-0.45,vec3(0.0)),vec3(2.0))*0.6;
                return clamp(c,0.0,1.0);
            }
            vec3 curves(vec3 c,sampler2D t) {c=clamp(c,0.0,1.0);return vec3(texture2D(t,vec2((c.r*255.0+0.5)/256.0,0.5)).r,texture2D(t,vec2((c.g*255.0+0.5)/256.0,0.5)).g,texture2D(t,vec2((c.b*255.0+0.5)/256.0,0.5)).b);}
            vec3 lut(vec3 c,sampler2D t,vec2 info,vec3 lo,vec3 hi) {
                if(info.x<2.0) return c;float n=info.x;
                vec3 p=clamp((c-lo)/(hi-lo),0.0,1.0)*(n-1.0);float z=floor(p.b);float z1=min(n-1.0,z+1.0);
                vec2 a=vec2((z*n+p.r+0.5)/(n*n),(p.g+0.5)/n);vec2 b=vec2((z1*n+p.r+0.5)/(n*n),(p.g+0.5)/n);
                return mix(c,mix(texture2D(t,a).rgb,texture2D(t,b).rgb,fract(p.b)),info.y);
            }
            vec3 manualColor(vec3 c) {c=advanced(c,uManualHsl,uManualTones,uManualBalance,uManualMatrix,uManualRollLuma);return lut(curves(c,uManualCurve),uManualLut,uManualLutInfo,uManualDomainMin,uManualDomainMax);}
            vec3 presetColor(vec3 c) {c=advanced(c,uPresetHsl,uPresetTones,uPresetBalance,uPresetMatrix,uPresetRollLuma);return lut(curves(c,uPresetCurve),uPresetLut,uPresetLutInfo,uPresetDomainMin,uPresetDomainMax);}
        """.trimIndent()
    }
}
