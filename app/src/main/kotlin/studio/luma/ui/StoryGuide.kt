package studio.luma.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import studio.luma.*
import studio.luma.core.*

@Composable internal fun StoryGuide(state:StudioUi,vm:StudioViewModel,back:()->Unit,fullEditor:()->Unit) {
    val story=state.edit.story ?: return
    var step by rememberSaveable(state.project?.id) {mutableIntStateOf(0)}
    var replace by rememberSaveable {mutableStateOf<String?>(null)}
    var textId by rememberSaveable {mutableStateOf<String?>(null)}
    var propertiesId by rememberSaveable {mutableStateOf<String?>(null)}
    var exporting by rememberSaveable {mutableStateOf(false)}
    var more by remember {mutableStateOf(false)}
    var help by remember {mutableStateOf(false)}
    var discard by remember {mutableStateOf(false)}
    val scroll=rememberScrollState()
    val context=LocalContext.current
    val single=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {uri->
        val target=replace;replace=null
        if(uri!=null && target!=null) vm.addStoryPhotos(listOf(uri),target)
    }
    val multiple=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) {uris->vm.addStoryPhotos(uris)}
    fun choosePhoto(id:String) {replace=id;single.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}
    val photos=story.layers.filter {it.type=="photo" && it.visible}
    val texts=story.layers.filter {it.type=="text" && it.visible}
    val title=listOf("Adicione suas fotos","Edite seus textos","Confira e salve")[step]
    LaunchedEffect(step) {scroll.scrollTo(0)}
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(scroll).testTag("story-guide").padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=back,modifier=Modifier.size(48.dp)) {Icon(Icons.AutoMirrored.Rounded.ArrowBack,"Guardar rascunho e voltar ao início")}
            Text("Criar Story",fontSize=22.sp,modifier=Modifier.weight(1f).semantics {heading()})
            Box {
                IconButton(onClick={more=true},modifier=Modifier.size(48.dp)) {Icon(Icons.Rounded.MoreVert,"Mais opções do Story")}
                DropdownMenu(more,{more=false}) {
                    DropdownMenuItem(text={Text("Todas as ferramentas")},onClick={more=false;fullEditor()})
                    DropdownMenuItem(text={Text("Salvar projeto editável")},onClick={more=false;vm.save()})
                    DropdownMenuItem(text={Text("Facilidade de uso")},onClick={more=false;help=true})
                    DropdownMenuItem(text={Text("Descartar rascunho")},enabled=state.dirty,onClick={more=false;discard=true})
                }
            }
        }
        Text("Passo ${step+2} de 4 · $title",fontSize=18.sp,color=Acid,modifier=Modifier.testTag("story-guide-step").semantics {heading();liveRegion=LiveRegionMode.Polite})
        Text(draftLabel(state),fontSize=14.sp,color=Muted,modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
            IconButton(onClick=vm::undo,enabled=state.canUndo,modifier=Modifier.size(48.dp)) {Icon(Icons.AutoMirrored.Rounded.Undo,"Desfazer")}
            IconButton(onClick=vm::redo,enabled=state.canRedo,modifier=Modifier.size(48.dp)) {Icon(Icons.AutoMirrored.Rounded.Redo,"Refazer")}
        }
        StoryCanvas(state,vm,false,Modifier.fillMaxWidth().height(240.dp).testTag("story-guide-preview"))
        when(step) {
            0->{
                Text(if(story.missingPhotos>0) "Faltam ${story.missingPhotos} fotos para completar o modelo." else if(photos.isEmpty()) "Adicione fotos ou continue para criar um Story só com texto." else "Suas fotos estão no modelo. Você pode trocar ou ajustar cada uma.",fontSize=16.sp)
                Button(onClick={multiple.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},enabled=!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("story-guide-add-photos")) {Text(if(story.missingPhotos>0) "Escolher fotos para o modelo" else "Adicionar fotos",fontSize=16.sp)}
                photos.forEachIndexed {index,photo->
                    Column(Modifier.fillMaxWidth().background(Panel).padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text("Foto ${index+1} · ${if(photo.asset==null) "falta adicionar" else "adicionada"}",fontSize=16.sp)
                        if(!photo.locked) {
                            OutlinedButton(onClick={choosePhoto(photo.id)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("story-guide-photo-${index+1}")) {Text(if(photo.asset==null) "Adicionar foto ${index+1}" else "Trocar foto ${index+1}")}
                            if(photo.asset!=null) TextButton(onClick={vm.selectLayer(photo.id);propertiesId=photo.id},modifier=Modifier.heightIn(min=48.dp)) {Text("Ajustar enquadramento da foto ${index+1}")}
                        } else Text("Foto bloqueada. Desbloqueie em Todas as ferramentas para alterá-la.",fontSize=14.sp,color=Muted)
                    }
                }
            }
            1->{
                Text("Toque em um texto para mudar a mensagem. Você pode manter os textos do modelo ou continuar sem adicionar outro.",fontSize=16.sp)
                texts.forEachIndexed {index,text->
                    OutlinedButton(onClick={vm.selectLayer(text.id);textId=text.id},enabled=!text.locked,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("story-guide-text-${index+1}")) {
                        Column(Modifier.fillMaxWidth()) {Text("Editar texto ${index+1}",fontSize=16.sp);Text(text.content.ifBlank {"Sem texto"},fontSize=14.sp,maxLines=3,color=Muted)}
                    }
                }
                Button(onClick={vm.addLayer("text","Seu texto");textId=vm.ui.value.selectedLayer},enabled=story.layers.size<80,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("story-guide-add-text")) {Text("Adicionar texto",fontSize=16.sp)}
            }
            2->{
                Text("Confira sua composição. Salvar cria uma imagem na galeria; o rascunho continua editável em Projetos.",fontSize=16.sp)
                if(story.missingPhotos>0) {
                    Text("Faltam ${story.missingPhotos} fotos. Volte à etapa Fotos antes de salvar.",fontSize=16.sp,color=MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick={step=0},modifier=Modifier.fillMaxWidth()) {Text("Completar fotos")}
                }
                Button(onClick={exporting=true},enabled=story.missingPhotos==0 && !state.rendering && !state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("story-guide-save")) {Text("Salvar Story na galeria",fontSize=16.sp)}
                state.exported?.let {uri->
                    Text("Story salvo na galeria, na pasta Luma Studio.",fontSize=16.sp,modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})
                    OutlinedButton(onClick={context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type="image/*";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Compartilhar Story"))},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Compartilhar Story")}
                }
            }
        }
        if(step<2) Button(onClick={vm.finishGesture();step++},enabled=step!=0 || story.missingPhotos==0,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("story-guide-next")) {Text(if(step==0) "Continuar para textos" else "Revisar e salvar",fontSize=16.sp)}
        if(step>0) OutlinedButton(onClick={vm.finishGesture();step--},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("story-guide-back")) {Text(if(step==1) "Voltar às fotos" else "Voltar aos textos")}
        TextButton(onClick=fullEditor,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("story-guide-full")) {Text("Todas as ferramentas",fontSize=16.sp)}
        Spacer(Modifier.height(16.dp))
    }
    story.layers.firstOrNull {it.id==textId && !it.locked}?.let {layer->TextDialog(layer,state,vm,{textId=null},simple=true) {content,type->vm.changeLayer(layer.id,{it.copy(content=content,typography=type)});textId=null}}
    story.layers.firstOrNull {it.id==propertiesId}?.let {layer->LayerProperties(layer,vm) {propertiesId=null}}
    if(exporting) ExportDialog({exporting=false},true,ExportSize(story.width,story.height)) {options->exporting=false;vm.export(options)}
    if(help) AccessibilityDialog {help=false}
    if(discard) AlertDialog(onDismissRequest={discard=false},title={Text("Descartar alterações?")},text={Text("O rascunho será removido. O último projeto salvo e a foto original serão mantidos.")},confirmButton={TextButton(onClick={discard=false;vm.closeEditor(true)}) {Text("Descartar alterações")}},dismissButton={TextButton(onClick={discard=false}) {Text("Continuar editando")}})
}
