package studio.luma.renderer

import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.graphics.*
import java.io.File
import studio.luma.core.EffectTypes
import studio.luma.core.EffectLayer
import java.nio.FloatBuffer

/** Three reusable render targets. No pass samples its output texture. */
internal class GpuEffects {
    private val targets = IntArray(3)
    private var program = 0
    private val overlay=IntArray(1)
    private var overlayKey:String?=null
    private var width = 0
    private var height = 0
    fun render(input: Int, w: Int, h: Int, effects: List<EffectLayer>, framebuffer: Int, vertices: FloatBuffer, directory:File?=null): Int {
        val active = effects.filter { it.enabled && it.intensity > 0f }
        if (active.isEmpty()) return input
        initialize()
        if (width != w || height != h) {
            targets.forEach { glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_2D,it); glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,w,h,0,GL_RGBA,GL_UNSIGNED_BYTE,null) }
            width=w; height=h
        }
        fun target(vararg excluded: Int) = targets.first { it !in excluded }
        fun pass(source: Int, output: Int, mode: Int, values: FloatArray, aux: Int = source, dx: Float = 0f, dy: Float = 0f) {
            check(output != source && output != aux)
            glBindFramebuffer(GL_FRAMEBUFFER,framebuffer)
            glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,output,0)
            check(glCheckFramebufferStatus(GL_FRAMEBUFFER)==GL_FRAMEBUFFER_COMPLETE) { "GPU sem memória para os efeitos." }
            glUseProgram(program); glViewport(0,0,w,h)
            glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D,source); glUniform1i(u("uImage"),0)
            glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D,aux); glUniform1i(u("uOriginal"),1)
            glActiveTexture(GL_TEXTURE2);glBindTexture(GL_TEXTURE_2D,overlay[0]);glUniform1i(u("uOverlay"),2)
            glUniform1i(u("uMode"),mode); glUniform4fv(u("uParams"),1,values,0)
            glUniform2f(u("uDirection"),dx,dy)
            glUniform2f(u("uReference"),1080f,1080f*h/w)
            val a=glGetAttribLocation(program,"aPosition"); vertices.position(0)
            glEnableVertexAttribArray(a); glVertexAttribPointer(a,2,GL_FLOAT,false,0,vertices); glDrawArrays(GL_TRIANGLE_STRIP,0,4)
        }
        var current = input
        for (layer in active) {
            val p=layer.preset.parameters
            val amount=(p["amount"] ?: 1f)*layer.intensity
            val radius=(p["radius"] ?: .3f)*36f/1080f
            when(layer.preset.type) {
                "blur", "bloom", "halation", "glow", "softlight" -> {
                    val light=layer.preset.type!="blur"
                    val original=current; val a=target(original); val b=target(original,a)
                    if(light) {
                        pass(original,a,7,floatArrayOf(p["threshold"] ?: .7f,0f,0f,0f))
                        pass(a,b,2,floatArrayOf(1f,0f,0f,0f),dx=radius)
                    } else pass(original,b,2,floatArrayOf(1f,0f,0f,0f),dx=radius)
                    pass(b,a,2,floatArrayOf(1f,0f,0f,0f),dy=radius*w/h)
                    val out=target(original,a)
                    pass(a,out,if(light) 8 else 9,floatArrayOf(amount,0f,if(layer.preset.type=="halation") 1f else 0f,if(layer.preset.type=="softlight") 1f else 0f),aux=original)
                    current=out
                }
                else -> {
                    val out=target(current)
                    if(layer.preset.type in listOf("texture","timestamp")) {
                        val key="${layer.preset.asset}:${layer.preset.text}:$w:$h"
                        if(overlayKey!=key) {
                            val bitmap=if(layer.preset.type=="texture") {
                                val file=directory?.let {File(it,layer.preset.asset ?: "")}
                                require(file!=null && file.isFile) {"Textura indisponível."};ImageSource.decode(file,1280)
                            } else Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).also { image ->
                                val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.rgb(255,177,76);textSize=w*.035f;typeface=Typeface.MONOSPACE;setShadowLayer(w*.002f,0f,0f,Color.BLACK)}
                                Canvas(image).drawText(layer.preset.text.take(120),w*.05f,h*.94f,paint)
                            }
                            try {glActiveTexture(GL_TEXTURE2);glBindTexture(GL_TEXTURE_2D,overlay[0]);GLUtils.texImage2D(GL_TEXTURE_2D,0,bitmap,0)} finally {bitmap.recycle()};overlayKey=key
                        }
                    }
                    val mode=when(layer.preset.type) { "grain"->1; "chromatic"->3; "leak"->4; "dust"->5; "frame"->6; else->10+EffectTypes.all.drop(7).indexOf(layer.preset.type).also {require(it>=0)} }
                    pass(current,out,mode,floatArrayOf(amount,p["size"] ?: p["width"] ?: p["radius"] ?: .3f,p["seed"] ?: p["angle"] ?: p["style"] ?: .5f,p["warmth"] ?: p["color"] ?: p["blend"] ?: .8f))
                    current=out
                }
            }
        }
        glBindFramebuffer(GL_FRAMEBUFFER,framebuffer); glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,current,0)
        return current
    }
    private fun u(name: String)=glGetUniformLocation(program,name)
    private fun initialize() {
        if(program!=0) return
        fun compile(type:Int,source:String):Int {
            val shader=glCreateShader(type); glShaderSource(shader,source); glCompileShader(shader)
            val status=IntArray(1); glGetShaderiv(shader,GL_COMPILE_STATUS,status,0)
            check(status[0]!=0) { glGetShaderInfoLog(shader) }; return shader
        }
        val v=compile(GL_VERTEX_SHADER,"attribute vec2 aPosition; varying vec2 vUV; void main(){gl_Position=vec4(aPosition,0.0,1.0);vUV=(aPosition+1.0)*0.5;}")
        val f=compile(GL_FRAGMENT_SHADER,SHADER)
        program=glCreateProgram(); glAttachShader(program,v); glAttachShader(program,f); glLinkProgram(program)
        val status=IntArray(1); glGetProgramiv(program,GL_LINK_STATUS,status,0); check(status[0]!=0) { glGetProgramInfoLog(program) }
        glDeleteShader(v); glDeleteShader(f); glGenTextures(3,targets,0);glGenTextures(1,overlay,0)
        (targets.toList()+overlay.toList()).forEach { glBindTexture(GL_TEXTURE_2D,it); glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR); glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR); glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE); glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE) }
    }
    fun close() { if(program!=0) { glDeleteTextures(3,targets,0);glDeleteTextures(1,overlay,0);overlayKey=null; glDeleteProgram(program); program=0 } }
    companion object {
        private val SHADER="""
            precision highp float;
            varying vec2 vUV;
            uniform sampler2D uImage;
            uniform sampler2D uOriginal;
            uniform sampler2D uOverlay;
            uniform int uMode;
            uniform vec4 uParams;
            uniform vec2 uDirection;
            uniform vec2 uReference;
            float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7))+uParams.z*437.0)*43758.5453);}
            vec3 straight(vec4 c){return c.a>0.0?c.rgb/c.a:vec3(0.0);}
            void main(){
                vec4 src=texture2D(uImage,vUV);
                vec3 c=straight(src);
                float a=uParams.x;
                if(uMode==1){
                    vec2 cell=floor(vUV*uReference/(0.6+uParams.y*3.0));
                    float n=(hash(cell)+hash(cell+17.7)-1.0);
                    float l=dot(c,vec3(0.2126,0.7152,0.0722));
                    c+=n*a*0.16*(0.3+0.7*sin(clamp(l,0.0,1.0)*3.14159));
                } else if(uMode==2){
                    gl_FragColor=src*0.227027;
                    gl_FragColor+=texture2D(uImage,vUV+uDirection*1.384615)*0.316216;
                    gl_FragColor+=texture2D(uImage,vUV-uDirection*1.384615)*0.316216;
                    gl_FragColor+=texture2D(uImage,vUV+uDirection*3.230769)*0.070270;
                    gl_FragColor+=texture2D(uImage,vUV-uDirection*3.230769)*0.070270;
                    return;
                } else if(uMode==3){
                    vec2 d=(vUV-0.5)*a*(0.003+uParams.y*0.02);
                    c.r=straight(texture2D(uImage,vUV+d)).r;
                    c.b=straight(texture2D(uImage,vUV-d)).b;
                } else if(uMode==4){
                    float angle=uParams.z*6.283185;
                    vec2 center=vec2(0.5)+vec2(cos(angle),sin(angle))*0.64;
                    float glow=exp(-dot(vUV-center,vUV-center)*(5.0+uParams.y*12.0))*a*0.8;
                    vec3 light=mix(vec3(0.16,0.38,0.9),vec3(1.0,0.28,0.05),uParams.w);
                    c=vec3(1.0)-(vec3(1.0)-c)*(vec3(1.0)-light*glow);
                } else if(uMode==5){
                    vec2 grid=vUV*uReference/(2.0+uParams.y*6.0);
                    vec2 cell=floor(grid); vec2 q=fract(grid)-0.5;
                    float speck=(1.0-smoothstep(0.06,0.16,length(q)))*step(0.984,hash(cell));
                    float scratch=step(0.998,hash(vec2(floor(grid.x),0.0)))*(1.0-smoothstep(0.01,0.06,abs(q.x)));
                    c=mix(c,vec3(0.95,0.91,0.82),clamp((speck+scratch)*a,0.0,1.0));
                } else if(uMode==6){
                    float border=0.008+uParams.y*0.075;
                    float yborder=border*uReference.x/uReference.y;
                    float bottom=uParams.z>0.6?yborder*3.0:yborder;
                    float inside=step(border,vUV.x)*step(vUV.x,1.0-border)*step(yborder,vUV.y)*step(vUV.y,1.0-bottom);
                    if(uParams.z<0.2) inside=step(0.12,vUV.y)*step(vUV.y,0.88);
                    vec3 paper=uParams.z<0.4?vec3(0.015):vec3(0.96,0.94,0.88);
                    c=mix(c,paper,(1.0-inside)*a);
                } else if(uMode==7){
                    float l=dot(c,vec3(0.2126,0.7152,0.0722));
                    float bright=smoothstep(uParams.x,uParams.x+0.16,l);
                    gl_FragColor=vec4(c*bright*src.a,src.a); return;
                } else if(uMode==8){
                    vec4 original=texture2D(uOriginal,vUV);
                    c*=mix(vec3(1.0),vec3(1.0,0.22,0.08),uParams.z);
                    c=mix(vec3(1.0)-(vec3(1.0)-straight(original))*(vec3(1.0)-c*a*0.7),straight(original)*(0.85+c*0.3),uParams.w*a);
                    src.a=original.a;
                } else if(uMode==9){
                    gl_FragColor=mix(texture2D(uOriginal,vUV),src,a); return;
                } else if(uMode==10){vec2 q=vUV-0.5;vec2 uv=0.5+q*(1.0+-0.65*a*dot(q,q)*3.0);gl_FragColor=texture2D(uImage,clamp(uv,0.0,1.0));return;} else if(uMode==11){vec2 q=vUV-0.5;vec2 uv=0.5+q*(1.0+-0.3*a*dot(q,q)*3.0);gl_FragColor=texture2D(uImage,clamp(uv,0.0,1.0));return;} else if(uMode==12){vec2 q=vUV-0.5;vec2 uv=0.5+q*(1.0+0.5*a*dot(q,q)*3.0);gl_FragColor=texture2D(uImage,clamp(uv,0.0,1.0));return;} else if(uMode==13){vec2 cell=floor(vUV*35.0);float d=pow(hash(cell),16.0)*exp(-length(fract(vUV*35.0)-0.5)*6.0);c+=vec3(1.0,0.8,0.5)*d*a*0.7;} else if(uMode==14){vec2 q=vUV-vec2(0.18,0.22);float rays=pow(abs(cos(atan(q.y,q.x)*6.0)),20.0);float f=exp(-length(q)*10.0)+rays*exp(-length(q)*6.0)*0.35;float ring=exp(-pow(length(vUV-vec2(0.7,0.65))-0.12,2.0)*1800.0);c+=a*(vec3(1.0,0.72,0.35)*f+vec3(0.2,0.65,0.8)*ring*0.15);} else if(uMode==16){gl_FragColor=texture2D(uImage,clamp(vUV+vec2(sin(uParams.z*61.0),cos(uParams.z*47.0))*a*0.007,0.0,1.0));return;} else if(uMode==17){c*=1.0-a*(0.08+hash(vec2(floor(vUV.y*12.0),0.0))*0.15);} else if(uMode==18){float line=floor(vUV.y*240.0);vec2 q=vUV+vec2((hash(vec2(line,0.0))-0.5)*a*0.018,0.0);c=straight(texture2D(uImage,q));c.r=straight(texture2D(uImage,q+vec2(a*0.004,0.0))).r;c+=a*(hash(floor(vUV*uReference))-0.5)*0.14;c*=1.0-a*0.12*(0.5+0.5*sin(vUV.y*1508.0));} else if(uMode==19){vec2 q=vUV-0.5;vec2 uv=0.5+q*(1.0+a*dot(q,q)*0.35);c=straight(texture2D(uImage,clamp(uv,0.0,1.0)));float stripe=mod(floor(vUV.x*uReference.x),3.0);vec3 phosphor=stripe<1.0?vec3(1.0,0.6,0.6):(stripe<2.0?vec3(0.6,1.0,0.6):vec3(0.6,0.6,1.0));c*=mix(vec3(1.0),phosphor,a*.7);c*=1.0-a*.25*(.5+.5*sin(vUV.y*uReference.y*3.14159));} else if(uMode==20){vec2 d=vec2(cos(uParams.z*6.283),sin(uParams.z*6.283))*a*(0.002+uParams.y*.025);c.r=straight(texture2D(uImage,vUV+d)).r;c.b=straight(texture2D(uImage,vUV-d)).b;} else if(uMode==21){vec2 grid=uReference/(2.0+uParams.y*28.0);c=mix(c,straight(texture2D(uImage,(floor(vUV*grid)+0.5)/grid)),a);} else if(uMode==22){float line=floor(vUV.y*(20.0+uParams.y*80.0));float jump=(hash(vec2(line,0.0))-.5)*step(.73,hash(vec2(line,5.0)))*a*.15;c=straight(texture2D(uImage,clamp(vUV+vec2(jump,0.0),0.0,1.0)));c.g=straight(texture2D(uImage,vUV-vec2(jump*.5,0.0))).g;} else if(uMode==23){c*=1.0-a*.45*step(.5,fract(vUV.y*uReference.y/(1.0+uParams.y*4.0)));} else if(uMode==24){vec2 q=floor(vUV*uReference/(1.0+uParams.y*5.0));c+=a*(vec3(hash(q),hash(q+31.0),hash(q+83.0))-.5)*.32;} else if(uMode==25){vec2 grid=uReference/(4.0+uParams.y*20.0);vec3 block=straight(texture2D(uImage,(floor(vUV*grid)+.5)/grid));float levels=6.0+(1.0-uParams.y)*26.0;c=mix(c,floor(mix(c,block,.5)*levels+.5)/levels,a);} else if(uMode==26){vec2 cell=floor(vUV*vec2(25.0,18.0));vec2 d=vec2(hash(cell)-.5,hash(cell+9.0)-.5)*a*.08;c=mix(c,straight(texture2D(uImage,clamp(vUV+d,0.0,1.0))),a*step(.4,hash(cell+3.0)));} else if(uMode==28){vec2 d=2.0/uReference;vec3 edge=abs(c-straight(texture2D(uImage,vUV+d)))*6.0;c+=a*edge*vec3(.4,.1,1.0);} else if(uMode==30){c=mix(c,c*1.35+vec3(.16),a*(1.0-smoothstep(.1,.7,length(vUV-.5))));} else if(uMode==31){c*=exp2(a*1.7);} else if(uMode==32){vec3 light=.5+.5*cos(vec3(0.0,2.094,4.188)+uParams.w*6.283);c+=a*light*exp(-length(vUV-vec2(.2,.3))*3.0)*.4;} else if(uMode==33){vec2 d=(vUV-.5)*a*.055;c.r=straight(texture2D(uImage,vUV+d)).r;c.g=straight(texture2D(uImage,vUV-d*.5)).g;c.b=straight(texture2D(uImage,vUV-d)).b;} else if(uMode==34){float band=exp(-pow(length(vUV-vec2(.5,1.2))-.9,2.0)*180.0);vec3 rainbow=.5+.5*cos(vec3(0.0,2.094,4.188)+(length(vUV-vec2(.5,1.2))-.75)*45.0);c+=rainbow*band*a*.45;} else if(uMode==35){vec2 direction=vec2(.025,0.0)*(.2+uParams.y);vec4 sum=vec4(0.0);for(int i=0;i<13;i++){float k=float(i)/12.0-.5;sum+=texture2D(uImage,clamp(vUV+direction*k,0.0,1.0));}gl_FragColor=mix(src,sum/13.0,a);return;} else if(uMode==36){vec2 direction=(vUV-.5)*.12*(.2+uParams.y);vec4 sum=vec4(0.0);for(int i=0;i<13;i++){float k=float(i)/12.0-.5;sum+=texture2D(uImage,clamp(vUV+direction*k,0.0,1.0));}gl_FragColor=mix(src,sum/13.0,a);return;} else if(uMode==37){vec2 direction=vec2(-(vUV.y-.5),vUV.x-.5)*.08*(.2+uParams.y);vec4 sum=vec4(0.0);for(int i=0;i<13;i++){float k=float(i)/12.0-.5;sum+=texture2D(uImage,clamp(vUV+direction*k,0.0,1.0));}gl_FragColor=mix(src,sum/13.0,a);return;} else if(uMode==38){vec2 direction=vec2(cos(uParams.z*6.283),sin(uParams.z*6.283))*.04*(.2+uParams.y);vec4 sum=vec4(0.0);for(int i=0;i<13;i++){float k=float(i)/12.0-.5;sum+=texture2D(uImage,clamp(vUV+direction*k,0.0,1.0));}gl_FragColor=mix(src,sum/13.0,a);return;} else if(uMode==39){vec2 direction=vec2(.015,.02)*(.2+uParams.y);vec4 sum=vec4(0.0);for(int i=0;i<13;i++){float k=float(i)/12.0-.5;sum+=texture2D(uImage,clamp(vUV+direction*k,0.0,1.0));}gl_FragColor=mix(src,sum/13.0,a);return;} else if(uMode==40){float n=hash(floor(vUV*uReference));c=mix(c,c*vec3(.98,.95,.88)+(n-.5)*.1,a*.6);} else if(uMode==41){vec2 q=vUV*uReference*.8;float weave=sin(q.x*3.14159)*sin(q.y*3.14159);c+=weave*a*.045;} else if(uMode==42){float fold=pow(abs(sin((vUV.x+vUV.y*.3)*35.0+sin(vUV.y*20.0))),35.0);c=mix(c,c*.92+vec3(fold*.23),a);} else if(uMode==43){vec2 d=vec2(sin(vUV.y*30.0),cos(vUV.x*25.0))*.008*a;c=straight(texture2D(uImage,clamp(vUV+d,0.0,1.0)));c+=pow(abs(sin((vUV.x-vUV.y)*18.0)),45.0)*a*.1;} else if(uMode==44){float line=hash(vec2(floor(vUV.y*uReference.y),0.0));c=mix(c,vec3(dot(c,vec3(.2126,.7152,.0722)))*.9+(line-.5)*.12,a*.65);} else if(uMode==45){float l=dot(c,vec3(.2126,.7152,.0722));c=mix(c,vec3(smoothstep(.25,.72,l)+(hash(floor(vUV*uReference))-.5)*.18),a);} else if(uMode==46){float l=dot(c,vec3(.2126,.7152,.0722));vec2 q=fract(vUV*uReference/5.0)-.5;float ink=1.0-smoothstep(sqrt(1.0-l)*.48,sqrt(1.0-l)*.48+.04,length(q));c=mix(c,vec3(.92,.9,.82)*(1.0-ink),a);} else if(uMode==47){vec2 grid=vec2(vUV.x+vUV.y*.2,vUV.y)*vec2(70.0,18.0);vec2 q=fract(grid);float drop=step(.78,hash(floor(grid)))*(1.0-smoothstep(.015,.06,abs(q.x-.5)))*smoothstep(.0,.15,q.y)*(1.0-smoothstep(.7,1.0,q.y));c+=drop*a*.5;} else if(uMode==48){vec2 grid=vUV*vec2(14.0,24.0);vec2 q=fract(grid)-.5;float star=(exp(-abs(q.x)*80.0-abs(q.y)*8.0)+exp(-abs(q.y)*80.0-abs(q.x)*8.0))*step(.82,hash(floor(grid)));c=mix(c,vec3(1.0,.9,.6),clamp(star*a,0.0,1.0));} else if(uMode==49){vec2 grid=vUV*vec2(7.0,12.0);vec2 q=(fract(grid)-.5)*2.0;q.y=-q.y;float heart=pow(q.x*q.x+q.y*q.y-.28,3.0)-q.x*q.x*q.y*q.y*q.y;float shape=(1.0-step(0.0,heart))*step(.73,hash(floor(grid)));c=mix(c,vec3(1.0,.24,.46),shape*a*.8);} else if(uMode==50){float line=1.0-smoothstep(.003,.009,abs(vUV.y-(.7+.1*sin(vUV.x*28.0))));line*=step(.12,vUV.x)*step(vUV.x,.88);c=mix(c,vec3(1.0,.85,.12),line*a);} else if(uMode==51){float tape=step(.1,vUV.x)*step(vUV.x,.45)*step(.035,vUV.y)*step(vUV.y,.09);c=mix(c,vec3(.91,.83,.61),tape*a*.6);} else if(uMode==52){vec4 overlay=texture2D(uOverlay,vUV);c=mix(c,straight(overlay),overlay.a*a);} else if(uMode==53){vec4 overlay=texture2D(uOverlay,vUV);vec3 t=straight(overlay);vec3 mixed=mix(c*t,1.0-(1.0-c)*(1.0-t),uParams.w);c=mix(c,mixed,overlay.a*a);} else if(uMode==54){float border=.012+uParams.y*.05;float bottom=border;vec3 paper=vec3(.97,.94,.86);bottom=border*3.5;float inside=step(border,vUV.x)*step(vUV.x,1.0-border)*step(border*uReference.x/uReference.y,vUV.y)*step(vUV.y,1.0-bottom*uReference.x/uReference.y);c=mix(c,paper,(1.0-inside)*a);} else if(uMode==55){float border=.012+uParams.y*.05;float bottom=border;vec3 paper=vec3(.97,.94,.86);border=.07;paper=vec3(.02);float inside=step(border,vUV.x)*step(vUV.x,1.0-border)*step(border*uReference.x/uReference.y,vUV.y)*step(vUV.y,1.0-bottom*uReference.x/uReference.y);paper=mix(paper,vec3(.8),step(.03,abs(vUV.x-.5))*step(.46,abs(vUV.x-.5))*(1.0-step(.55,fract(vUV.y*16.0))));c=mix(c,paper,(1.0-inside)*a);} else if(uMode==56){float border=.012+uParams.y*.05;float bottom=border;vec3 paper=vec3(.97,.94,.86);paper=vec3(.04,.035,.025);float inside=step(border,vUV.x)*step(vUV.x,1.0-border)*step(border*uReference.x/uReference.y,vUV.y)*step(vUV.y,1.0-bottom*uReference.x/uReference.y);c=mix(c,paper,(1.0-inside)*a);} else if(uMode==57){float border=.012+uParams.y*.05;float bottom=border;vec3 paper=vec3(.97,.94,.86);border=.008;paper=vec3(.9);float inside=step(border,vUV.x)*step(vUV.x,1.0-border)*step(border*uReference.x/uReference.y,vUV.y)*step(vUV.y,1.0-bottom*uReference.x/uReference.y);c=mix(c,paper,(1.0-inside)*a);} else if(uMode==58){float border=.012+uParams.y*.05;float bottom=border;vec3 paper=vec3(.97,.94,.86);border=.016;paper=vec3(.15,.17,.18);float inside=step(border,vUV.x)*step(vUV.x,1.0-border)*step(border*uReference.x/uReference.y,vUV.y)*step(vUV.y,1.0-bottom*uReference.x/uReference.y);c=mix(c,paper,(1.0-inside)*a);} else if(uMode==59){float border=.012+uParams.y*.05;float bottom=border;vec3 paper=vec3(.97,.94,.86);bottom=border*2.0;paper=vec3(.99);float inside=step(border,vUV.x)*step(vUV.x,1.0-border)*step(border*uReference.x/uReference.y,vUV.y)*step(vUV.y,1.0-bottom*uReference.x/uReference.y);c=mix(c,paper,(1.0-inside)*a);} else if(uMode==60){float border=.012+uParams.y*.05;float bottom=border;vec3 paper=vec3(.97,.94,.86);border+=hash(floor(vUV*uReference/8.0))*.003;float inside=step(border,vUV.x)*step(vUV.x,1.0-border)*step(border*uReference.x/uReference.y,vUV.y)*step(vUV.y,1.0-bottom*uReference.x/uReference.y);c=mix(c,paper,(1.0-inside)*a);} else if(uMode==61){float border=.012+uParams.y*.05;float bottom=border;vec3 paper=vec3(.97,.94,.86);bottom=border*2.0;paper=vec3(.83,.72,.52)+hash(floor(vUV*uReference))*.05;float inside=step(border,vUV.x)*step(vUV.x,1.0-border)*step(border*uReference.x/uReference.y,vUV.y)*step(vUV.y,1.0-bottom*uReference.x/uReference.y);c=mix(c,paper,(1.0-inside)*a);}
                gl_FragColor=vec4(clamp(c,0.0,1.0)*src.a,src.a);
            }
        """.trimIndent()
    }
}
