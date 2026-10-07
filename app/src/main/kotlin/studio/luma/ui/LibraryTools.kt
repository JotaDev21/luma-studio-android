@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package studio.luma.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import studio.luma.*
import studio.luma.core.*
import kotlin.math.roundToInt

private val layouts=linkedMapOf("Grid" to "grid","Livre" to "freeform","Photo Dump" to "photo-dump","Magazine" to "magazine","Polaroid" to "polaroid","Scrapbook" to "scrapbook","Film Strip" to "film-strip")

@Composable fun CollageDialog(vm:StudioViewModel,dismiss:()->Unit) {
    var config by remember {mutableStateOf(CollageSettings())}
    val document=remember(config) {CollageEngine.create(config)}
    AlertDialog(onDismissRequest=dismiss,title={Text("Montar colagem")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Prévia do layout · preencha suas fotos no editor",color=Muted,fontSize=14.sp)
        Canvas(Modifier.fillMaxWidth().height(170.dp).background(Panel)) {
            val scale=minOf(size.width/document.width,size.height/document.height);val dx=(size.width-document.width*scale)/2
            document.layers.forEachIndexed {index,layer->val t=layer.transform
                rotate(t.rotation,Offset(dx+(t.x+t.width/2)*scale,(t.y+t.height/2)*scale)) {drawRect(if(index%2==0) Lilac else Acid,Offset(dx+t.x*scale,t.y*scale),Size(t.width*scale,t.height*scale))}
            }
        }
        CategoryRow(layouts.keys.toList(),layouts.entries.first {it.value==config.layout}.key) {config=config.copy(layout=layouts.getValue(it))}
        Text("${config.count} fotos",fontSize=14.sp)
        Slider(config.count.toFloat(),{config=config.copy(count=it.roundToInt())},valueRange=2f..20f,steps=17)
        Text("Você poderá mudar layout, bordas e enquadramento depois.",color=Muted,fontSize=14.sp)
    }},confirmButton={TextButton(onClick={vm.createCollage(config);dismiss()}) {Text("Montar colagem")}},dismissButton={TextButton(onClick=dismiss) {Text("Cancelar")}})
}

@Composable fun CollageControls(state:StudioUi,vm:StudioViewModel) {
    val config=state.edit.story?.collage ?: return
    var control by remember {mutableStateOf("Espaço")}
    val value=when(control) {"Espaço"->config.spacing;"Borda"->config.border;"Cantos"->config.cornerRadius;"Sombra"->config.shadow;else->config.rotation}
    val max=when(control) {"Cantos"->200f;"Sombra"->60f;"Inclinação"->30f;else->100f}
    Column(Modifier.padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        CategoryRow(layouts.keys.toList(),layouts.entries.first {it.value==config.layout}.key) {vm.changeCollage(config.copy(layout=layouts.getValue(it)))}
        CategoryRow(listOf("Espaço","Borda","Cantos","Sombra","Inclinação"),control) {control=it}
        Text("$control · ${value.roundToInt()}",color=Muted,fontSize=14.sp)
        Slider(value,{v->vm.changeCollage(when(control) {"Espaço"->config.copy(spacing=v);"Borda"->config.copy(border=v);"Cantos"->config.copy(cornerRadius=v);"Sombra"->config.copy(shadow=v);else->config.copy(rotation=v)},false)},valueRange=0f..max,onValueChangeFinished=vm::finishGesture,modifier=Modifier.heightIn(min=48.dp))
        TextButton(onClick={vm.proposeSmartLayout()},enabled=state.edit.story!!.layers.any {it.asset!=null && it.visible && !it.locked}) {Text("Organizar fotos",fontSize=14.sp)}
    }
}

@Composable fun SaveTemplateDialog(state:StudioUi,vm:StudioViewModel,dismiss:()->Unit) {
    val story=state.edit.story ?: return
    var name by remember {mutableStateOf(state.project?.name ?: "Meu template")}
    var photos by remember {mutableStateOf(story.layers.filter {it.type=="photo"}.map {it.id}.toSet())}
    var texts by remember {mutableStateOf(story.layers.filter {it.type=="text"}.map {it.id}.toSet())}
    AlertDialog(onDismissRequest=dismiss,title={Text("Salvar como template")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name,{name=it.take(100)},label={Text("Nome do template")},singleLine=true)
        Text("Escolha as fotos que serão substituídas a cada uso.",color=Muted,fontSize=14.sp)
        story.layers.filter {it.type=="photo"}.forEachIndexed {index,layer -> Row(verticalAlignment=Alignment.CenterVertically) {
            Checkbox(layer.id in photos,{checked->photos=if(checked) photos+layer.id else photos-layer.id},enabled=layer.asset!=null,modifier=Modifier.testTag("template-photo-${layer.id}"))
            Text("Foto ${index+1} · espaço preenchível",fontSize=14.sp)
        }}
        story.layers.filter {it.type=="text"}.forEach {layer->Row(verticalAlignment=Alignment.CenterVertically) {
            Checkbox(layer.id in texts,{checked->texts=if(checked) texts+layer.id else texts-layer.id})
            Text("Texto editável · ${layer.content.take(25)}",fontSize=14.sp)
        }}
        Text("Fotos desmarcadas permanecem fixas no template e entram no pack se você exportar a biblioteca. Textos desmarcados ficam bloqueados; você pode desbloqueá-los no editor.",color=Muted,fontSize=14.sp)
    }},confirmButton={TextButton(onClick={vm.saveCurrentTemplate(name.trim(),photos,texts);dismiss()},enabled=name.isNotBlank()) {Text("Salvar na biblioteca")}},dismissButton={TextButton(onClick=dismiss) {Text("Cancelar")}})
}

@Composable fun StyleControls(state:StudioUi,vm:StudioViewModel) {
    var saving by remember {mutableStateOf<String?>(null)}
    var browsing by remember {mutableStateOf(false)}
    val ordered=remember(state.styles) {state.styles.sortedByDescending {it.meta.id.startsWith("luma-style-social-")}}
    Column(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Estilos · ${state.styles.size}",fontSize=20.sp,fontFamily=FontFamily.Serif)
            TextButton(onClick={browsing=true}) {Icon(Icons.Rounded.GridView,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));Text("Explorar estilos",fontSize=14.sp)}
        }
        Text("Cor e efeitos prontos. As versões com textos também mudam a composição.",color=Muted,fontSize=14.sp)
        LazyRow(Modifier.testTag("style-carousel"),horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(ordered,key={it.meta.id}) {style->
            Column(Modifier.width(158.dp).clip(RoundedCornerShape(12.dp)).background(Panel).clickable(enabled=!state.busy) {vm.applyStyle(style)}.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(style.meta.name,fontSize=14.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                Text(if(style.layers.isEmpty()) "Foto · cor e efeitos" else "Moldura e textos",fontSize=14.sp,color=Muted)
            }
        }}
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            item {TextButton(onClick={saving="style"}) {Text("Salvar estilo",fontSize=14.sp)}}
            item {TextButton(onClick={saving="filter"}) {Text("Salvar filtro",fontSize=14.sp)}}
            if(state.edit.story==null) item {TextButton(onClick={vm.convertPhotoToStory()}) {Text("+ Texto e gráficos",fontSize=14.sp)}}
        }
    }
    if(browsing) StyleBrowser(state,vm,{vm.applyStyle(it);browsing=false}) {browsing=false}
    saving?.let {kind ->
        var name by remember(kind) {mutableStateOf("")}
        var graphics by remember(kind) {mutableStateOf(true)}
        AlertDialog(onDismissRequest={saving=null},title={Text(if(kind=="style") "Salvar estilo" else "Salvar filtro")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name,{name=it.take(100)},singleLine=true,label={Text("Nome")})
            if(kind=="style") {
                Text("Guarda filtro, ajustes e a sequência completa de efeitos.",color=Muted,fontSize=14.sp)
                if(state.edit.story!=null) Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(graphics,{graphics=it});Text("Incluir textos, formas e stickers",fontSize=14.sp)}
                Text("Fotos e layout ficam no projeto. Ao aplicar outro estilo, seus elementos anteriores são substituídos.",color=Muted,fontSize=14.sp)
            } else Text("Guarda o tratamento de cor e sua intensidade. Para incluir efeitos, salve como estilo.",color=Muted,fontSize=14.sp)
        }},confirmButton={TextButton(onClick={if(kind=="style") vm.saveCurrentStyle(name.trim(),graphics) else vm.saveCurrentFilter(name.trim());saving=null},enabled=name.isNotBlank()) {Text("Salvar na biblioteca")}},dismissButton={TextButton(onClick={saving=null}) {Text("Cancelar")}})
    }
}

private fun matchingStyles(state:StudioUi,category:String,query:String)=state.styles.filter {style->
    (category=="Todos" || category=="Novos" && style.meta.id.startsWith("luma-style-social-") || category=="Favoritos" && style.meta.id in state.favorites || category=="Só foto" && style.layers.isEmpty() || category=="Com elementos" && style.layers.isNotEmpty() || category==style.meta.category) && catalogMatches(style.meta,query)
}
private fun styleCategories(state:StudioUi)=listOf("Novos","Todos","Favoritos","Só foto","Com elementos")+state.styles.map {it.meta.category}.distinct()

@Composable private fun StyleCard(style:StylePreset,state:StudioUi,vm:StudioViewModel,modifier:Modifier,choose:()->Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Panel).testTag("style-${style.meta.id}").clickable(enabled=!state.busy,onClick=choose).padding(13.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Icon(if(style.layers.isEmpty()) Icons.Rounded.PhotoFilter else Icons.Rounded.DashboardCustomize,null,tint=Lilac,modifier=Modifier.size(24.dp))
            IconButton(onClick={vm.toggleFavorite(style.meta.id)},modifier=Modifier.size(32.dp)) {Icon(if(style.meta.id in state.favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favoritar ${style.meta.name}",tint=Acid,modifier=Modifier.size(18.dp))}
        }
        Text(style.meta.name,fontSize=14.sp,fontWeight=FontWeight.Medium,maxLines=2,overflow=TextOverflow.Ellipsis)
        Text(style.filter?.preset?.meta?.name ?: "Cor personalizada",fontSize=14.sp,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis)
        Text(if(style.layers.isEmpty()) "Só foto" else "Textos editáveis",fontSize=14.sp,color=Lilac)
    }
}

@Composable private fun StyleBrowser(state:StudioUi,vm:StudioViewModel,choose:(StylePreset)->Unit,dismiss:()->Unit) {
    var query by remember {mutableStateOf("")};var category by remember {mutableStateOf("Novos")}
    val styles=matchingStyles(state,category,query)
    ModalBottomSheet(onDismissRequest=dismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.90f).navigationBarsPadding().padding(horizontal=20.dp).testTag("style-browser"),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {Text("Sua próxima estética",Modifier.weight(1f),fontSize=26.sp,fontFamily=FontFamily.Serif);IconButton(onClick=dismiss) {Icon(Icons.Rounded.Close,"Fechar estilos")}}
            OutlinedTextField(query,{query=it.take(100)},Modifier.fillMaxWidth().testTag("style-search"),singleLine=true,placeholder={Text("Buscar: social, papel, noite…",fontSize=14.sp)},leadingIcon={Icon(Icons.Rounded.Search,null)})
            CatalogCategories(styleCategories(state),category,{category=it},"style-categories")
            Text("${styles.size} estilos · toque para aplicar",color=Muted,fontSize=14.sp)
            LazyVerticalGrid(GridCells.Adaptive(145.dp),Modifier.weight(1f).testTag("style-grid"),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=20.dp)) {
                items(styles,key={it.meta.id}) {style->StyleCard(style,state,vm,Modifier.fillMaxWidth()) {choose(style)}}
                if(styles.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {Text("Nenhum estilo encontrado. Tente outra busca ou categoria.",color=Muted,modifier=Modifier.padding(vertical=24.dp))}
            }
        }
    }
}

@Composable fun StyleLab(state:StudioUi,vm:StudioViewModel,choose:(StylePreset)->Unit,modifier:Modifier) {
    var query by remember {mutableStateOf("")};var category by remember {mutableStateOf("Novos")}
    val styles=matchingStyles(state,category,query)
    LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {Text("Style Lab",fontSize=30.sp,fontFamily=FontFamily.Serif)}
        item {Text("Explore combinações prontas ou salve sua assinatura pela aba Estilos do editor.",color=Muted,fontSize=14.sp)}
        item {OutlinedTextField(query,{query=it.take(100)},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Buscar estilos")},leadingIcon={Icon(Icons.Rounded.Search,null)})}
        item {CatalogCategories(styleCategories(state),category,{category=it},"lab-style-categories")}
        item {Text("${styles.size} estilos · escolha sua foto ao tocar",color=Muted,fontSize=14.sp)}
        items(styles.chunked(2),key={it.first().meta.id}) {row->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            row.forEach {style->StyleCard(style,state,vm,Modifier.weight(1f)) {choose(style)}}
            if(row.size==1) Spacer(Modifier.weight(1f))
        }}
        if(styles.isEmpty()) item {Text("Nenhum estilo encontrado.",color=Muted)}
    }
}

@Composable fun Packs(state:StudioUi,vm:StudioViewModel,modifier:Modifier) {
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {it?.let(vm::importPack)}
    val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {it?.let(vm::exportLibrary)}
    val custom=state.library
    var resources by remember {mutableStateOf(false)}
    var remove by remember {mutableStateOf<Pair<String,String>?>(null)}
    LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {Text("Sua biblioteca cresce.",fontSize=28.sp)}
        item {Text("Importe packs Luma de filtros, estilos e templates. Seus conteúdos ficam disponíveis nos mesmos editores.",color=Muted,fontSize=14.sp)}
        item {Button(onClick={importer.launch(arrayOf("*/*"))},modifier=Modifier.fillMaxWidth()) {Text("Importar pack")}}
        item {OutlinedButton(onClick={exporter.launch("Minha_biblioteca.lumapack")},enabled=custom.filters.isNotEmpty() || custom.effects.isNotEmpty() || custom.templates.isNotEmpty() || custom.styles.isNotEmpty() || custom.resources.isNotEmpty(),modifier=Modifier.fillMaxWidth()) {Text("Exportar minha biblioteca")}}
        item {Text("${custom.filters.size} filtros · ${custom.effects.size} efeitos · ${custom.templates.size} templates · ${custom.styles.size} estilos pessoais",color=Acid,fontSize=14.sp)}
        item {Text("O pack exportado inclui imagens que você manteve fixas nos templates. Fotos dos projetos e imagens marcadas como espaços preenchíveis ficam fora dele.",color=Muted,fontSize=14.sp)}
        item {OutlinedButton(onClick={resources=true},modifier=Modifier.fillMaxWidth()) {Text("Fontes, LUTs, stickers e texturas")}}
        item {Text("Itens pessoais",fontSize=17.sp)}
        items(custom.filters.map {it.meta}+custom.styles.map {it.meta}+custom.templates.map {it.meta},key={it.id}) {meta->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Text(meta.name,Modifier.weight(1f),fontSize=14.sp);TextButton(onClick={vm.duplicateLibrary(meta.id)}) {Text("Duplicar",fontSize=14.sp)};TextButton(onClick={remove=meta.id to meta.name}) {Text("Excluir",fontSize=14.sp)}}}
        item {Text("Packs instalados",fontSize=17.sp)}
        if(custom.packs.isEmpty()) item {Text("Seus packs importados aparecerão aqui.",color=Muted,fontSize=14.sp)}
        items(custom.packs,key={it.id}) {pack ->Row(Modifier.fillMaxWidth().background(Panel).padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {Icon(Icons.Rounded.Inventory2,null,tint=Lilac);Column(Modifier.weight(1f)) {Text(pack.name);Text("${pack.itemIds.size} itens adicionados",color=Muted,fontSize=14.sp)};TextButton(onClick={remove=pack.id to pack.name}) {Text("Remover")}}}
    }
    if(resources) ResourceDialog(state,vm) {resources=false}
    remove?.let {(id,name)->AlertDialog(onDismissRequest={remove=null},title={Text("Remover $name?")},text={Text("Os projetos já criados continuam editáveis com os recursos que usaram.")},confirmButton={TextButton(onClick={vm.removeLibrary(id);remove=null}) {Text("Remover")}},dismissButton={TextButton(onClick={remove=null}) {Text("Cancelar")}})}
}
