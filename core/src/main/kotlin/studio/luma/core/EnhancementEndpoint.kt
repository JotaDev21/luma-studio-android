package studio.luma.core

import java.net.URI

object EnhancementEndpoint {
    fun normalize(value: String): String {
        val uri=URI(value.trim().trimEnd('/'))
        require(uri.scheme in listOf("http","https") && uri.host!=null && uri.rawUserInfo==null && uri.rawQuery==null && uri.rawFragment==null && uri.path.isNullOrEmpty()) {"Use apenas o endereço do servidor, sem caminhos ou senhas."}
        require(uri.port==-1 || uri.port in 1..65535) {"Porta inválida."}
        if(uri.scheme=="http") {
            val parts=uri.host.split('.').map {it.toIntOrNull()}
            val local=uri.host=="localhost" || parts.size==4 && parts.all {it!=null && it in 0..255} && (parts[0]==127 || parts[0]==10 || parts[0]==192 && parts[1]==168 || parts[0]==172 && parts[1]!! in 16..31)
            require(local) {"Fora da rede local, o servidor precisa usar HTTPS."}
        }
        return uri.toASCIIString()
    }
}
