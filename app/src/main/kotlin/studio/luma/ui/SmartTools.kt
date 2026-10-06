package studio.luma.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import studio.luma.*

@Composable internal fun SmartProposalDialog(state:StudioUi,vm:StudioViewModel) {
    val proposal=state.smartProposal ?: return
    var before by remember(proposal) {mutableStateOf(false)}
    var reference by remember(proposal) {mutableStateOf(false)}
    AlertDialog(onDismissRequest=vm::discardSmartProposal,title={Text(proposal.title)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        val bitmap=if(reference && proposal.reference!=null) proposal.reference else if(before) proposal.before else proposal.preview
        Image(bitmap.asImageBitmap(),if(before) "Antes da sugestão" else "Prévia da sugestão",Modifier.fillMaxWidth().height(240.dp).testTag("smart-preview"),contentScale=ContentScale.Fit)
        proposal.reference?.let {TextButton(onClick={reference=!reference;before=false}) {Text(if(reference) "Ver sugestão" else "Ver referência")}}
        TextButton(onClick={before=!before;reference=false}) {Text(if(before) "Ver sugestão" else "Ver antes")}
        Text(proposal.explanation,fontSize=12.sp)
        Text(if(proposal.reference!=null) "A sugestão estima cores e contraste; não identifica o filtro original. Curvas, HSL e intensidade do LUT continuam disponíveis em Cor Pro." else "O enquadramento usa a foto original e mantém seus retoques. Confira o resultado; depois de aplicar, você poderá ajustar e desfazer.",fontSize=11.sp,color=Muted)
    }},confirmButton={TextButton(onClick=vm::applySmartProposal) {Text("Aplicar sugestão")}},dismissButton={TextButton(onClick=vm::discardSmartProposal) {Text("Cancelar")}})
}

@Composable internal fun SmartCropFormatDialog(currentAspect:Float,vm:StudioViewModel,dismiss:()->Unit) {
    AlertDialog(onDismissRequest=dismiss,title={Text("Formato do recorte inteligente")},text={Column {
        Text("A análise é feita no celular. Confira a prévia antes de aplicar.",fontSize=12.sp,color=Muted)
        listOf("Formato atual" to currentAspect,"Story · 9:16" to 9f/16,"Feed · 4:5" to .8f,"Quadrado · 1:1" to 1f,"Paisagem · 1.91:1" to 1.91f).forEach {(label,ratio)->
            TextButton(onClick={dismiss();vm.proposeSmartCrop(ratio)},modifier=Modifier.fillMaxWidth()) {Text(label)}
        }
    }},confirmButton={TextButton(onClick=dismiss) {Text("Cancelar")}})
}

@Composable internal fun SmartStoryControls(state:StudioUi,vm:StudioViewModel) {
    val doc=state.edit.story ?: return
    val selected=doc.layers.firstOrNull {it.id==state.selectedLayer}
    val slots=doc.layers.count {it.type=="photo" && it.visible && !it.locked}
    Column(Modifier.padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        TextButton(onClick={vm.proposeSmartCrop(layerId=selected?.id)},enabled=selected?.let {it.type=="photo" && it.asset!=null && it.visible && !it.locked}==true) {Text("Enquadrar foto selecionada")}
        TextButton(onClick={vm.proposeSmartLayout()},enabled=slots in 2..20 && doc.layers.any {it.type=="photo" && it.asset!=null && it.visible && !it.locked}) {Text("Organizar fotos")}
        Text("A organização escolhe as molduras para suas fotos e ajusta o enquadramento. Camadas bloqueadas e ocultas são preservadas.",fontSize=11.sp,color=Muted)
    }
}
