package studio.luma.core

import java.net.URI
import java.net.URLDecoder

data class EnhancementPairing(val endpoint:String,val code:String,val expiresAt:Long) {
    fun requireFresh(nowSeconds:Long=System.currentTimeMillis()/1000) {
        require(expiresAt>nowSeconds) {"Este QR expirou. Abra Ver conexão Luma IA no PC para gerar outro."}
        require(expiresAt-nowSeconds<=660) {"Confira a hora do celular e gere outro QR no PC."}
    }
    companion object {
        fun parse(value:String,nowSeconds:Long=System.currentTimeMillis()/1000):EnhancementPairing {
            require(value.length in 1..1024) {"Use o QR de conexão mostrado pelo Luma no seu PC."}
            val uri=URI(value)
            require(uri.scheme=="luma" && uri.rawAuthority=="connect" && uri.path.isNullOrEmpty() && uri.rawFragment==null) {"Este QR não é uma conexão do Luma."}
            val fields=linkedMapOf<String,String>()
            (uri.rawQuery ?: "").split('&').forEach {field->
                val parts=field.split('=',limit=2)
                require(parts.size==2) {"QR de conexão incompleto."}
                val key=URLDecoder.decode(parts[0],"UTF-8")
                require(key !in fields) {"QR de conexão inválido."}
                fields[key]=URLDecoder.decode(parts[1],"UTF-8")
            }
            require(fields.keys==setOf("v","server","code","expires") && fields["v"]=="1") {"QR de conexão inválido ou de outra versão."}
            val code=fields.getValue("code")
            require(code.matches(Regex("[0-9]{6}"))) {"Código do QR inválido."}
            val expires=fields.getValue("expires").toLongOrNull() ?: error("Validade do QR inválida.")
            return EnhancementPairing(EnhancementEndpoint.normalize(fields.getValue("server")),code,expires).also {it.requireFresh(nowSeconds)}
        }
    }
}
