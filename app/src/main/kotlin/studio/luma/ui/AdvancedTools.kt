package studio.luma.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import androidx.compose.ui.semantics.*
import studio.luma.*
import studio.luma.core.*
import studio.luma.data.*

@Composable internal fun AdvancedEntry(state:StudioUi,vm:StudioViewModel) {
    var color by remember {mutableStateOf(false)};var resources by remember {mutableStateOf(false)}
    val reference=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {it?.let(vm::reference)}
    LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        item {OutlinedButton(onClick={color=true}) {Text("Cor Pro")}}
        item {OutlinedButton(onClick={reference.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}) {Text("Referência")}}
        item {OutlinedButton(onClick={resources=true}) {Text("Meus recursos")}}
        item {TextButton(onClick={vm.surprise()}) {Text("Surpreenda-me")}}
    }
    if(color) ColorDialog(state.edit.color,{color=false},vm::finishGesture,state.preview) {value,commit->vm.update(state.edit.copy(color=value),commit)}
    if(resources) ResourceDialog(state,vm) {resources=false}
}

@Composable internal fun ColorDialog(color:ColorTreatment,dismiss:()->Unit,finish:()->Unit,preview:android.graphics.Bitmap?=null,change:(ColorTreatment,Boolean)->Unit) {
    var tab by remember {mutableStateOf("Curvas")};var channel by remember {mutableStateOf("RGB")};var band by remember {mutableIntStateOf(0)};var tone by remember {mutableStateOf("Sombras")}
    AlertDialog(onDismissRequest=dismiss,title={Text("Cor Pro")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        preview?.let {Image(it.asImageBitmap(),"Prévia da cor",Modifier.fillMaxWidth().height(130.dp))}
        CategoryRow(listOf("Curvas","HSL","Gradação","LUT"),tab) {tab=it}
        when(tab) {
            "Curvas"->{
                CategoryRow(listOf("RGB","R","G","B"),channel) {channel=it}
                val points=when(channel) {"R"->color.curves.red;"G"->color.curves.green;"B"->color.curves.blue;else->color.curves.rgb}
                fun update(points:List<Point>,commit:Boolean=false) {val c=when(channel) {"R"->color.curves.copy(red=points);"G"->color.curves.copy(green=points);"B"->color.curves.copy(blue=points);else->color.curves.copy(rgb=points)};change(color.copy(curves=c),commit)}
                Canvas(Modifier.fillMaxWidth().height(140.dp).background(Panel).testTag("curve-graph")) {
                    for(i in 0..4) {val v=i/4f;drawLine(Muted.copy(alpha=.3f),Offset(size.width*v,0f),Offset(size.width*v,size.height));drawLine(Muted.copy(alpha=.3f),Offset(0f,size.height*v),Offset(size.width,size.height*v))}
                    points.zipWithNext().forEach {(a,b)->drawLine(Acid,Offset(a.x*size.width,(1-a.y)*size.height),Offset(b.x*size.width,(1-b.y)*size.height),3f)}
                }
                listOf("Pretos" to 0f,"Sombras" to .25f,"Meios-tons" to .5f,"Luzes" to .75f,"Brancos" to 1f).forEach {(label,x)->
                    AdvancedSlider(label,sampleCurve(points,x),0f..1f,{v->val grid=listOf(0f,.25f,.5f,.75f,1f).map {Point(it,if(it==x) v else sampleCurve(points,it))};update(grid)},finish)
                }
                TextButton(onClick={update(Curves().rgb,true)}) {Text("Redefinir canal")}
            }
            "HSL"->{
                val labels=listOf("Vermelho","Laranja","Amarelo","Verde","Ciano","Azul","Violeta","Magenta")
                CategoryRow(labels,labels[band]) {band=labels.indexOf(it)}
                val value=color.hsl[band]
                fun set(value:HslBand) {change(color.copy(hsl=color.hsl.mapIndexed {i,b->if(i==band) value else b}),false)}
                AdvancedSlider("Matiz",value.hue,-1f..1f,{set(value.copy(hue=it))},finish)
                AdvancedSlider("Saturação",value.saturation,-1f..1f,{set(value.copy(saturation=it))},finish)
                AdvancedSlider("Luminância",value.luminance,-1f..1f,{set(value.copy(luminance=it))},finish)
            }
            "Gradação"->{
                CategoryRow(listOf("Sombras","Meios-tons","Luzes","Balanço"),tone) {tone=it}
                val values=when(tone) {"Sombras"->color.shadows;"Meios-tons"->color.midtones;"Luzes"->color.highlights;else->color.balance}
                listOf("Vermelho / ciano","Verde / magenta","Azul / amarelo").forEachIndexed {index,label->AdvancedSlider(label,values[index],-1f..1f,{v->val next=values.mapIndexed {i,value->if(i==index) v else value};change(when(tone) {"Sombras"->color.copy(shadows=next);"Meios-tons"->color.copy(midtones=next);"Luzes"->color.copy(highlights=next);else->color.copy(balance=next)},false)},finish)}
                AdvancedSlider("Transição das altas luzes",color.rolloff,0f..1f,{change(color.copy(rolloff=it),false)},finish)
                AdvancedSlider("Luminância geral",color.luminance,-1f..1f,{change(color.copy(luminance=it),false)},finish)
            }
            "LUT"->{Text(color.lut?.name ?: "Importe um arquivo .cube em Meus recursos.",color=Muted);AdvancedSlider("Intensidade",color.lutIntensity,0f..1f,{change(color.copy(lutIntensity=it),false)},finish);TextButton(onClick={change(color.copy(lut=null),true)}) {Text("Remover LUT")}}
        }
        if(color.matrix!=ColorTreatment().matrix) {Text("Correspondência de referência aplicada",color=Acid,fontSize=14.sp);TextButton(onClick={change(color.copy(matrix=ColorTreatment().matrix),true)}) {Text("Remover correspondência")}}
        TextButton(onClick={change(ColorTreatment(),true)}) {Text("Limpar tratamento de cor")}
    }},confirmButton={TextButton(onClick={finish();dismiss()}) {Text("Pronto")}})
}
@Composable internal fun AdvancedSlider(label:String,value:Float,range:ClosedFloatingPointRange<Float>,change:(Float)->Unit,finish:()->Unit={}) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {Text(label,fontSize=14.sp,modifier=Modifier.weight(1f));Text("%.2f".format(value),fontSize=14.sp,color=Acid)}
    Slider(value.coerceIn(range),change,valueRange=range,onValueChangeFinished=finish,modifier=Modifier.heightIn(min=48.dp).testTag(label).semantics {contentDescription=label})
}
@Composable internal fun ResourceDialog(state:StudioUi,vm:StudioViewModel,dismiss:()->Unit) {
    var kind by remember {mutableStateOf("lut")};var name by remember {mutableStateOf("")};var remove by remember {mutableStateOf<LocalAsset?>(null)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {it?.let {uri->vm.importResource(uri,kind,name)}}
    AlertDialog(onDismissRequest=dismiss,title={Text("Meus recursos")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        CategoryRow(listOf("lut","font","sticker","texture","frame"),kind) {kind=it}
        OutlinedTextField(name,{name=it.take(100)},label={Text("Nome do recurso")},singleLine=true)
        Button(onClick={picker.launch(arrayOf("*/*"))}) {Text("Importar ${when(kind) {"font"->"TTF / OTF";"lut"->".cube 3D";else->"imagem"}}")}
        Text("Arquivos ficam no celular e podem entrar no seu pack. Selecione um texto para aplicar uma fonte.",color=Muted,fontSize=14.sp)
        LazyColumn(Modifier.heightIn(max=250.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {items(state.library.resources.filter {it.kind==kind},key={it.meta.id}) {resource->Column {Text(resource.meta.name);Row {if(state.project!=null) TextButton(onClick={vm.useResource(resource)}) {Text("Aplicar")};TextButton(onClick={remove=resource}) {Text("Excluir")}}}}}
    }},confirmButton={TextButton(onClick=dismiss) {Text("Fechar")}})
    remove?.let {resource->AlertDialog(onDismissRequest={remove=null},title={Text("Excluir ${resource.meta.name}?")},text={Text("Projetos que já usam o recurso guardam sua própria cópia.")},confirmButton={TextButton(onClick={vm.removeLibrary(resource.meta.id);remove=null}) {Text("Excluir")}},dismissButton={TextButton(onClick={remove=null}) {Text("Cancelar")}})}
}
@Composable internal fun BatchDialog(state:StudioUi,vm:StudioViewModel,dismiss:()->Unit) {
    var style by remember {mutableStateOf<StylePreset?>(null)};var uris by remember {mutableStateOf<List<android.net.Uri>>(emptyList())};var export by remember {mutableStateOf(false)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) {uris=it}
    AlertDialog(onDismissRequest=dismiss,title={Text("Exportar lote")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Até 20 fotos. Cada original vira um projeto editável.",color=Muted,fontSize=14.sp)
        Button(onClick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}) {Text("Selecionar fotos · ${uris.size}")}
        Text("Tratamento comum",fontSize=14.sp)
        LazyColumn(Modifier.heightIn(max=240.dp)) {item {TextButton(onClick={style=null}) {Text(if(style==null) "✓ Original" else "Original")}};items(state.styles.filter {it.layers.isEmpty()},key={it.meta.id}) {s->TextButton(onClick={style=s}) {Text((if(style==s) "✓ " else "")+s.meta.name)}}}
    }},confirmButton={TextButton(onClick={export=true},enabled=uris.isNotEmpty()) {Text("Formato e resolução")}},dismissButton={TextButton(onClick=dismiss) {Text("Cancelar")}})
    if(export) ExportDialog({export=false}) {options->vm.batch(uris,style,options);export=false;dismiss()}
}
@Composable internal fun MusicDialog(vm:StudioViewModel,dismiss:()->Unit) {
    var title by remember {mutableStateOf("")};var artist by remember {mutableStateOf("")};var lyrics by remember {mutableStateOf("")};var progress by remember {mutableFloatStateOf(.4f)}
    AlertDialog(onDismissRequest=dismiss,title={Text("Card de música")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(title,{title=it.take(100)},label={Text("Música")});OutlinedTextField(artist,{artist=it.take(100)},label={Text("Artista")});OutlinedTextField(lyrics,{lyrics=it.take(500)},label={Text("Trecho / tempo")})
        AdvancedSlider("Progresso",progress,.01f..1f,{progress=it})
        Text("Escolha a capa da sua galeria no espaço de foto. Todos os elementos continuam editáveis.",color=Muted,fontSize=14.sp)
    }},confirmButton={TextButton(onClick={vm.music(title,artist,lyrics,progress);dismiss()},enabled=title.isNotBlank()) {Text("Criar card")}},dismissButton={TextButton(onClick=dismiss) {Text("Cancelar")}})
}
@Composable internal fun GradientControls(value:GradientSpec?,change:(GradientSpec?)->Unit) {
    val palettes=listOf(listOf(0xFF161322.toInt(),0xFFB7A9F5.toInt()),listOf(0xFFEA8EA8.toInt(),0xFFF4DDB3.toInt()),listOf(0xFF243E41.toInt(),0xFFB4D0C1.toInt()),listOf(0xFF161618.toInt(),0xFFD5FF69.toInt()))
    Text("Degradê",fontSize=14.sp,color=Muted)
    LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {item {TextButton(onClick={change(null)}) {Text("Sem")}};items(palettes) {colors->Box(Modifier.size(48.dp).background(Brush.linearGradient(colors.map {Color(it)})).clickable {change(GradientSpec(colors,value?.angle ?: 90f))}.semantics {contentDescription="Degradê de ${colorName(colors.first())} para ${colorName(colors.last())}";selected=value?.colors==colors;role=Role.RadioButton})}}
    value?.let {AdvancedSlider("Ângulo do degradê",it.angle,-180f..180f,{angle->change(it.copy(angle=angle))})}
}
@Composable internal fun DrawingSurface(state:StudioUi,vm:StudioViewModel,modifier:Modifier) {
    var color by remember {mutableIntStateOf(-1)};var radius by remember {mutableFloatStateOf(.01f)};var erase by remember {mutableStateOf(false)};var live by remember {mutableStateOf<List<Point>>(emptyList())}
    val doc=state.edit.story ?: return
    Column(modifier,horizontalAlignment=Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
            val ratio=doc.width.toFloat()/doc.height;val w=minOf(maxWidth,maxHeight*ratio)
            Box(Modifier.width(w).aspectRatio(ratio)) {
                state.preview?.let {Image(it.asImageBitmap(),"Desenhar na composição",Modifier.fillMaxSize())}
                Canvas(Modifier.fillMaxSize().pointerInput(radius,color,erase) {
                    fun point(p:Offset)=Point((p.x/size.width).coerceIn(0f,1f),(p.y/size.height).coerceIn(0f,1f))
                    detectDragGestures(onDragStart={live=listOf(point(it))},onDragCancel={live=emptyList()},onDragEnd={if(live.isNotEmpty()) vm.drawing(DrawingStroke(live,radius,color,erase=erase));live=emptyList()}) {change,_->change.consume();if(live.size<2048) live=live+point(change.position)}
                }) {live.zipWithNext().forEach {(a,b)->drawLine(if(erase) Color.Gray else Color(color),Offset(a.x*size.width,a.y*size.height),Offset(b.x*size.width,b.y*size.height),radius*minOf(size.width,size.height)*2,StrokeCap.Round)}}
            }
        }
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(listOf(-1,0xFFD5FF69.toInt(),0xFFEA8EA8.toInt(),0xFFB7A9F5.toInt(),0xFF151518.toInt())) {c->Box(Modifier.size(48.dp).background(Color(c)).clickable {color=c;erase=false}.semantics {contentDescription="Desenhar com ${colorName(c)}";selected=color==c && !erase;role=Role.RadioButton})};item {TextButton(onClick={erase=!erase}) {Text(if(erase) "Apagando" else "Apagar")}}}
        Slider(radius,{radius=it},valueRange=.001f.. .08f,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).semantics {contentDescription="Tamanho do pincel de desenho"})
    }
}
