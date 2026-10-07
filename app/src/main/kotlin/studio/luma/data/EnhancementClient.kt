package studio.luma.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import studio.luma.core.EnhancementEndpoint
import studio.luma.renderer.ImageSource
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class EnhancementClient {
    private val http=OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS).readTimeout(5,TimeUnit.MINUTES).writeTimeout(60,TimeUnit.SECONDS).callTimeout(6,TimeUnit.MINUTES).followRedirects(false).followSslRedirects(false).build()
    private suspend fun response(request:Request):Response=suspendCancellableCoroutine {continuation->
        val call=http.newCall(request)
        continuation.invokeOnCancellation {call.cancel()}
        call.enqueue(object:Callback {
            override fun onFailure(call:Call,e:IOException) {if(continuation.isActive) continuation.resumeWithException(IOException("Não foi possível falar com seu PC. Confira se o servidor está ligado e conectado à mesma rede.",e))}
            override fun onResponse(call:Call,response:Response) {continuation.resume(response) {_,value,_->value.close()}}
        })
    }
    private fun checkResponse(response:Response) {
        if(response.isSuccessful) return
        val message=when(response.code) {
            401,403->"Conexão não autorizada. Conecte seu PC novamente."
            409,429->"Seu PC está ocupado com outra melhoria. Tente novamente em instantes."
            413->"Foto muito grande para melhorar. Reduza o tamanho da imagem."
            422->"O servidor não conseguiu ler esta foto ou essas opções."
            503->"A IA do PC ainda não está pronta. Aguarde o carregamento dos modelos."
            else->"O servidor não conseguiu concluir a melhoria. Tente novamente."
        }
        throw IOException(message)
    }
    suspend fun pair(address:String,code:String):EnhancementConnection=withContext(Dispatchers.IO) {
        val endpoint=EnhancementEndpoint.normalize(address)
        require(code.matches(Regex("[0-9]{6}"))) {"Digite o código de seis números mostrado no PC."}
        val request=Request.Builder().url("$endpoint/pair").post(JSONObject().put("code",code).toString().toRequestBody("application/json".toMediaType())).build()
        response(request).use {reply->
            checkResponse(reply)
            val body=requireNotNull(reply.body)
            require(body.contentLength() in -1..4096)
            val source=body.source();source.request(4097)
            require(source.buffer.size<=4096)
            val bytes=source.readByteArray()
            val token=JSONObject(bytes.toString(Charsets.UTF_8)).getString("token")
            require(token.length in 32..256)
            EnhancementConnection(endpoint,token)
        }
    }
    suspend fun enhance(connection:EnhancementConnection,input:File,output:File,scale:Int,faces:Boolean,strength:Float)=withContext(Dispatchers.IO) {
        require(scale in listOf(2,4) && strength.isFinite() && strength in .1f..1f && input.length() in 1..64_000_000)
        val url="${EnhancementEndpoint.normalize(connection.endpoint)}/enhance?scale=$scale&faces=$faces&strength=$strength"
        val request=Request.Builder().url(url).header("Authorization","Bearer ${connection.token}").post(input.asRequestBody("image/png".toMediaType())).build()
        try {
            response(request).use {reply->
                checkResponse(reply)
                require(reply.header("Content-Type")?.substringBefore(';')=="image/png") {"O PC respondeu com um arquivo inválido."}
                val body=requireNotNull(reply.body)
                require(body.contentLength()<=150_000_000) {"Resultado muito grande."}
                body.byteStream().use {source->output.outputStream().use {target->
                    val buffer=ByteArray(65536);var total=0L
                    while(true) {val count=source.read(buffer);if(count<0) break;total+=count;require(total<=150_000_000);target.write(buffer,0,count)}
                }}
            }
            val size=ImageSource.dimensions(output)
            require(size.first in 1..8192 && size.second in 1..8192 && size.first.toLong()*size.second<=32_000_000) {"O resultado ultrapassa as dimensões permitidas."}
        } catch(e:Exception) {output.delete();throw e}
    }
}
