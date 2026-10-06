package studio.luma.renderer

/** Shared SDR operators for global and local GPU adjustments. */
internal object ColorShader {
    val functions="""
            float lum(vec3 c){ return dot(c,vec3(0.2126,0.7152,0.0722)); }
            vec3 linearize(vec3 c){
                return mix(c/12.92,pow((c+0.055)/1.055,vec3(2.4)),step(vec3(0.04045),c));
            }
            vec3 encode(vec3 c){
                return mix(c*12.92,1.055*pow(max(c,vec3(0.0)),vec3(1.0/2.4))-0.055,step(vec3(0.0031308),c));
            }
            vec3 adjust(vec3 c,float e,float b,float ct,float sat,float vib,float temp,float tint,float hi,float sh,float wh,float bl,float gam,float fade){
                if(abs(e)>0.00001) c = encode(linearize(c)*exp2(e));
                c += b * 0.25;
                float l = clamp(lum(c),0.0,1.0);
                c += sh * 0.28 * (1.0-smoothstep(0.0,0.65,l));
                c += hi * 0.28 * smoothstep(0.35,1.0,l);
                c += wh * 0.18 * smoothstep(0.7,1.0,l);
                c += bl * 0.18 * (1.0-smoothstep(0.0,0.3,l));
                c *= vec3(1.0+temp*0.18+tint*0.04,1.0-tint*0.12,1.0-temp*0.18+tint*0.04);
                c = (c-0.5)*ct+0.5;
                c = pow(max(c,vec3(0.0)),vec3(1.0/gam));
                float chroma = max(c.r,max(c.g,c.b))-min(c.r,min(c.g,c.b));
                c = mix(vec3(lum(c)),c,sat+vib*(1.0-clamp(chroma,0.0,1.0))*0.5);
                c = c*(1.0-fade*0.3)+fade*0.12;
                return clamp(c,0.0,1.0);
            }
            vec3 straightPixel(vec4 s){return s.a>0.0?s.rgb/s.a:vec3(0.0);}
            vec3 detail(sampler2D image,vec2 uv,vec2 pixel,float clarity,float texture,float sharpness,float dehaze,float denoise) {
                vec3 center=straightPixel(texture2D(image,uv));
                if(abs(clarity)+abs(texture)+sharpness+abs(dehaze)+denoise<0.00001) return center;
                vec3 blur=vec3(0.0);vec3 fine=vec3(0.0);vec3 local=vec3(0.0);vec3 smoothColor=vec3(0.0);float mass=0.0;float dark=1.0;
                for(int j=-1;j<=1;j++) for(int i=-1;i<=1;i++) {
                    vec2 d=vec2(float(i),float(j));vec3 c=straightPixel(texture2D(image,uv+d*pixel));blur+=c;
                    float weight=exp(-dot(c-center,c-center)/(0.005+denoise*0.12));smoothColor+=c*weight;mass+=weight;
                    vec3 v=straightPixel(texture2D(image,uv+d*pixel*2.5));fine+=v;
                    vec3 broad=straightPixel(texture2D(image,uv+d*vec2(0.007)));local+=broad;dark=min(dark,min(broad.r,min(broad.g,broad.b)));
                }
                vec3 result=mix(center,smoothColor/max(mass,0.001),denoise*0.8);
                result+=(center-blur/9.0)*sharpness*0.8+(center-fine/9.0)*texture*0.7+(center-local/9.0)*clarity*0.6;
                if(dehaze>0.0) {float transmission=max(0.4,1.0-dehaze*0.5*dark/0.95);result=(result-vec3(0.95))/transmission+vec3(0.95);}
                else result=mix(result,vec3(0.85),-dehaze*0.3);
                return clamp(result,0.0,1.0);
            }
    """.trimIndent()
}
