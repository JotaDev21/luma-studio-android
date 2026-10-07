package studio.luma.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import studio.luma.StudioUi
import studio.luma.StudioViewModel

@Composable internal fun ScanPcQr(vm:StudioViewModel,enabled:Boolean) {
    val context=LocalContext.current
    var scanning by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    var alive by remember {mutableStateOf(true)}
    DisposableEffect(Unit) {onDispose {alive=false}}
    Button(onClick={
        scanning=true;error=null
        try {
            val options=GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build()
            GmsBarcodeScanning.getClient(context,options).startScan()
                .addOnSuccessListener {barcode->if(alive) {scanning=false;barcode.rawValue?.let(vm::reviewEnhancementQr) ?: run {error="O QR não contém uma conexão. Gere outro no PC."}}}
                .addOnCanceledListener {if(alive) scanning=false}
                .addOnFailureListener {if(alive) {scanning=false;error="Não foi possível abrir o leitor. Confira o Google Play Services e a internet na primeira utilização, ou use o código manual."}}
        } catch(e:Exception) {scanning=false;error="O leitor não está disponível. Use o endereço e o código manual abaixo."}
    },enabled=enabled && !scanning,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("ai-scan-qr")) {Text(if(scanning) "Abrindo leitor…" else "Ler QR do PC",fontSize=16.sp)}
    error?.let {Text(it,color=MaterialTheme.colorScheme.error,fontSize=14.sp)}
}

@Composable internal fun PairingConfirmation(state:StudioUi,vm:StudioViewModel) {
    val request=state.pairingProposal ?: return
    AlertDialog(onDismissRequest=vm::cancelPairing,title={Text("Conectar a este PC?")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(request.endpoint,fontSize=16.sp,modifier=Modifier.testTag("ai-qr-server"))
            Text("Confira se este é o endereço mostrado no seu PC. O QR usa um código temporário e não envia fotos.",fontSize=16.sp)
            Text("Depois de conectar, só enviaremos uma foto quando você tocar em Melhorar foto.",fontSize=14.sp,color=Muted)
        }
    },confirmButton={TextButton(onClick=vm::confirmPairing,enabled=!state.busy,modifier=Modifier.testTag("ai-qr-confirm")) {Text("Conectar PC")}},dismissButton={TextButton(onClick=vm::cancelPairing) {Text("Cancelar")}})
}
