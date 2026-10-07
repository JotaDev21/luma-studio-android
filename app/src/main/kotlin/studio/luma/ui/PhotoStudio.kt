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
    var tool by remember(state.project?.id) {mutableStateOf("Filtros")}
    var pro by remember {mutableStateOf(false)}
    var export by remember {mutableStateOf(false)};var save by remember {mutableStateOf(false)}
    var enhancement by remember {mutableStateOf(false)}
    var compare by remember {mutableStateOf(false)};var holding by remember {mutableStateOf(false)};var split by remember {mutableFloatStateOf(.5f)}
    val context=LocalContext.current
    Column(Modifier.fillMaxSize().background(StudioGround).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=back,modifier=Modifier.size(40.dp)) {Icon(Icons.Rounded.Close,"Voltar",tint=StudioPaper,modifier=Modifier.size(20.dp))}
            Column(Modifier.weight(1f).padding(start=6.dp)) {
                Text("LUMA",color=StudioPaper,fontSize=16.sp,fontWeight=FontWeight.Bold,letterSpacing=3.sp)
                Text("PHOTO STUDIO",color=Muted,fontSize=8.sp,letterSpacing=1.6.sp)
            }
            IconButton(onClick={save=true},modifier=Modifier.size(40.dp)) {Icon(Icons.Rounded.BookmarkBorder,"Salvar projeto",tint=StudioPaper,modifier=Modifier.size(21.dp))}
            Button(onClick={export=true},enabled=state.preview!=null && !state.rendering,shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(horizontal=15.dp,vertical=0.dp),colors=ButtonDefaults.buttonColors(containerColor=StudioPaper,contentColor=StudioGround),modifier=Modifier.height(36.dp)) {Text("Exportar",fontSize=12.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.width(5.dp));Icon(Icons.AutoMirrored.Rounded.ArrowForward,null,Modifier.size(15.dp))}
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp).height(34.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(5.dp).background(if(state.dirty) StudioAccent else Color(0xFF8CBDA6),RoundedCornerShape(50)))
                Text(if(state.dirty) "EDITANDO" else "PROJETO SALVO",fontSize=9.sp,letterSpacing=1.4.sp,color=Muted)
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=vm::undo,enabled=state.canUndo,modifier=Modifier.size(32.dp)) {Icon(Icons.AutoMirrored.Rounded.Undo,"Desfazer",Modifier.size(18.dp),tint=if(state.canUndo) StudioPaper else StudioLine)}
                IconButton(onClick=vm::redo,enabled=state.canRedo,modifier=Modifier.size(32.dp)) {Icon(Icons.AutoMirrored.Rounded.Redo,"Refazer",Modifier.size(18.dp),tint=if(state.canRedo) StudioPaper else StudioLine)}
                Spacer(Modifier.width(8.dp))
                Text(if(pro) "Modo Pro" else "Modo rápido",fontSize=10.sp,color=if(pro) StudioAccent else Muted,modifier=Modifier.clip(RoundedCornerShape(6.dp)).clickable {pro=!pro;if(!pro && tool=="Avançado") tool="Filtros"}.padding(8.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Mais detalhe na sua foto",fontSize=11.sp,color=Muted)
            FilledTonalButton(onClick={vm.finishGesture();enhancement=true},enabled=!state.busy && !state.rendering && state.preview!=null,contentPadding=PaddingValues(horizontal=12.dp,vertical=0.dp),shape=RoundedCornerShape(10.dp),modifier=Modifier.height(34.dp).testTag("photo-enhance")) {Icon(Icons.Rounded.AutoAwesome,null,Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text("Melhorar com IA",fontSize=11.sp)}
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal=18.dp,vertical=12.dp),contentAlignment=Alignment.Center) {
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
                    if(tool!="Recorte") Row(Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(7.dp)).background(StudioGround.copy(alpha=.7f)).clickable {compare=!compare}.padding(horizontal=10.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.Compare,null,Modifier.size(14.dp),tint=StudioPaper);Text(if(holding) "ORIGINAL" else if(compare) "ANTES / DEPOIS" else "COMPARAR",fontSize=8.sp,color=StudioPaper,letterSpacing=.8.sp)
                    }
                    if(state.rendering) LinearProgressIndicator(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp),color=StudioAccent,trackColor=Color.Transparent)
                }
            }
        }
        if(compare && tool!="Recorte") Slider(split,{split=it},valueRange=.05f.. .95f,modifier=Modifier.fillMaxWidth().padding(horizontal=24.dp).height(26.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal=22.dp).padding(bottom=12.dp),horizontalArrangement=Arrangement.SpaceBetween) {
            val dimensions=state.preview ?: state.source
            Text(if(tool=="Recorte") "ARRASTE OS CANTOS" else state.edit.filter?.preset?.meta?.name ?: "Sua foto, seu ponto de partida",fontSize=10.sp,color=StudioPaper,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
            if(state.edit.filter!=null) IconButton(onClick={vm.applyFilter(null)},modifier=Modifier.size(20.dp)) {Icon(Icons.Rounded.Close,"Remover filtro",Modifier.size(12.dp),tint=Muted)}
            Spacer(Modifier.width(8.dp))
            Text(dimensions?.let {"${it.width} × ${it.height} · PRÉVIA"} ?: "",fontSize=8.sp,color=Muted,letterSpacing=.8.sp)
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=24.dp,topEnd=24.dp)).background(StudioSurface).animateContentSize().padding(top=14.dp,bottom=6.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(30.dp).height(3.dp).background(StudioLine,RoundedCornerShape(50)))
            Box(Modifier.fillMaxWidth().heightIn(min=166.dp,max=320.dp).padding(top=10.dp)) {
                when(tool) {
                    "Filtros"->PhotoLooks(state,vm)
                    "Ajustes"->PhotoAdjustments(state,vm)
                    "Recorte"->CropControls(state,vm)
                    "Efeitos"->EffectControls(state,vm)
                    "Estilos"->StyleControls(state,vm)
                    "Avançado"->AdvancedEntry(state,vm)
                }
            }
            HorizontalDivider(color=StudioLine.copy(alpha=.5f),modifier=Modifier.padding(top=9.dp))
            LazyRow(Modifier.fillMaxWidth().testTag("photo-tools"),contentPadding=PaddingValues(horizontal=14.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                val tools=listOf(Triple("Filtros",Icons.Rounded.AutoAwesome,"Looks"),Triple("Ajustes",Icons.Rounded.Tune,"Ajustes"),Triple("Recorte",Icons.Rounded.Crop,"Recorte"),Triple("Máscaras",Icons.Rounded.Brush,"Máscaras"),Triple("Efeitos",Icons.Rounded.BlurOn,"Efeitos"),Triple("Estilos",Icons.Rounded.Palette,"Estilos"))+if(pro) listOf(Triple("Avançado",Icons.Rounded.Toll,"Pro")) else emptyList()
                items(tools,key={it.first}) {(id,icon,label)->Column(Modifier.width(60.dp).height(62.dp).clip(RoundedCornerShape(10.dp)).clickable {vm.finishGesture();if(id=="Máscaras") vm.openMasks() else tool=id}.padding(top=10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Icon(icon,contentDescription=id,modifier=Modifier.size(21.dp),tint=if(tool==id) StudioAccent else Muted)
                    Text(if(id=="Avançado") "Avançado" else if(id=="Filtros") "Filtros" else label,fontSize=10.sp,color=if(tool==id) StudioPaper else Muted,maxLines=1)
                }}
            }
        }
        state.exported?.let {uri->Row(Modifier.fillMaxWidth().padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically) {Text("Salva na galeria",fontSize=11.sp,color=StudioAccent,modifier=Modifier.weight(1f));TextButton(onClick={context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type=context.contentResolver.getType(uri);putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Compartilhar foto"))}) {Text("Compartilhar",fontSize=11.sp)}}}
    }
    if(export) ExportDialog({export=false}) {export=false;vm.finishGesture();vm.export(it)}
    if(enhancement) EnhancementControls(state,vm) {enhancement=false}
    state.enhancementProposal?.let {EnhancementComparison(it,state.busy,vm::applyEnhancement,vm::discardEnhancement)}
    if(save) {
        var name by remember {mutableStateOf(state.project?.name ?: "Minha foto")}
        AlertDialog(onDismissRequest={save=false},title={Text("Guardar no estúdio")},text={OutlinedTextField(name,{name=it.take(100)},label={Text("Nome do projeto")},singleLine=true)},confirmButton={TextButton(onClick={save=false;vm.finishGesture();vm.save(name)}) {Text("Salvar")}},dismissButton={TextButton(onClick={save=false}) {Text("Cancelar")}})
    }
}

@Composable internal fun EditorCollections(categories:List<String>,selected:String,choose:(String)->Unit,modifier:Modifier=Modifier) {
    LazyRow(modifier,horizontalArrangement=Arrangement.spacedBy(19.dp),contentPadding=PaddingValues(horizontal=20.dp)) {
        items(categories.distinct(),key={it}) {category->Column(Modifier.clip(RoundedCornerShape(4.dp)).clickable {choose(category)}.padding(top=4.dp,bottom=2.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text(collectionName(category),fontSize=11.sp,color=if(selected==category) StudioPaper else Muted,fontWeight=if(selected==category) FontWeight.SemiBold else FontWeight.Normal)
            Box(Modifier.height(2.dp).width(if(selected==category) 24.dp else 0.dp).background(StudioAccent,RoundedCornerShape(50)))
        }}
    }
}
@Composable private fun PhotoLooks(state:StudioUi,vm:StudioViewModel) {
    var category by remember {mutableStateOf("Todos")};var browser by remember {mutableStateOf(false)}
    val filters=state.filters.filter {category=="Todos" || category=="Favoritos" && it.meta.id in state.favorites || it.meta.category==category}
    val carousel=rememberLazyListState()
    LaunchedEffect(state.edit.filter?.preset?.meta?.id,category) {
        val index=filters.indexOfFirst {it.meta.id==state.edit.filter?.preset?.meta?.id}
        if(index>=0) carousel.animateScrollToItem(index.coerceAtLeast(0))
        else if(state.edit.filter==null) carousel.animateScrollToItem(0)
    }
    Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {Text("Looks",fontFamily=FontFamily.Serif,fontSize=25.sp,color=StudioPaper);Text("${state.filters.size}",fontSize=9.sp,color=StudioAccent,modifier=Modifier.border(1.dp,StudioLine,RoundedCornerShape(5.dp)).padding(horizontal=6.dp,vertical=4.dp))}
            Row(Modifier.clip(RoundedCornerShape(7.dp)).clickable {browser=true}.padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {Icon(Icons.Rounded.GridView,null,Modifier.size(15.dp),tint=StudioPaper);Text("Explorar looks",fontSize=11.sp,color=StudioPaper)}
        }
        EditorCollections(lookCollections(state.filters),category,{category=it})
        LazyRow(Modifier.testTag("look-carousel"),state=carousel,contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            item {LookCard(null,state,vm,Modifier.width(88.dp).height(114.dp))}
            items(filters,key={it.meta.id}) {preset->LookCard(preset,state,vm,Modifier.width(88.dp).height(114.dp))}
            if(filters.isEmpty()) item {Text("Marque os looks que mais gosta com o coração.",fontSize=11.sp,color=Muted,modifier=Modifier.width(240.dp).padding(16.dp))}
        }
        LookIntensity(state,vm,Modifier.padding(horizontal=20.dp))
    }
    if(browser) LookBrowser(state,vm,category,{category=it}) {vm.finishGesture();browser=false}
}
@Composable private fun LookIntensity(state:StudioUi,vm:StudioViewModel,modifier:Modifier=Modifier) {
    val selection=state.edit.filter
    Row(modifier.fillMaxWidth().height(36.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.Tonality,null,Modifier.size(17.dp),tint=Muted)
        Text("Intensidade",fontSize=10.sp,color=Muted)
        Slider(selection?.intensity ?: 0f,{v->val current=vm.ui.value.edit;current.filter?.let {vm.update(current.copy(filter=it.copy(intensity=v)),false)}},enabled=selection!=null,onValueChangeFinished=vm::finishGesture,modifier=Modifier.weight(1f).testTag("look-intensity"),colors=SliderDefaults.colors(thumbColor=StudioPaper,activeTrackColor=StudioAccent,inactiveTrackColor=StudioLine))
        Text("${((selection?.intensity ?: 0f)*100).toInt()}",fontSize=10.sp,color=StudioPaper,modifier=Modifier.width(25.dp))
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
        if(preset!=null) IconButton(onClick={vm.toggleFavorite(preset.meta.id)},modifier=Modifier.align(Alignment.TopEnd).size(32.dp)) {Icon(if(preset.meta.id in state.favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favoritar ${preset.meta.name}",Modifier.size(23.dp).background(StudioGround.copy(alpha=.75f),RoundedCornerShape(50)).padding(4.dp),tint=if(preset.meta.id in state.favorites) StudioAccent else StudioPaper)}
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal=9.dp,vertical=9.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Text(preset?.meta?.name ?: "Original",fontSize=10.sp,lineHeight=12.sp,color=StudioPaper,fontWeight=FontWeight.Medium,maxLines=2,overflow=TextOverflow.Ellipsis)
            if(preset!=null) Text(collectionName(preset.meta.category).uppercase(),fontSize=6.sp,letterSpacing=.8.sp,color=StudioPaper.copy(alpha=.7f),maxLines=1)
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
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {Text("Encontre seu look",fontFamily=FontFamily.Serif,fontSize=24.sp);Text(state.edit.filter?.preset?.meta?.name ?: "Original",color=StudioAccent,fontSize=11.sp,maxLines=1)}
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
            Button(onClick=dismiss,modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp).padding(bottom=12.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=StudioPaper,contentColor=StudioGround)) {Text("Usar este look",fontWeight=FontWeight.SemiBold)}
        }
    }
}
@Composable private fun PhotoAdjustments(state:StudioUi,vm:StudioViewModel) {
    var name by remember {mutableStateOf("Exposição")};val spec=adjustments.first {it.name==name};val value=spec.read(state.edit.adjustments)
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.padding(horizontal=20.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {Text(name,fontFamily=FontFamily.Serif,fontSize=24.sp,color=StudioPaper);Text("%.2f".format(java.util.Locale.ROOT,value),fontSize=24.sp,fontWeight=FontWeight.Light,color=StudioAccent)}
        EditorCollections(adjustments.map {it.name},name,{name=it})
        Slider(value,{v->vm.update(vm.ui.value.edit.copy(adjustments=spec.write(vm.ui.value.edit.adjustments,v)),false)},valueRange=spec.min..spec.max,onValueChangeFinished=vm::finishGesture,modifier=Modifier.fillMaxWidth().padding(horizontal=24.dp).testTag("photo-adjustment"),colors=SliderDefaults.colors(thumbColor=StudioPaper,activeTrackColor=StudioAccent,inactiveTrackColor=StudioLine))
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),horizontalArrangement=Arrangement.SpaceBetween) {Text("Ajuste a luz. Encontre a sua cor.",fontSize=10.sp,color=Muted);Text("Redefinir",fontSize=10.sp,color=StudioPaper,modifier=Modifier.clickable {vm.update(state.edit.copy(adjustments=spec.write(state.edit.adjustments,spec.default)))}.padding(4.dp))}
    }
}
