@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package studio.luma.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import studio.luma.*
import studio.luma.core.*
import kotlin.math.*

private val storyColors=listOf(0xFFFFFFFF.toInt(),0xFF151518.toInt(),0xFFE8E3D7.toInt(),0xFFD5FF69.toInt(),0xFFB7A9F5.toInt(),0xFFEA8EA8.toInt(),0xFF668A92.toInt(),0xFF846854.toInt(),0)

@Composable fun StoryEditor(state:StudioUi,vm:StudioViewModel,back:()->Unit) {
    val story=state.edit.story ?: return
    var guided by rememberSaveable(state.project?.id) {mutableStateOf(false)}
    if(guided) {StoryGuide(state,vm,back) {guided=false};return}
    val scrollLayout=useScrollableEditor()
    val selected=story.layers.firstOrNull { it.id==state.selectedLayer }
    var tool by remember { mutableStateOf("Criar") }
    var layers by remember { mutableStateOf(false) }
    var properties by remember { mutableStateOf(false) }
    var textLayer by remember { mutableStateOf<StoryLayer?>(null) }
    var stickers by remember { mutableStateOf(false) }
    var shapes by remember { mutableStateOf(false) }
    var export by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var saveTemplate by remember {mutableStateOf(false)}
    var music by remember {mutableStateOf(false)}
    var resources by remember {mutableStateOf(false)}
    var customCanvas by remember {mutableStateOf(false)}
    var guides by remember { mutableStateOf(false) }
    var replace by remember { mutableStateOf<String?>(null) }
    val context=LocalContext.current
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris -> vm.addStoryPhotos(uris,replace); replace=null }
    fun photos(id:String?=null) { replace=id; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().then(if(scrollLayout) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack,"Voltar") }
            Column(Modifier.weight(1f).clickable {rename=true}) {
                Text(state.project?.name ?: "Meu Story",fontSize=16.sp,maxLines=2)
                Text(draftLabel(state),color=Muted,fontSize=14.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick={guided=true},modifier=Modifier.weight(1f).testTag("story-return-guide")) {Text("Passo a passo")}
            IconButton(onClick={vm.undo()},enabled=state.canUndo) {Icon(Icons.AutoMirrored.Rounded.Undo,"Desfazer")}
            IconButton(onClick={vm.redo()},enabled=state.canRedo) {Icon(Icons.AutoMirrored.Rounded.Redo,"Refazer")}
            IconButton(onClick={vm.save()}) {Icon(Icons.Rounded.Save,"Salvar projeto",tint=Acid)}
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Text(if(story.missingPhotos>0) "Faltam ${story.missingPhotos} fotos" else "Arraste ou use Ajustar camada",color=Muted,fontSize=14.sp,modifier=Modifier.weight(1f))
            Row {
                IconButton(onClick={guides=!guides},modifier=Modifier.size(48.dp)) {Icon(Icons.Rounded.GridOn,"Guias de composição",tint=if(guides) Acid else Muted,modifier=Modifier.size(20.dp))}
                IconButton(onClick={layers=true},modifier=Modifier.size(48.dp)) {Icon(Icons.Rounded.Layers,"Camadas",modifier=Modifier.size(20.dp))}
            }
        }
        val previewModifier=(if(scrollLayout) Modifier.height(280.dp) else Modifier.weight(1f)).fillMaxWidth().padding(12.dp)
        if(tool=="Desenho") DrawingSurface(state,vm,previewModifier) else StoryCanvas(state,vm,guides,previewModifier)
        if(selected!=null) LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            item { TextButton(onClick={properties=true}) {Text("Ajustar ${layerName(selected)}",fontSize=14.sp)} }
            if(selected.type=="text" || selected.type=="sticker") item {TextButton(onClick={textLayer=selected},enabled=!selected.locked) {Text("Editar texto",fontSize=14.sp)}}
            if(selected.type=="photo") {
                item {TextButton(onClick={vm.openMasks(selected.id)},enabled=!selected.locked && selected.asset!=null) {Text("Máscaras da foto",fontSize=14.sp)}}
                item {TextButton(onClick={photos(selected.id)},enabled=!selected.locked) {Text("Trocar foto",fontSize=14.sp)}}
            }
            item {TextButton(onClick={vm.duplicateLayer(selected.id)}) {Text("Duplicar",fontSize=14.sp)}}
        }
        LazyRow(Modifier.testTag("story-tools"),contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            items(listOf("Criar")+(if(story.collage!=null) listOf("Colagem") else emptyList())+listOf("Inteligente","Canvas","Looks","Ajustes","Efeitos","Estilos","Avançado","Desenho")) { label -> FilterChip(tool==label,{tool=label},label={Text(label,fontSize=14.sp)}) }
        }
        Box(Modifier.fillMaxWidth().heightIn(min=110.dp,max=if(scrollLayout) 520.dp else 340.dp)) {
            when(tool) {
                "Criar" -> Column(Modifier.padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    LazyRow(Modifier.testTag("story-create-actions"),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        item {OutlinedButton(onClick={photos()}) {Text(if(story.missingPhotos>0) "Preencher fotos" else "+ Fotos",fontSize=14.sp)}}
                        item {OutlinedButton(onClick={vm.addLayer("text","Seu texto"); textLayer=vm.ui.value.edit.story?.layers?.lastOrNull()}) {Text("+ Texto",fontSize=14.sp)}}
                        item {OutlinedButton(onClick={stickers=true}) {Text("+ Stickers",fontSize=14.sp)}}
                        item {OutlinedButton(onClick={shapes=true}) {Text("+ Forma",fontSize=14.sp)}}
                        item {OutlinedButton(onClick={music=true}) {Text("+ Música",fontSize=14.sp)}}
                        item {OutlinedButton(onClick={resources=true}) {Text("Meus recursos",fontSize=14.sp)}}
                        item {OutlinedButton(onClick={saveTemplate=true}) {Text("Salvar template",fontSize=14.sp)}}
                    }
                    Text("Fotos e PNGs da galeria mantêm seus originais. Textos e formas são editáveis.",color=Muted,fontSize=14.sp)
                }
                "Canvas" -> Column(Modifier.padding(horizontal=16.dp).verticalScroll(rememberScrollState())) {
                    CategoryRow(listOf("Story","Feed","Quadrado","Paisagem","Perfil","Wallpaper","Personalizado"),when {story.width==story.height->"Quadrado"; story.width>story.height->"Paisagem"; story.width*5==story.height*4->"Feed"; else->"Story"}) { format ->
                        if(format=="Personalizado") {customCanvas=true;return@CategoryRow}
                        val (w,h)=when(format) {"Feed"->1080 to 1350;"Quadrado","Perfil"->1080 to 1080;"Paisagem"->1920 to 1005;else->1080 to 1920}
                        vm.changeStory { doc -> doc.copy(width=w,height=h,layers=doc.layers.map { layer ->val t=layer.transform; layer.copy(transform=t.copy(x=(t.x*w/doc.width).coerceIn(-16384f,16384f),y=(t.y*h/doc.height).coerceIn(-16384f,16384f),width=(t.width*w/doc.width).coerceIn(8f,16384f),height=(t.height*h/doc.height).coerceIn(8f,16384f)))}) }
                    }
                    Text("Cor de fundo",color=Muted,fontSize=14.sp)
                    ColorChoices(story.background) { color->vm.changeStory {it.copy(background=color,gradient=null,backgroundAsset=null)} }
                    GradientControls(story.gradient) {gradient->vm.changeStory {it.copy(gradient=gradient,backgroundAsset=null)}}
                    val photo=story.layers.firstOrNull {it.id==state.selectedLayer && it.asset!=null} ?: story.layers.firstOrNull {it.asset!=null}
                    Row {TextButton(onClick={photo?.asset?.let {asset->vm.changeStory {it.copy(backgroundAsset=asset)}}},enabled=photo!=null) {Text("Fundo da foto")};TextButton(onClick={vm.changeStory {it.copy(backgroundAsset=null)}}) {Text("Remover foto do fundo")}}
                    if(story.backgroundAsset!=null) AdvancedSlider("Desfoque do fundo",story.backgroundBlur,0f..1f,{v->vm.changeStory(false) {it.copy(backgroundBlur=v)}},vm::finishGesture)
                    Text("Guias de composição: margens de 12%. Confira a prévia no app em que publicar.",color=Muted,fontSize=14.sp)
                }
                "Looks" -> StoryLooks(state,vm)
                "Ajustes" -> AdjustmentControls(state,vm)
                "Efeitos" -> EffectControls(state,vm)
                "Estilos" -> StyleControls(state,vm)
                "Avançado" -> AdvancedEntry(state,vm)
                "Desenho" -> Text("Pinte na composição. O desenho vira uma camada editável.",Modifier.padding(16.dp),color=Muted,fontSize=14.sp)
                "Colagem" -> CollageControls(state,vm)
                "Inteligente" -> SmartStoryControls(state,vm)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
            Button(onClick={export=true},enabled=story.missingPhotos==0 && !state.rendering,modifier=Modifier.weight(1f)) {Text("Exportar Story")}
            state.exported?.let { uri -> IconButton(onClick={context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type="image/*";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Compartilhar Story"))}) {Icon(Icons.Rounded.Share,"Compartilhar Story")} }
            if(state.rendering) LumaProgressIndicator(Modifier.size(20.dp),color=Acid,strokeWidth=2.dp)
        }
    }
    if(music) MusicDialog(vm) {music=false}
    if(resources) ResourceDialog(state,vm) {resources=false}
    if(customCanvas) CanvasSizeDialog(story,vm) {customCanvas=false}
    if(layers) LayerDialog(story,state.selectedLayer,vm,{layers=false}) { id -> vm.selectLayer(id); layers=false; properties=true }
    if(properties && selected!=null) LayerProperties(selected,vm) {properties=false}
    textLayer?.let { original -> TextDialog(original,state,vm,{textLayer=null}) { content,typography ->vm.changeLayer(original.id,{it.copy(content=content,typography=typography)}); textLayer=null} }
    if(stickers) AlertDialog(onDismissRequest={stickers=false},title={Text("Stickers")},text={Column {
        listOf(listOf("♡","★","✦","☾"),listOf("🌸","✨","🎧","🦋"),listOf("🎂","🎮","💿","🌿")).forEach { row -> Row {row.forEach { emoji ->TextButton(onClick={vm.addLayer("sticker",emoji); stickers=false},modifier=Modifier.weight(1f)) {Text(emoji,fontSize=26.sp)}}} }
        Text("Para usar seu próprio sticker PNG, adicione pela opção Fotos.",color=Muted,fontSize=14.sp)
    }},confirmButton={TextButton(onClick={stickers=false}) {Text("Fechar")}})
    if(shapes) AlertDialog(onDismissRequest={shapes=false},title={Text("Adicionar forma")},text={Column {listOf("Retângulo" to "rect","Círculo" to "circle","Linha" to "line","Seta" to "arrow").forEach { (name,shape)->TextButton(onClick={vm.addLayer("shape",shape=shape);shapes=false}) {Text(name)} }}},confirmButton={TextButton(onClick={shapes=false}) {Text("Fechar")}})
    if(export) ExportDialog({export=false},story=true,canvasSize=ExportSize(story.width,story.height)) {options->export=false;vm.export(options)}
    if(saveTemplate) SaveTemplateDialog(state,vm) {saveTemplate=false}
    if(rename) {
        var name by remember {mutableStateOf(state.project?.name ?: "Meu Story")}
        AlertDialog(onDismissRequest={rename=false},title={Text("Nome do projeto")},text={OutlinedTextField(name,{name=it.take(100)},singleLine=true)},confirmButton={TextButton(onClick={vm.save(name);rename=false}) {Text("Salvar")}},dismissButton={TextButton(onClick={rename=false}) {Text("Cancelar")}})
    }
}

private fun layerName(layer:StoryLayer)=when(layer.type) {"photo"->"foto";"text"->"texto";"sticker"->"sticker";"drawing"->"desenho";else->"forma"}
private fun hit(layer:StoryLayer,p:Offset):Boolean {
    val t=layer.transform;val cx=t.x+t.width/2;val cy=t.y+t.height/2;val a=-t.rotation*PI/180
    val x=(p.x-cx)*cos(a)-(p.y-cy)*sin(a)+t.width/2;val y=(p.x-cx)*sin(a)+(p.y-cy)*cos(a)+t.height/2
    return x>=0 && x<=t.width && y>=0 && y<=t.height
}

@Composable internal fun StoryCanvas(state:StudioUi,vm:StudioViewModel,guides:Boolean,modifier:Modifier) {
    val story=state.edit.story ?: return
    val latest by rememberUpdatedState(state)
    BoxWithConstraints(modifier,contentAlignment=Alignment.Center) {
        val ratio=story.width.toFloat()/story.height
        val width=minOf(maxWidth,maxHeight*ratio)
        Box(Modifier.width(width).height(width/ratio).background(Panel).pointerInput(story.width,story.height) {
            awaitEachGesture {
                val down=awaitFirstDown(requireUnconsumed=false)
                val scale=size.width.toFloat()/story.width
                val point=down.position/scale
                val chosen=latest.edit.story?.layers?.asReversed()?.firstOrNull {it.visible && hit(it,point)}
                vm.selectLayer(chosen?.id);down.consume()
                var changed=false
                do {
                    val event=awaitPointerEvent()
                    if(chosen!=null && !chosen.locked && event.changes.any {it.pressed}) {
                        val pan=event.calculatePan()/scale; val zoom=event.calculateZoom();val rotation=event.calculateRotation()
                        if(pan.getDistance()>.01f || abs(zoom-1f)>.0001f || abs(rotation)>.01f) {
                            vm.changeLayer(chosen.id,{ layer ->
                                val t=layer.transform;val w=(t.width*zoom).coerceIn(8f,16384f);val h=(t.height*zoom).coerceIn(8f,16384f)
                                layer.copy(transform=t.copy(x=(t.x+pan.x+(t.width-w)/2).coerceIn(-16384f,16384f),y=(t.y+pan.y+(t.height-h)/2).coerceIn(-16384f,16384f),width=w,height=h,rotation=((t.rotation+rotation+540f)%360f)-180f))
                            },false);changed=true
                        }
                    }
                    event.changes.forEach {it.consume()}
                } while(event.changes.any {it.pressed})
                if(changed && chosen!=null) vm.snapLayer(chosen.id)
            }
        }) {
            state.preview?.let {Image(it.asImageBitmap(),"Composição do Story",Modifier.fillMaxSize().semantics {customActions=story.layers.filter {it.visible}.mapIndexed {index,layer->CustomAccessibilityAction("Selecionar ${layerName(layer)} ${index+1}") {vm.selectLayer(layer.id);true}}})}
            Canvas(Modifier.fillMaxSize()) {
                val s=size.width/story.width
                if(guides) {
                    drawRect(Color.White.copy(alpha=.5f),Offset(size.width*.06f,size.height*.12f),Size(size.width*.88f,size.height*.76f),style=Stroke(1f))
                    drawLine(Color.White.copy(alpha=.3f),Offset(size.width/2,0f),Offset(size.width/2,size.height),1f)
                }
                story.layers.firstOrNull {it.id==state.selectedLayer && it.visible}?.let {layer ->val t=layer.transform
                    rotate(t.rotation,Offset((t.x+t.width/2)*s,(t.y+t.height/2)*s)) {
                        drawRect(if(layer.locked) Lilac else Acid,Offset(t.x*s,t.y*s),Size(t.width*s,t.height*s),style=Stroke(2f))
                    }
                }
            }
        }
    }
}

@Composable private fun LayerDialog(story:StoryDocument,selected:String?,vm:StudioViewModel,dismiss:()->Unit,choose:(String)->Unit) {
    AlertDialog(onDismissRequest=dismiss,title={Text("Camadas · topo primeiro")},text={LazyColumn(Modifier.heightIn(max=430.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(story.layers.isEmpty()) item {Text("Adicione fotos, textos ou formas.")}
        items(story.layers.asReversed(),key={it.id}) {layer ->Column(Modifier.background(if(layer.id==selected) Panel else Color.Transparent)) {
            TextButton(onClick={choose(layer.id)}) {Text("${layerName(layer).replaceFirstChar {it.uppercase()}} · ${layer.content.take(24).ifBlank {if(layer.asset==null && layer.type=="photo") "Preencher foto" else layer.shape}}",maxLines=1,fontSize=14.sp)}
            LazyRow {
                item { Row {
                IconButton(onClick={vm.changeLayer(layer.id,{it.copy(visible=!it.visible)})},modifier=Modifier.size(48.dp)) {Icon(if(layer.visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,"Mostrar camada")}
                IconButton(onClick={vm.changeLayer(layer.id,{it.copy(locked=!it.locked)})},modifier=Modifier.size(48.dp)) {Icon(if(layer.locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,"Bloquear camada")}
                IconButton(onClick={vm.moveLayer(layer.id,1)},modifier=Modifier.size(48.dp)) {Icon(Icons.Rounded.KeyboardArrowUp,"Subir camada")}
                IconButton(onClick={vm.moveLayer(layer.id,-1)},modifier=Modifier.size(48.dp)) {Icon(Icons.Rounded.KeyboardArrowDown,"Descer camada")}
                IconButton(onClick={vm.duplicateLayer(layer.id)},modifier=Modifier.size(48.dp)) {Icon(Icons.Rounded.ContentCopy,"Duplicar camada")}
                IconButton(onClick={vm.deleteLayer(layer.id)},enabled=!layer.locked,modifier=Modifier.size(48.dp)) {Icon(Icons.Rounded.DeleteOutline,"Excluir camada")}
                } }
            }
        }}
    }},confirmButton={TextButton(onClick=dismiss) {Text("Pronto")}})
}

@Composable internal fun LayerProperties(layer:StoryLayer,vm:StudioViewModel,dismiss:()->Unit) {
    val t=layer.transform
    fun changeTransform(commit:Boolean=false,change:(LayerTransform)->LayerTransform) {vm.changeLayer(layer.id,{it.copy(transform=change(it.transform))},commit)}
    AlertDialog(onDismissRequest=dismiss,title={Text("Ajustar ${layerName(layer)}")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(layer.locked,{vm.changeLayer(layer.id,{l->l.copy(locked=it)})});Text("Bloquear posição e edição",fontSize=14.sp)}
        if(!layer.locked) {
            Text("Mover em pequenos passos",fontSize=16.sp)
            LazyRow {
                item {IconButton(onClick={changeTransform(true) {it.copy(x=(it.x-10).coerceAtLeast(-16384f))}}) {Icon(Icons.AutoMirrored.Rounded.ArrowBack,"Mover 10 pixels para a esquerda")}}
                item {IconButton(onClick={changeTransform(true) {it.copy(x=(it.x+10).coerceAtMost(16384f))}}) {Icon(Icons.AutoMirrored.Rounded.ArrowForward,"Mover 10 pixels para a direita")}}
                item {IconButton(onClick={changeTransform(true) {it.copy(y=(it.y-10).coerceAtLeast(-16384f))}}) {Icon(Icons.Rounded.ArrowUpward,"Mover 10 pixels para cima")}}
                item {IconButton(onClick={changeTransform(true) {it.copy(y=(it.y+10).coerceAtMost(16384f))}}) {Icon(Icons.Rounded.ArrowDownward,"Mover 10 pixels para baixo")}}
            }
            PropertySlider("Posição horizontal",t.x,-16384f..16384f,{v->changeTransform {it.copy(x=v)}},vm::finishGesture)
            PropertySlider("Posição vertical",t.y,-16384f..16384f,{v->changeTransform {it.copy(y=v)}},vm::finishGesture)
            CategoryRow(listOf("Esquerda","Centro","Direita"),"") {vm.alignLayer(layer.id,horizontal=when(it) {"Esquerda"->"left";"Direita"->"right";else->"center"})}
            CategoryRow(listOf("Topo","Meio","Base"),"") {vm.alignLayer(layer.id,vertical=when(it) {"Topo"->"top";"Base"->"bottom";else->"center"})}
            PropertySlider("Opacidade",t.opacity,0f..1f,{v->changeTransform {it.copy(opacity=v)}},vm::finishGesture)
            PropertySlider("Rotação",t.rotation,-180f..180f,{v->changeTransform {it.copy(rotation=v)}},vm::finishGesture)
            PropertySlider("Largura",t.width,8f..4096f,{v->changeTransform {it.copy(width=v)}},vm::finishGesture)
            PropertySlider("Altura",t.height,8f..4096f,{v->changeTransform {it.copy(height=v)}},vm::finishGesture)
            Text("Mistura",color=Muted,fontSize=14.sp)
            CategoryRow(listOf("normal","multiply","screen","overlay"),layer.blendMode) {value->vm.changeLayer(layer.id,{it.copy(blendMode=value)})}
            if(layer.type=="photo") {
                CategoryRow(listOf("Preencher","Foto inteira"),if(layer.photoFit=="contain") "Foto inteira" else "Preencher") {value->vm.changeLayer(layer.id,{it.copy(photoFit=if(value=="Foto inteira") "contain" else "cover",photoZoom=1f,photoPanX=0f,photoPanY=0f)})}
                PropertySlider("Zoom da foto",layer.photoZoom,1f..4f,{v->vm.changeLayer(layer.id,{it.copy(photoZoom=v)},false)},vm::finishGesture)
                PropertySlider("Foto · horizontal",layer.photoPanX,-1f..1f,{v->vm.changeLayer(layer.id,{it.copy(photoPanX=v)},false)},vm::finishGesture)
                PropertySlider("Foto · vertical",layer.photoPanY,-1f..1f,{v->vm.changeLayer(layer.id,{it.copy(photoPanY=v)},false)},vm::finishGesture)
            }
            if(layer.type=="shape") {Text("Cor",fontSize=14.sp);ColorChoices(layer.color) {color->vm.changeLayer(layer.id,{it.copy(color=color)})};GradientControls(layer.gradient) {gradient->vm.changeLayer(layer.id,{it.copy(gradient=gradient)})}}
        }
    }},confirmButton={TextButton(onClick=dismiss) {Text("Pronto")}})
}

@Composable private fun PropertySlider(name:String,value:Float,range:ClosedFloatingPointRange<Float>,change:(Float)->Unit,finish:()->Unit={}) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {Text(name,color=Muted,fontSize=14.sp,modifier=Modifier.weight(1f));Text("%.2f".format(value),fontSize=14.sp,color=Acid)}
    Slider(value.coerceIn(range),change,valueRange=range,onValueChangeFinished=finish,modifier=Modifier.heightIn(min=48.dp).semantics {contentDescription=name})
}

@Composable private fun ColorChoices(selected:Int,choose:(Int)->Unit) {
    LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=8.dp)) {
        items(storyColors) { color ->Box(Modifier.size(48.dp).background(if(color==0) Panel else Color(color)).border(if(color==selected) 2.dp else 1.dp,if(color==selected) Acid else Muted).clickable {choose(color)}.semantics {contentDescription=colorName(color);this.selected=color==selected;role=Role.RadioButton},contentAlignment=Alignment.Center) {if(color==0) Text("∅",color=Muted)} }
    }
}

@Composable private fun StoryLooks(state:StudioUi,vm:StudioViewModel) {
    var query by remember {mutableStateOf("")};var category by remember {mutableStateOf("Todos")}
    val presets=state.filters.filter {(category=="Todos" || category=="Favoritos" && it.meta.id in state.favorites || category==it.meta.category) && catalogMatches(it.meta,query)}
    Column(Modifier.padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("Look aplicado às fotos da composição",color=Muted,fontSize=14.sp)
        OutlinedTextField(query,{query=it.take(100)},Modifier.fillMaxWidth().testTag("story-look-search"),singleLine=true,placeholder={Text("Buscar filtros",fontSize=14.sp)})
        CatalogCategories(listOf("Todos","Favoritos","Instagram","JDM")+state.filters.map {it.meta.category},category,{category=it},"story-look-categories")
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            item {FilterChip(state.edit.filter==null,{vm.applyFilter(null)},label={Text("Original",fontSize=14.sp)})}
            items(presets,key={it.meta.id}) {preset->FilterChip(state.edit.filter?.preset?.meta?.id==preset.meta.id,{vm.applyFilter(preset)},label={Text(preset.meta.name,fontSize=14.sp)})}
        }
        state.edit.filter?.let {selection ->PropertySlider("Intensidade",selection.intensity,0f..1f,{v->vm.update(state.edit.copy(filter=selection.copy(intensity=v)),false)},vm::finishGesture)}
    }
}

@Composable internal fun TextDialog(layer:StoryLayer,state:StudioUi,vm:StudioViewModel,dismiss:()->Unit,simple:Boolean=false,apply:(String,studio.luma.core.Typography)->Unit) {
    var content by remember(layer.id) {mutableStateOf(layer.content)}
    var style by remember(layer.id) {mutableStateOf(layer.typography)}
    var advanced by rememberSaveable(layer.id) {mutableStateOf(!simple)}
    AlertDialog(onDismissRequest=dismiss,title={Text("Texto e tipografia")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(content,{content=it.take(2000)},label={Text("Texto")},modifier=Modifier.fillMaxWidth().testTag("story-text-field"),minLines=2,maxLines=5)
        if(simple) {
            PropertySlider("Tamanho do texto",style.size,8f..400f,{style=style.copy(size=it)})
            Text("Cor do texto",fontSize=16.sp);ColorChoices(style.color) {style=style.copy(color=it)}
            TextButton(onClick={advanced=!advanced}) {Text(if(advanced) "Ocultar opções de texto" else "Mais opções de texto")}
        }
        if(advanced) {
        Text("Estilos",color=Muted,fontSize=14.sp)
        CategoryRow(listOf("Minimal","Editorial","Magazine","Film","Y2K","Romantic","Cinematic","Typewriter","Retro","Luxury","Grunge","Handwritten"),"") {preset ->
            style=style.copy(family=when(preset) {"Editorial","Romantic","Luxury","Retro"->"serif";"Typewriter","Film"->"monospace";"Cinematic","Magazine","Grunge"->"sans-serif-condensed";"Handwritten"->"cursive";else->"sans-serif"},weight=if(preset in listOf("Magazine","Y2K","Grunge")) 800 else 400,spacing=if(preset in listOf("Cinematic","Luxury")) .15f else 0f,stroke=if(preset=="Y2K") 2f else 0f,shadow=if(preset=="Grunge") 5f else 0f)
        }
        CategoryRow(listOf("sans-serif","serif","monospace","sans-serif-condensed","cursive"),style.family) {style=style.copy(family=it,fontAsset=null)}
        PropertySlider("Tamanho",style.size,8f..400f,{style=style.copy(size=it)})
        PropertySlider("Peso",style.weight.toFloat(),100f..900f,{style=style.copy(weight=it.roundToInt())})
        CategoryRow(listOf("left","center","right"),style.alignment) {style=style.copy(alignment=it)}
        PropertySlider("Espaçamento de letras",style.spacing,-.1f.. .5f,{style=style.copy(spacing=it)})
        PropertySlider("Entrelinhas",style.lineSpacing,.7f..2f,{style=style.copy(lineSpacing=it)})
        Text("Cor do texto",color=Muted,fontSize=14.sp);ColorChoices(style.color) {style=style.copy(color=it)}
        PropertySlider("Contorno",style.stroke,0f..12f,{style=style.copy(stroke=it)})
        if(style.stroke>0) {Text("Cor do contorno",color=Muted,fontSize=14.sp);ColorChoices(style.strokeColor) {style=style.copy(strokeColor=it)}}
        GradientControls(style.gradient) {style=style.copy(gradient=it)}
        PropertySlider("Brilho ao redor",style.glow,0f..40f,{style=style.copy(glow=it)})
        PropertySlider("Desfoque do texto",style.blur,0f..20f,{style=style.copy(blur=it)})
        PropertySlider("Curvatura",style.curvature,-1f..1f,{style=style.copy(curvature=it)})
        Text("Fontes importadas",color=Muted,fontSize=14.sp)
        LazyRow {items(state.library.resources.filter {it.kind=="font"}) {font->TextButton(onClick={vm.prepareResource(font);style=style.copy(fontAsset=font.asset)}) {Text(font.meta.name,fontSize=14.sp)}}}
        TextButton(onClick={style=style.copy(fontAsset=null)}) {Text("Usar fonte do Android")}
        PropertySlider("Sombra",style.shadow,0f..30f,{style=style.copy(shadow=it)})
        Text("Fundo do texto",color=Muted,fontSize=14.sp);ColorChoices(style.background) {style=style.copy(background=it)}
        Text("Fontes disponíveis no Android. A caixa da camada limita o texto; ajuste sua altura se necessário.",fontSize=14.sp,color=Muted)
        }
    }},confirmButton={TextButton(onClick={apply(content,style)}) {Text("Aplicar")}},dismissButton={TextButton(onClick=dismiss) {Text("Cancelar")}})
}

@Composable private fun CanvasSizeDialog(story:StoryDocument,vm:StudioViewModel,dismiss:()->Unit) {
    var width by remember {mutableStateOf(story.width.toString())};var height by remember {mutableStateOf(story.height.toString())}
    val w=width.toIntOrNull() ?: 0;val h=height.toIntOrNull() ?: 0
    AlertDialog(onDismissRequest=dismiss,title={Text("Canvas personalizado")},text={Column {OutlinedTextField(width,{width=it.filter(Char::isDigit).take(4)},label={Text("Largura")});OutlinedTextField(height,{height=it.filter(Char::isDigit).take(4)},label={Text("Altura")});Text("64–8192 px por lado · até 32 MP",color=Muted,fontSize=14.sp)}},confirmButton={TextButton(onClick={vm.changeStory {doc->doc.copy(width=w,height=h,layers=doc.layers.map {l->val t=l.transform;l.copy(transform=t.copy(x=t.x*w/doc.width,y=t.y*h/doc.height,width=(t.width*w/doc.width).coerceIn(8f,16384f),height=(t.height*h/doc.height).coerceIn(8f,16384f)))})};dismiss()},enabled=w in 64..8192 && h in 64..8192 && w.toLong()*h<=32_000_000) {Text("Aplicar")}},dismissButton={TextButton(onClick=dismiss) {Text("Cancelar")}})
}
