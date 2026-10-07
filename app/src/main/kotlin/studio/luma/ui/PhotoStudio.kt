@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package studio.luma.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import studio.luma.*
import studio.luma.core.*

private val StudioPaper=Color(0xFFF3EEE5)
private val StudioAccent=Color(0xFFF0AB83)
private val StudioLine=Color(0xFF353A41)
private val StudioGround=Color(0xFF111418)
private val StudioSurface=Color(0xFF1C2026)
private val neutralCard=FilterPreset(meta=AssetMeta("__original","Original","Original"),savedIntensity=0f)

internal fun collectionName(category:String)=when(category) {
    "Todos"->"Todos";"JDM"->"Flash 80/90";"Film"->"Analógicos";"CCD"->"Digicam";"Portrait"->"Retratos";"BW"->"P&B";"IG"->"Cotidiano";"Nature"->"Natureza";"Soft"->"Suaves";"Golden"->"Dourados";"Cool"->"Frios";"Coastal"->"Litoral";"Urban"->"Urbano";"Vintage"->"Vintage";"Nordic"->"Nórdicos";"Noir"->"Noir";else->category
}
private fun lookCollections(filters:List<FilterPreset>)=listOf("Todos","Favoritos")+listOf("JDM","Instagram").filter {category->filters.any {it.meta.category==category}}+filters.map {it.meta.category}.distinct().filter {it !in listOf("JDM","Instagram")}
private val accents=Regex("\\p{M}+")
private fun searchText(value:String)=java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFD).replace(accents,"").lowercase(java.util.Locale.ROOT)
private fun searchAliases(category:String)=when(category) {"Film"->"filme analogico retro";"BW"->"preto branco pb monocromatico";"CCD"->"camera digital digicam";else->""}
private fun collectionNote(category:String)=when(category) {
    "Instagram"->"Looks originais inspirados nos filtros do Instagram";"JDM"->"Verde de filme · flash compacto · memória dos anos 90";"Film","Vintage"->"Tons analógicos · memória e textura";"CCD"->"Flash, noites e câmeras de bolso";"Cinema","Noir"->"Luz, sombra e cor de cinema";"Portrait","Soft"->"Pele, luz suave e tons delicados";"BW"->"Contraste e luz em preto e branco";"Nature","Coastal"->"Verdes, céu e luz ao ar livre";"Neon","Y2K"->"Cor elétrica · cidade depois do sol";"Editorial"->"Cor refinada · contraste preciso";"Matte"->"Pretos suaves · luz com atmosfera";else->"Um novo jeito de sentir sua foto"
}

@Composable internal fun PhotoStudio(state:StudioUi,vm:StudioViewModel,back:()->Unit) {
    var tool by rememberSaveable(state.project?.id) {mutableStateOf("Filtros")}
    var pro by rememberSaveable {mutableStateOf(false)}
    var more by remember {mutableStateOf(false)}
    var help by remember {mutableStateOf(false)}
    var discard by remember {mutableStateOf(false)}
    var export by rememberSaveable {mutableStateOf(false)};var save by rememberSaveable {mutableStateOf(false)}
    var enhancement by remember {mutableStateOf(false)}
    var compare by remember {mutableStateOf(false)};var holding by remember {mutableStateOf(false)};var split by remember {mutableFloatStateOf(.5f)}
    val context=LocalContext.current
    val scrollLayout=LocalDensity.current.fontScale>=1.3f || LocalConfiguration.current.screenHeightDp<640
    val scroll=rememberScrollState()
    Column(Modifier.fillMaxSize().background(StudioGround).statusBarsPadding().navigationBarsPadding().then(if(scrollLayout) Modifier.verticalScroll(scroll) else Modifier)) {
        Row(Modifier.fillMaxWidth().heightIn(min=56.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=back,modifier=Modifier.size(48.dp)) {Icon(Icons.AutoMirrored.Rounded.ArrowBack,"Voltar ao início",tint=StudioPaper)}
            Column(Modifier.weight(1f).padding(start=6.dp)) {
                Text("Editar foto",color=StudioPaper,fontSize=18.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            if(!scrollLayout) Button(onClick={export=true},enabled=state.preview!=null && !state.rendering,shape=RoundedCornerShape(10.dp),colors=ButtonDefaults.buttonColors(containerColor=StudioPaper,contentColor=StudioGround),modifier=Modifier.heightIn(min=48.dp).testTag("photo-save")) {Text("Salvar foto",fontSize=14.sp,fontWeight=FontWeight.SemiBold)}
            Box {
                IconButton(onClick={more=true},modifier=Modifier.size(48.dp)) {Icon(Icons.Rounded.MoreVert,"Mais ferramentas",tint=StudioPaper)}
                DropdownMenu(expanded=more,onDismissRequest={more=false}) {
                    DropdownMenuItem(text={Text("Salvar projeto editável")},onClick={more=false;save=true})
                    DropdownMenuItem(text={Text("Máscaras e retoques")},onClick={more=false;vm.finishGesture();vm.openMasks()})
                    DropdownMenuItem(text={Text("Estilos prontos")},onClick={more=false;vm.finishGesture();tool="Estilos";pro=true})
                    DropdownMenuItem(text={Text("Cor e ferramentas avançadas")},onClick={more=false;vm.finishGesture();tool="Avançado";pro=true})
                    DropdownMenuItem(text={Text(if(pro) "Usar modo simples" else "Mostrar todas as ferramentas")},onClick={more=false;pro=!pro;if(!pro && tool !in listOf("Filtros","Ajustes","Recorte","Efeitos")) tool="Filtros"})
                    DropdownMenuItem(text={Text("Como usar")},onClick={more=false;help=true})
                    DropdownMenuItem(text={Text("Descartar rascunho")},enabled=state.dirty,onClick={more=false;discard=true})
                }
            }
        }
        if(scrollLayout) Button(onClick={export=true},enabled=state.preview!=null && !state.rendering,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp).heightIn(min=48.dp).testTag("photo-save")) {Text("Salvar foto na galeria",fontSize=16.sp)}
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp).heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Text(draftLabel(state),fontSize=13.sp,color=if(state.draftState==DraftState.FAILED) MaterialTheme.colorScheme.error else Muted,modifier=Modifier.weight(1f).semantics {liveRegion=LiveRegionMode.Polite}.testTag("draft-status"))
            Row(verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=vm::undo,enabled=state.canUndo,modifier=Modifier.size(48.dp)) {Icon(Icons.AutoMirrored.Rounded.Undo,"Desfazer",tint=if(state.canUndo) StudioPaper else Muted)}
                IconButton(onClick=vm::redo,enabled=state.canRedo,modifier=Modifier.size(48.dp)) {Icon(Icons.AutoMirrored.Rounded.Redo,"Refazer",tint=if(state.canRedo) StudioPaper else Muted)}
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            if(!scrollLayout) Text(if(pro) "Todas as ferramentas" else "Modo simples",fontSize=13.sp,color=Muted,modifier=Modifier.weight(1f))
            TextButton(onClick={vm.finishGesture();enhancement=true},enabled=!state.busy && !state.rendering && state.preview!=null,modifier=Modifier.heightIn(min=48.dp).testTag("photo-enhance")) {Icon(Icons.Rounded.AutoAwesome,null);Spacer(Modifier.width(6.dp));Text("Melhorar com IA",fontSize=14.sp)}
        }
        BoxWithConstraints((if(scrollLayout) Modifier.height(240.dp) else Modifier.weight(1f)).fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),contentAlignment=Alignment.Center) {
            val bitmap=if(tool=="Recorte") state.source else state.preview ?: state.source
            bitmap?.let {image->
                val aspect=image.width.toFloat()/image.height;val width=minOf(maxWidth,maxHeight*aspect)
                Box(Modifier.width(width).aspectRatio(aspect).clip(RoundedCornerShape(12.dp)).background(StudioSurface).testTag("photo-stage")) {
                    Canvas(Modifier.fillMaxSize()) {val cell=12.dp.toPx();for(y in 0..(size.height/cell).toInt()) for(x in 0..(size.width/cell).toInt()) drawRect(if((x+y)%2==0) Color(0xFF262B31) else StudioSurface,Offset(x*cell,y*cell),androidx.compose.ui.geometry.Size(cell,cell))}
                    if(tool=="Recorte") CropCanvas(state,vm) else {
                        Image(image.asImageBitmap(),"Foto editada",Modifier.fillMaxSize().pointerInput(image) {detectTapGestures(onPress={holding=true;try {tryAwaitRelease()} finally {holding=false}})},contentScale=ContentScale.FillBounds)
                        state.before?.let {before->if(holding || compare) Image(before.asImageBitmap(),"Original",Modifier.fillMaxSize().drawWithContent {clipRect(right=if(holding) size.width else size.width*split) {this@drawWithContent.drawContent()}},contentScale=ContentScale.FillBounds)}
                        if(compare && !holding) Canvas(Modifier.fillMaxSize()) {drawLine(StudioPaper,Offset(size.width*split,0f),Offset(size.width*split,size.height),2.dp.toPx())}
                    }
                    if(state.rendering) LinearProgressIndicator(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp),color=StudioAccent,trackColor=Color.Transparent)
                }
            }
        }
        if(compare && tool!="Recorte") Slider(split,{split=it},valueRange=.05f.. .95f,modifier=Modifier.fillMaxWidth().padding(horizontal=24.dp).heightIn(min=48.dp).semantics {contentDescription="Posição da comparação antes e depois"})
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(if(tool=="Recorte") "Arraste os cantos para recortar" else state.edit.filter?.preset?.meta?.name ?: "Foto original",fontSize=13.sp,color=StudioPaper,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
            if(state.edit.filter!=null) IconButton(onClick={vm.applyFilter(null)},modifier=Modifier.size(48.dp)) {Icon(Icons.Rounded.Close,"Remover filtro",tint=Muted)}
            if(tool!="Recorte") TextButton(onClick={compare=!compare}) {Text(if(compare) "Fechar comparação" else "Comparar",fontSize=13.sp)}
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=24.dp,topEnd=24.dp)).background(StudioSurface).padding(top=8.dp,bottom=6.dp)) {
            Box(Modifier.fillMaxWidth().heightIn(min=166.dp,max=if(scrollLayout) 480.dp else if(pro) 340.dp else 310.dp)) {
                when(tool) {
                    "Filtros"->PhotoLooks(state,vm,!pro)
                    "Ajustes"->PhotoAdjustments(state,vm,pro)
                    "Recorte"->CropControls(state,vm)
                    "Efeitos"->EffectControls(state,vm)
                    "Estilos"->StyleControls(state,vm)
                    "Avançado"->AdvancedEntry(state,vm)
                }
            }
            HorizontalDivider(color=StudioLine.copy(alpha=.5f),modifier=Modifier.padding(top=9.dp))
            val tools=listOf(Triple("Filtros",Icons.Rounded.AutoAwesome,"Filtros"),Triple("Ajustes",Icons.Rounded.Tune,"Luz e cor"),Triple("Recorte",Icons.Rounded.Crop,"Recortar"),Triple("Efeitos",Icons.Rounded.BlurOn,"Efeitos"))
            if(pro) LazyRow(Modifier.fillMaxWidth().testTag("photo-tools"),contentPadding=PaddingValues(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                items(tools+listOf(Triple("Máscaras",Icons.Rounded.Brush,"Máscaras"),Triple("Estilos",Icons.Rounded.Palette,"Estilos"),Triple("Avançado",Icons.Rounded.Toll,"Avançado")),key={it.first}) {(id,icon,label)->PhotoTool(id,label,icon,tool==id,Modifier.widthIn(min=80.dp)) {vm.finishGesture();if(id=="Máscaras") vm.openMasks() else tool=id}}
            } else Row(Modifier.fillMaxWidth().testTag("photo-tools").padding(horizontal=8.dp)) {
                tools.forEach {(id,icon,label)->PhotoTool(id,label,icon,tool==id,Modifier.weight(1f)) {vm.finishGesture();tool=id}}
            }
        }
        state.exported?.let {uri->Row(Modifier.fillMaxWidth().padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically) {Text("Salva na galeria",fontSize=11.sp,color=StudioAccent,modifier=Modifier.weight(1f));TextButton(onClick={context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type=context.contentResolver.getType(uri);putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Compartilhar foto"))}) {Text("Compartilhar",fontSize=11.sp)}}}
    }
    if(export) ExportDialog({export=false}) {export=false;vm.finishGesture();vm.export(it)}
    if(enhancement) EnhancementControls(state,vm) {enhancement=false}
    if(help) StudioHelpDialog {help=false}
    if(discard) AlertDialog(onDismissRequest={discard=false},title={Text("Descartar este rascunho?")},text={Text("As alterações desde o último salvamento do projeto serão removidas. A foto original será mantida.")},confirmButton={TextButton(onClick={discard=false;vm.closeEditor(true)}) {Text("Descartar alterações")}},dismissButton={TextButton(onClick={discard=false}) {Text("Continuar editando")}})
    state.enhancementProposal?.let {EnhancementComparison(it,state.busy,vm::applyEnhancement,vm::discardEnhancement)}
    if(save) {
        var name by remember {mutableStateOf(state.project?.name ?: "Minha foto")}
        AlertDialog(onDismissRequest={save=false},title={Text("Guardar no estúdio")},text={OutlinedTextField(name,{name=it.take(100)},label={Text("Nome do projeto")},singleLine=true)},confirmButton={TextButton(onClick={save=false;vm.finishGesture();vm.save(name)}) {Text("Salvar")}},dismissButton={TextButton(onClick={save=false}) {Text("Cancelar")}})
    }
}

@Composable private fun PhotoTool(id:String,label:String,icon:ImageVector,active:Boolean,modifier:Modifier,action:()->Unit) {
    Column(modifier.heightIn(min=64.dp).clip(RoundedCornerShape(10.dp)).clickable(onClickLabel=label,onClick=action).semantics {selected=active;role=Role.Tab}.padding(vertical=10.dp,horizontal=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Icon(icon,null,tint=if(active) StudioAccent else Muted)
        Text(label,fontSize=13.sp,lineHeight=17.sp,color=if(active) StudioPaper else Muted,maxLines=2)
    }
}

@Composable internal fun EditorCollections(categories:List<String>,selected:String,choose:(String)->Unit,modifier:Modifier=Modifier,label:(String)->String=::collectionName) {
    LazyRow(modifier,horizontalArrangement=Arrangement.spacedBy(19.dp),contentPadding=PaddingValues(horizontal=20.dp)) {
        items(categories.distinct(),key={it}) {category->Column(Modifier.heightIn(min=48.dp).clip(RoundedCornerShape(4.dp)).clickable {choose(category)}.semantics {this.selected=selected==category;role=Role.Tab}.padding(vertical=10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text(label(category),fontSize=14.sp,color=if(selected==category) StudioPaper else Muted,fontWeight=if(selected==category) FontWeight.SemiBold else FontWeight.Normal)
            Box(Modifier.height(2.dp).width(if(selected==category) 24.dp else 0.dp).background(StudioAccent,RoundedCornerShape(50)))
        }}
    }
}
@Composable private fun PhotoLooks(state:StudioUi,vm:StudioViewModel,simple:Boolean) {
    var category by remember {mutableStateOf("Todos")};var browser by remember {mutableStateOf(false)}
    val filters=if(simple) (state.filters.filter {it.meta.id in state.favorites}.take(8)+state.filters.filter {it.meta.category=="JDM"}.take(6)+state.filters.filter {it.meta.category=="Instagram"}.take(6)+state.filters.filter {it.meta.category=="Portrait"}.take(4)+state.filters.take(4)+listOfNotNull(state.edit.filter?.preset)).distinctBy {it.meta.id} else state.filters.filter {category=="Todos" || category=="Favoritos" && it.meta.id in state.favorites || it.meta.category==category}
    val carousel=rememberLazyListState()
    LaunchedEffect(state.edit.filter?.preset?.meta?.id,category,filters.map {it.meta.id}) {
        val index=filters.indexOfFirst {it.meta.id==state.edit.filter?.preset?.meta?.id}
        if(index>=0) carousel.animateScrollToItem(index+1)
        else if(state.edit.filter==null) carousel.animateScrollToItem(0)
    }
    Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Filtros",fontFamily=FontFamily.Serif,fontSize=24.sp,color=StudioPaper,modifier=Modifier.weight(1f))
            TextButton(onClick={browser=true},modifier=Modifier.testTag("all-filters")) {Text("Ver todos (${state.filters.size})",fontSize=14.sp,color=StudioPaper)}
        }
        if(!simple) EditorCollections(lookCollections(state.filters),category,{category=it})
        LazyRow(Modifier.testTag("look-carousel"),state=carousel,contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            item {LookCard(null,state,vm,Modifier.width(108.dp).height(148.dp))}
            items(filters,key={it.meta.id}) {preset->LookCard(preset,state,vm,Modifier.width(108.dp).height(148.dp))}
            if(filters.isEmpty()) item {Text("Marque os looks que mais gosta com o coração.",fontSize=11.sp,color=Muted,modifier=Modifier.width(240.dp).padding(16.dp))}
        }
        LookIntensity(state,vm,Modifier.padding(horizontal=20.dp))
    }
    if(browser) LookBrowser(state,vm,category,{category=it}) {vm.finishGesture();browser=false}
}
@Composable private fun LookIntensity(state:StudioUi,vm:StudioViewModel,modifier:Modifier=Modifier) {
    val selection=state.edit.filter
    Row(modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Força",fontSize=14.sp,color=Muted)
        Slider(selection?.intensity ?: 0f,{v->val current=vm.ui.value.edit;current.filter?.let {vm.update(current.copy(filter=it.copy(intensity=v)),false)}},enabled=selection!=null,onValueChangeFinished=vm::finishGesture,modifier=Modifier.weight(1f).testTag("look-intensity").semantics {contentDescription="Força do filtro";stateDescription="${((selection?.intensity ?: 0f)*100).toInt()} por cento"},colors=SliderDefaults.colors(thumbColor=StudioPaper,activeTrackColor=StudioAccent,inactiveTrackColor=StudioLine))
        Text("${((selection?.intensity ?: 0f)*100).toInt()}%",fontSize=14.sp,color=StudioPaper)
    }
}
@Composable private fun LookCard(preset:FilterPreset?,state:StudioUi,vm:StudioViewModel,modifier:Modifier) {
    val candidate=preset ?: neutralCard
    val bitmap by produceState<Bitmap?>(state.thumbnails[candidate.meta.id],state.source,state.thumbnailRevision,candidate) {value=state.thumbnails[candidate.meta.id];if(value==null) value=vm.thumbnail(candidate)}
    val focus=LocalFocusManager.current
    val selected=if(preset==null) state.edit.filter==null else state.edit.filter?.preset?.meta?.id==preset.meta.id
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(StudioGround).border(if(selected) 2.dp else 1.dp,if(selected) StudioAccent else StudioLine,RoundedCornerShape(12.dp)).clickable {focus.clearFocus();vm.applyFilter(preset)}.semantics {this.selected=selected;role=Role.Button}.testTag("look-${candidate.meta.id}")) {
        bitmap?.let {Image(it.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)} ?: Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {CircularProgressIndicator(Modifier.size(16.dp),color=StudioAccent,strokeWidth=1.dp)}
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Transparent,Color.Black.copy(alpha=.82f)))))
        if(selected) Box(Modifier.align(Alignment.TopStart).padding(7.dp).size(19.dp).background(StudioAccent,RoundedCornerShape(50)),contentAlignment=Alignment.Center) {Icon(Icons.Rounded.Check,null,Modifier.size(12.dp),tint=StudioGround)}
        if(preset!=null) IconButton(onClick={vm.toggleFavorite(preset.meta.id)},modifier=Modifier.align(Alignment.TopEnd).size(48.dp)) {Icon(if(preset.meta.id in state.favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favoritar ${preset.meta.name}",Modifier.size(32.dp).background(StudioGround.copy(alpha=.75f),RoundedCornerShape(50)).padding(4.dp),tint=if(preset.meta.id in state.favorites) StudioAccent else StudioPaper)}
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal=9.dp,vertical=9.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Text(preset?.meta?.name ?: "Original",fontSize=14.sp,lineHeight=18.sp,color=StudioPaper,fontWeight=FontWeight.Medium,maxLines=2,overflow=TextOverflow.Ellipsis)
            if(preset!=null && LocalDensity.current.fontScale<1.3f) Text(collectionName(preset.meta.category),fontSize=12.sp,color=StudioPaper.copy(alpha=.85f),maxLines=1)
        }
    }
}
@Composable private fun LookBrowser(state:StudioUi,vm:StudioViewModel,category:String,chooseCategory:(String)->Unit,dismiss:()->Unit) {
    var query by remember {mutableStateOf("")}
    val sheet=rememberModalBottomSheetState(skipPartiallyExpanded=true)
    val categories=lookCollections(state.filters)
    val words=remember(query) {searchText(query).trim().split(Regex("\\s+")).filter {it.isNotEmpty()}}
    val index=remember(state.filters) {state.filters.associate {it.meta.id to searchText(it.meta.name+" "+it.meta.category+" "+collectionName(it.meta.category)+" "+searchAliases(it.meta.category)+" "+it.meta.tags.joinToString(" "))}}
    val filters=state.filters.filter {preset->(category=="Todos" || category=="Favoritos" && preset.meta.id in state.favorites || category==preset.meta.category) && words.all {index.getValue(preset.meta.id).contains(it)}}
    ModalBottomSheet(onDismissRequest=dismiss,sheetState=sheet,containerColor=StudioSurface,contentColor=StudioPaper,dragHandle={Box(Modifier.padding(top=10.dp,bottom=12.dp).width(32.dp).height(3.dp).background(StudioLine,RoundedCornerShape(50)))}) {
        val focus=LocalFocusManager.current
        Column(Modifier.fillMaxWidth().fillMaxHeight(.91f).testTag("look-browser").navigationBarsPadding(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                state.preview?.let {Image(it.asImageBitmap(),"Prévia do look selecionado",Modifier.size(66.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop)}
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {Text("Escolha um filtro",fontFamily=FontFamily.Serif,fontSize=24.sp);Text(state.edit.filter?.preset?.meta?.name ?: "Original",color=StudioAccent,fontSize=14.sp,maxLines=2)}
                IconButton(onClick=dismiss) {Icon(Icons.Rounded.Close,"Fechar galeria de looks")}
            }
            OutlinedTextField(query,{query=it.take(100)},modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp).testTag("look-search"),singleLine=true,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={focus.clearFocus()}),placeholder={Text("Buscar: retrato, neon, filme…",fontSize=12.sp)},leadingIcon={Icon(Icons.Rounded.Search,null,Modifier.size(18.dp))},shape=RoundedCornerShape(12.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=StudioAccent,unfocusedBorderColor=StudioLine,cursorColor=StudioAccent))
            EditorCollections(categories,category,{focus.clearFocus();chooseCategory(it)},Modifier.testTag("look-collections"))
            Row(Modifier.padding(horizontal=20.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(if(category=="Todos") "TODAS AS COLEÇÕES" else collectionName(category).uppercase(),fontSize=9.sp,letterSpacing=1.4.sp,color=Muted);Text("${filters.size} ${if(filters.size==1) "look" else "looks"}",fontSize=10.sp,color=Muted)}
            Text(collectionNote(category),fontSize=10.sp,color=Muted,modifier=Modifier.padding(horizontal=20.dp))
            LazyVerticalGrid(columns=GridCells.Adaptive(98.dp),modifier=Modifier.weight(1f).testTag("look-grid"),contentPadding=PaddingValues(horizontal=20.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(11.dp),verticalArrangement=Arrangement.spacedBy(11.dp)) {
                item {LookCard(null,state,vm,Modifier.fillMaxWidth().aspectRatio(.75f))}
                items(filters,key={it.meta.id}) {preset->LookCard(preset,state,vm,Modifier.fillMaxWidth().aspectRatio(.75f))}
                if(filters.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {Column(Modifier.fillMaxWidth().padding(vertical=24.dp),horizontalAlignment=Alignment.CenterHorizontally) {Text("Nenhum look por aqui",fontFamily=FontFamily.Serif,fontSize=21.sp);Text("Tente outra busca ou coleção.",fontSize=12.sp,color=Muted)}}
            }
            LookIntensity(state,vm,Modifier.padding(horizontal=20.dp))
            Button(onClick=dismiss,modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp).padding(bottom=12.dp).heightIn(min=48.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=StudioPaper,contentColor=StudioGround)) {Text("Usar este filtro",fontSize=16.sp,fontWeight=FontWeight.SemiBold)}
        }
    }
}
@Composable private fun PhotoAdjustments(state:StudioUi,vm:StudioViewModel,pro:Boolean) {
    var name by remember {mutableStateOf("Exposição")};val spec=adjustments.first {it.name==name};val value=spec.read(state.edit.adjustments)
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        val visible=if(pro) adjustments.map {it.name} else listOf("Exposição","Contraste","Saturação","Temperatura","Sombras")
        LaunchedEffect(pro) {if(name !in visible) name="Exposição"}
        Row(Modifier.padding(horizontal=20.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {Text(adjustmentLabel(name),fontFamily=FontFamily.Serif,fontSize=22.sp,color=StudioPaper,modifier=Modifier.weight(1f));Text(if(value==spec.default) "Padrão" else "%.2f".format(java.util.Locale.ROOT,value),fontSize=16.sp,color=StudioAccent)}
        EditorCollections(visible,name,{name=it},label=::adjustmentLabel)
        Slider(value,{v->vm.update(vm.ui.value.edit.copy(adjustments=spec.write(vm.ui.value.edit.adjustments,v)),false)},valueRange=spec.min..spec.max,onValueChangeFinished=vm::finishGesture,modifier=Modifier.fillMaxWidth().padding(horizontal=24.dp).heightIn(min=48.dp).testTag("photo-adjustment").semantics {contentDescription=adjustmentLabel(name)},colors=SliderDefaults.colors(thumbColor=StudioPaper,activeTrackColor=StudioAccent,inactiveTrackColor=StudioLine))
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {Text("Desfazer volta um passo.",fontSize=14.sp,color=Muted,modifier=Modifier.weight(1f));TextButton(onClick={vm.update(state.edit.copy(adjustments=spec.write(state.edit.adjustments,spec.default)))}) {Text("Redefinir",fontSize=14.sp)}}
    }
}
private fun adjustmentLabel(name:String)=when(name) {"Exposição"->"Clarear foto";"Saturação"->"Força das cores";"Temperatura"->"Cor quente ou fria";"Sombras"->"Áreas escuras";else->name}
