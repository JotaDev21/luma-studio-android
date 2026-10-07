package studio.luma

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import studio.luma.ui.StudioApp
import studio.luma.ui.LumaTheme
import studio.luma.core.EnhancementEndpoint
import androidx.compose.runtime.*
import androidx.compose.material3.*

private data class PcConnectionRequest(val endpoint:String,val code:String)

class MainActivity : ComponentActivity() {
    val studio: StudioViewModel by viewModels()
    private var pcConnection by mutableStateOf<PcConnectionRequest?>(null)
    private fun readPcConnection(intent:Intent?) {
        if(intent?.action!="studio.luma.CONNECT_AI") return
        val endpoint=runCatching {EnhancementEndpoint.normalize(intent.getStringExtra("endpoint") ?: "")}.getOrNull() ?: return
        val code=intent.getStringExtra("pairCode") ?: return
        if(code.matches(Regex("[0-9]{6}"))) pcConnection=PcConnectionRequest(endpoint,code)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readPcConnection(intent)
        enableEdgeToEdge()
        setContent {
            StudioApp(studio)
            pcConnection?.let {request->LumaTheme {
                val state by studio.ui.collectAsState()
                AlertDialog(onDismissRequest={pcConnection=null},title={Text("Conectar a IA do seu PC?")},text={Text("Servidor: ${request.endpoint}\n\nSuas fotos só serão enviadas quando você escolher Melhorar com IA. A conexão será salva neste celular.")},confirmButton={TextButton(onClick={pcConnection=null;studio.connectEnhancement(request.endpoint,request.code)},enabled=!state.busy) {Text("Conectar PC")}},dismissButton={TextButton(onClick={pcConnection=null}) {Text("Cancelar")}})
            }}
        }
    }
    override fun onNewIntent(intent:Intent) {super.onNewIntent(intent);setIntent(intent);readPcConnection(intent)}
}
