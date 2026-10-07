package studio.luma.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import studio.luma.EnhancementProposal
import studio.luma.StudioUi
import studio.luma.StudioViewModel

@Composable internal fun EnhancementControls(state:StudioUi,vm:StudioViewModel,dismiss:()->Unit) {
    var setup by remember {mutableStateOf(!state.enhancementConnected)}
    var endpoint by remember {mutableStateOf(state.enhancementEndpoint)}
    var code by remember {mutableStateOf("")}
    var scale by remember {mutableIntStateOf(2)}
    var faces by remember {mutableStateOf(true)}
    var strength by remember {mutableFloatStateOf(.7f)}
    LaunchedEffect(state.enhancementConnected) {if(state.enhancementConnected) setup=false}
    AlertDialog(onDismissRequest={if(!state.busy) dismiss()},title={Text(if(setup) "Conectar seu PC" else "Melhorar com IA")},text={
        Column(Modifier.verticalScroll(rememberScrollState()).testTag("ai-controls"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(setup) {
                Text("Abra o servidor Luma AI no PC e use o endereço e o código mostrados nele. O celular e o PC precisam estar na mesma rede.",fontSize=13.sp)
                OutlinedTextField(endpoint,{endpoint=it},label={Text("Endereço do PC")},placeholder={Text("http://192.168.1.10:8765")},singleLine=true,modifier=Modifier.testTag("ai-endpoint"))
                OutlinedTextField(code,{code=it.filter(Char::isDigit).take(6)},label={Text("Código de conexão")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),singleLine=true,modifier=Modifier.testTag("ai-pair-code"))
                Text("A conexão fica salva. Você não precisa repetir o código a cada foto.",color=Muted,fontSize=11.sp)
            } else {
                Text("Mais definição, com um novo olhar para os detalhes.",fontSize=16.sp)
                Text("Ampliação",color=Muted,fontSize=12.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {listOf(2,4).forEach {value->FilterChip(scale==value,{scale=value},label={Text("Até ${value}×")})}}
                Text("Até 4096 px no maior lado. Fotos grandes são preparadas em até 2048 px para a IA.",color=Muted,fontSize=11.sp)
                Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(faces,{faces=it},modifier=Modifier.testTag("ai-faces"));Column {Text("Restaurar rostos",fontSize=13.sp);Text("Ideal para selfies e fotos antigas",color=Muted,fontSize=11.sp)}}
                Text("Força da melhoria · ${(strength*100).toInt()}%",fontSize=12.sp)
                Slider(strength,{strength=it},valueRange=.1f..1f,modifier=Modifier.testTag("ai-strength"))
                Text("Você compara antes de salvar. A IA pode criar detalhes ou alterar traços do rosto; reduza a força se ficar artificial.",color=Muted,fontSize=11.sp)
                Text("Somente a foto editada será enviada ao seu PC, sem dados de localização. O projeto original será mantido.",fontSize=12.sp)
                TextButton(onClick={setup=true;endpoint=state.enhancementEndpoint;code=""}) {Text("Trocar ou reconectar PC")}
            }
        }
    },confirmButton={TextButton(onClick={
        if(setup) vm.connectEnhancement(endpoint,code) else {dismiss();vm.enhancePhoto(scale,faces,strength)}
    },enabled=!state.busy && (!setup || endpoint.isNotBlank() && code.length==6),modifier=Modifier.testTag("ai-start")) {Text(if(setup) "Conectar" else "Melhorar foto")}},dismissButton={TextButton(onClick=dismiss,enabled=!state.busy) {Text("Cancelar")}})
}

@Composable internal fun EnhancementComparison(proposal:EnhancementProposal,busy:Boolean,apply:()->Unit,discard:()->Unit) {
    var split by remember(proposal.file) {mutableFloatStateOf(.5f)}
    Dialog(onDismissRequest={if(!busy) discard()},properties=DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=!busy,dismissOnClickOutside=false)) {
        Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding().navigationBarsPadding().padding(20.dp).testTag("ai-comparison"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Sua foto, com mais detalhe.",fontSize=24.sp,color=Color.White)
            Text("${proposal.width} × ${proposal.height} px · deslize para comparar",fontSize=12.sp,color=Muted)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                val ratio=proposal.after.width.toFloat()/proposal.after.height
                val width=minOf(maxWidth,maxHeight*ratio)
                Box(Modifier.width(width).aspectRatio(ratio).clip(RoundedCornerShape(14.dp)).background(Panel)) {
                    Image(proposal.after.asImageBitmap(),"Resultado da IA",Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
                    Image(proposal.before.asImageBitmap(),"Antes da melhoria",Modifier.fillMaxSize().drawWithContent {clipRect(right=size.width*split) {this@drawWithContent.drawContent()}},contentScale=ContentScale.FillBounds)
                    Canvas(Modifier.fillMaxSize()) {drawLine(Color.White,androidx.compose.ui.geometry.Offset(size.width*split,0f),androidx.compose.ui.geometry.Offset(size.width*split,size.height),2.dp.toPx())}
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text("ANTES",color=Muted,fontSize=11.sp);Text("COM IA",color=Acid,fontSize=11.sp)}
            Slider(split,{split=it},valueRange=0f..1f,enabled=!busy,modifier=Modifier.testTag("ai-compare-slider"))
            Text("Confira principalmente os olhos, a pele e os contornos. O resultado será salvo como outra foto no estúdio.",color=Muted,fontSize=12.sp)
            Button(onClick=apply,enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("ai-apply"),shape=RoundedCornerShape(12.dp)) {Text(if(busy) "Salvando…" else "Usar cópia melhorada")}
            TextButton(onClick=discard,enabled=!busy,modifier=Modifier.align(Alignment.CenterHorizontally)) {Text("Manter como estava")}
        }
    }
}
