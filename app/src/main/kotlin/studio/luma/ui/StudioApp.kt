@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package studio.luma.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.luma.*
import studio.luma.core.*
import studio.luma.data.*
import java.io.File

@Composable fun StudioApp(vm: StudioViewModel) = LumaTheme {
    val state by vm.ui.collectAsStateWithLifecycle()
    var pendingFilter by remember { mutableStateOf<FilterPreset?>(null) }
    var pendingStyle by remember {mutableStateOf<StylePreset?>(null)}
    var collage by remember {mutableStateOf(false)}
    var batch by remember {mutableStateOf(false)}
    val messages=remember {SnackbarHostState()}
    LaunchedEffect(state.notice) {state.notice?.let {messages.showSnackbar(it);vm.clearNotice()}}
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.import(it, pendingFilter,pendingStyle) }; pendingFilter = null;pendingStyle=null
    }
    val choose: (FilterPreset?) -> Unit = { pendingFilter = it;pendingStyle=null; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val chooseStyle:(StylePreset)->Unit={pendingStyle=it;pendingFilter=null;picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}
    val back = { if (!state.busy) vm.keepDraftAndClose(); Unit }
    BackHandler(state.page == Page.EDITOR) { if(state.maskEditor) vm.closeMasks() else back() }
    BackHandler(state.page != Page.HOME && state.page != Page.EDITOR) { vm.page(Page.HOME) }
    Surface(Modifier.fillMaxSize(), color = Ink) {
        Box(Modifier.fillMaxSize()) {
            if (state.page == Page.EDITOR) {
                if(state.maskEditor) MaskEditor(state,vm) else if (state.edit.story != null) StoryEditor(state, vm, back) else PhotoStudio(state, vm, back)
            } else Scaffold(
                containerColor = Ink,
                bottomBar = {
                    NavigationBar(containerColor = Ink, tonalElevation = 0.dp) {
                        listOf(Triple(Page.HOME, "Início", Icons.Rounded.Home), Triple(Page.LIBRARY, "Filtros", Icons.Rounded.AutoAwesome), Triple(Page.TEMPLATES, "Stories", Icons.Rounded.Dashboard), Triple(Page.PROJECTS, "Projetos", Icons.Rounded.FolderOpen)).forEach { (page, label, icon) ->
                            NavigationBarItem(selected = state.page == page, onClick = { vm.page(page) }, icon = { Icon(icon, null) }, label = { Text(label) }, colors = NavigationBarItemDefaults.colors(indicatorColor = Panel, selectedIconColor = Acid, selectedTextColor = Acid))
                        }
                    }
                }
            ) { padding ->
                when (state.page) {
                    Page.HOME -> Home(state, vm, choose, {collage=true},{batch=true},Modifier.padding(padding))
                    Page.LIBRARY -> Library(state, vm, choose, Modifier.padding(padding))
                    Page.PROJECTS -> Projects(state, vm, Modifier.padding(padding))
                    Page.TEMPLATES -> Templates(state, vm, Modifier.padding(padding))
                    Page.STYLES -> StyleLab(state,vm,chooseStyle,Modifier.padding(padding))
                    Page.PACKS -> Packs(state,vm,Modifier.padding(padding))
                    else -> Unit
                }
            }
            if (state.busy) Box(Modifier.fillMaxSize().background(Ink.copy(alpha = .85f)).clickable(enabled = true, onClick = {}), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LumaProgressIndicator(color = Acid)
                    Text(if(state.enhancing) "Seu PC está melhorando a foto com IA…" else if(state.batchTotal>0) "Lote: ${state.batchProgress} de ${state.batchTotal} fotos" else "Processando no seu dispositivo", color = Muted)
                    if(state.enhancing) TextButton(onClick=vm::cancelEnhancement) {Text("Cancelar melhoria")}
                    if(state.batchTotal>0) TextButton(onClick=vm::cancelBatch) {Text("Parar após esta foto")}
                }
            }
            SnackbarHost(messages,Modifier.align(Alignment.BottomCenter).padding(bottom=96.dp))
        }
    }
    SmartProposalDialog(state,vm)
    PairingConfirmation(state,vm)
    if(batch) BatchDialog(state,vm) {batch=false}
    if(collage) CollageDialog(vm) {collage=false}
    state.error?.let { error -> AlertDialog(onDismissRequest = vm::clearError, title = { Text("Não foi possível concluir") }, text = { Text(error) }, confirmButton = { TextButton(onClick = vm::clearError) { Text("Entendi") } }) }
}

@Composable private fun Wordmark() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Camera, null, tint = Acid, modifier = Modifier.size(26.dp))
            Text("luma", fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
            Text("STUDIO", color = Muted, fontSize=14.sp, letterSpacing = 2.sp)
        }
        Text("Seu estúdio", color = Acid, fontSize=14.sp)
    }
}

@Composable private fun Home(state: StudioUi, vm: StudioViewModel, choose: (FilterPreset?) -> Unit, collage:()->Unit,batch:()->Unit,modifier: Modifier) {
    var help by rememberSaveable {mutableStateOf(false)}
    var accessibility by rememberSaveable {mutableStateOf(false)}
    LazyColumn(modifier.fillMaxSize().testTag("home-screen"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Wordmark() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("O que você quer criar?", fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                Text("Escolha uma opção para começar.", color = Muted, fontSize = 14.sp)
            }
        }
        item {Column(Modifier.testTag("home-actions"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            HomeAction("Editar foto","Escolha uma foto e mude o visual",Icons.Rounded.AddPhotoAlternate,"home-photo",Acid) {choose(null)}
            HomeAction("Criar Story","Escolha um modelo ou comece em branco",Icons.Rounded.Dashboard,"home-story",Lilac) {vm.page(Page.TEMPLATES)}
            HomeAction("Fazer colagem","Junte várias fotos em uma imagem",Icons.Rounded.GridView,"home-collage",Acid,collage)
        }}
        item {SectionLabel("Continue de onde parou")}
        if (state.projects.isEmpty()) item {EmptyState("Seus projetos aparecerão aqui.","Suas edições têm rascunho automático.",Icons.Rounded.FolderOpen)}
        else items(state.projects.take(3), key = { it.id }) { ProjectRow(it, vm.thumbFile(it)) { vm.open(it) } }
        item {SectionLabel("Sua biblioteca")}
        item {HomeAction("Estilos prontos","Combine filtros e efeitos",Icons.Rounded.Palette,"home-styles",Lilac) {vm.page(Page.STYLES)}}
        item {HomeAction("Editar várias fotos","Aplique um estilo em até 20 fotos",Icons.Rounded.Collections,"home-batch",Acid,batch)}
        item {HomeAction("Importar e guardar recursos","Seus filtros, modelos e arquivos",Icons.Rounded.Inventory2,"home-packs",Lilac) {vm.page(Page.PACKS)}}
        item {TextButton(onClick={help=true}) {Icon(Icons.AutoMirrored.Rounded.HelpOutline,null,modifier=Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("Como usar o Luma")}}
        item {TextButton(onClick={accessibility=true}) {Text("Facilidade de uso")}}
    }
    if(help) StudioHelpDialog {help=false}
    if(accessibility) AccessibilityDialog {accessibility=false}
}

@Composable private fun HomeAction(title:String,description:String,icon:ImageVector,tag:String,color:Color,action:()->Unit) {
    val largeText=androidx.compose.ui.platform.LocalDensity.current.fontScale>=1.3f
    Row(Modifier.fillMaxWidth().heightIn(min=64.dp).clip(RoundedCornerShape(12.dp)).background(Panel).clickable(onClickLabel=title,onClick=action).testTag(tag).padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Icon(icon,null,tint=color,modifier=Modifier.size(22.dp))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {Text(title,fontSize=17.sp,fontWeight=FontWeight.SemiBold);Text(description,fontSize=13.sp,color=Muted,lineHeight=18.sp,maxLines=if(largeText) 3 else 1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}
        Icon(Icons.AutoMirrored.Rounded.ArrowForward,null,tint=color,modifier=Modifier.size(18.dp))
    }
}

@Composable private fun PosterArt(modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Brush.linearGradient(listOf(Color(0xFF414E3B), Color(0xFF182821), Color(0xFF766E4D))))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFD2DFA1).copy(alpha = .7f), Color.Transparent)), radius = size.width * .6f, center = Offset(size.width * .95f, size.height * .1f))
        for (i in 0..18) {
            val x = size.width * .4f + i * size.width * .045f
            drawLine(Color(0xFFA9BC8F).copy(alpha = .22f), Offset(x, size.height * .02f), Offset(x - size.width * .3f, size.height), strokeWidth = 1.5f)
        }
        drawCircle(Color(0xFFAEC289).copy(alpha = .12f), size.width * .35f, Offset(size.width * .9f, size.height * .9f), style = androidx.compose.ui.graphics.drawscope.Stroke(40f))
    }
}

@Composable private fun ActionTile(title: String, subtitle: String, icon: ImageVector, color: Color, modifier: Modifier, action: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Panel).clickable(onClick = action).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, title, tint = color); Text(title, fontWeight = FontWeight.Medium); Text(subtitle, color = Muted, fontSize=14.sp)
    }
}
@Composable private fun SectionLabel(title: String, extra: String = "") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp); Text(extra, color = Muted, fontSize=14.sp) }
}
private fun palette(category: String): List<Color> = when (category) {
    "Film" -> listOf(Color(0xFF384736), Color(0xFFB4A080), Color(0xFFDFCAA5))
    "CCD" -> listOf(Color(0xFF24394B), Color(0xFF668A92), Color(0xFFCEC3A6))
    "Cinema" -> listOf(Color(0xFF1D5359), Color(0xFF687866), Color(0xFFD59563))
    "Y2K" -> listOf(Color(0xFF4A3770), Color(0xFFA078AD), Color(0xFFF3A2BF))
    "BW" -> listOf(Color(0xFF26262A), Color(0xFF898987), Color(0xFFE3E2DC))
    "Portrait" -> listOf(Color(0xFF704F47), Color(0xFFC59985), Color(0xFFF0DCC5))
    else -> listOf(Color(0xFF557A71), Color(0xFFACC0A0), Color(0xFFE4DABD))
}
@Composable private fun PaletteCard(preset: FilterPreset, favorite: Boolean, choose: () -> Unit, toggle: () -> Unit) {
    Column(Modifier.width(132.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(palette(preset.meta.category))).combinedClickable(onClick = choose, onLongClick = toggle)) {
            Text(preset.meta.category.uppercase(), Modifier.align(Alignment.BottomStart).padding(12.dp), color = Color.White, fontSize=14.sp, letterSpacing = 1.sp)
            IconButton(onClick = toggle, modifier = Modifier.align(Alignment.TopEnd)) { Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favoritar ${preset.meta.name}", tint = if (favorite) Acid else Color.White, modifier = Modifier.size(19.dp)) }
        }
        Text(preset.meta.name, fontSize=14.sp, maxLines = 1)
        Text("Paleta do estilo", color = Muted, fontSize=14.sp)
    }
}

@Composable private fun Library(state: StudioUi, vm: StudioViewModel, choose: (FilterPreset?) -> Unit, modifier: Modifier) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Todos") }
    val filtered = state.filters.filter { (category == "Todos" || category == "Favoritos" && it.meta.id in state.favorites || it.meta.category == category) && catalogMatches(it.meta,query) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Text("Encontre seu look.", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        item { Text("Tratamentos originais. Uma foto, muitas possibilidades.", color = Muted, fontSize=14.sp) }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Buscar nome, categoria ou tags") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, shape = RoundedCornerShape(16.dp), singleLine = true) }
        item { CatalogCategories(listOf("Todos","Favoritos","Instagram","JDM") + state.filters.map {it.meta.category},category,{category=it},"filter-library-categories") }
        item { Text("${filtered.size} filtros · toque para escolher uma foto", color = Muted, fontSize=14.sp) }
        if (filtered.isEmpty()) item { EmptyState("Nenhum filtro encontrado", "Tente outra busca ou adicione favoritos.", Icons.Rounded.Search) }
        items(filtered.chunked(2)) { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            row.forEach { preset -> Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Panel).clickable { choose(preset) }.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth().height(115.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(palette(preset.meta.category)))) {
                    Text(preset.meta.category.uppercase(), Modifier.align(Alignment.BottomStart).padding(12.dp), fontSize=14.sp, letterSpacing = 1.sp)
                    IconButton(onClick = { vm.toggleFavorite(preset.meta.id) }, modifier = Modifier.align(Alignment.TopEnd)) { Icon(if (preset.meta.id in state.favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favoritar ${preset.meta.name}", tint = Acid, modifier = Modifier.size(19.dp)) }
                }
                Text(preset.meta.name, fontSize=14.sp, fontWeight = FontWeight.Medium)
                Text("Paleta · preview na sua foto", color = Muted, fontSize=14.sp)
            } }; if (row.size == 1) Spacer(Modifier.weight(1f))
        } }
    }
}

@Composable private fun Projects(state: StudioUi, vm: StudioViewModel, modifier: Modifier) {
    var deleting by remember {mutableStateOf<Project?>(null)}
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Seu arquivo criativo.", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        item { Text("Originais preservados. Edições sempre abertas.", color = Muted, fontSize=14.sp) }
        if (state.projects.isEmpty()) item { EmptyState("Nenhum projeto ainda", "Selecione uma foto na tela inicial.", Icons.Rounded.FolderOpen) }
        items(state.projects, key = { it.id }) {p->Column {ProjectRow(p, vm.thumbFile(p)) { vm.open(p) };TextButton(onClick={deleting=p}) {Text("Excluir projeto",fontSize=14.sp)}}}
    }
    deleting?.let {project->AlertDialog(onDismissRequest={deleting=null},title={Text("Excluir ${project.name}?")},text={Text("A cópia editável será removida. Fotos originais e exportações da galeria permanecem.")},confirmButton={TextButton(onClick={vm.deleteProject(project.id);deleting=null}) {Text("Excluir")}},dismissButton={TextButton(onClick={deleting=null}) {Text("Cancelar")}})}
}
@Composable private fun ProjectRow(project: Project, file: File, action: () -> Unit) {
    val image by produceState<Bitmap?>(null, file.path, project.updatedAt) { value = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.path) } }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).clickable(onClick = action).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(66.dp).clip(RoundedCornerShape(10.dp)).background(Color.DarkGray)) { image?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(project.name, fontSize = 16.sp, maxLines = 2)
            Text(if (project.edit.story != null) "Story · ${project.edit.story?.layers?.size} camadas" else project.edit.filter?.preset?.meta?.name ?: "Foto", color = Muted, fontSize = 14.sp)
            Text("Toque para continuar", color = Muted, fontSize=14.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Muted)
    }
}
@Composable private fun EmptyState(title: String, subtitle: String, icon: ImageVector) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { Icon(icon, null, tint = Muted); Text(title, fontSize = 14.sp); Text(subtitle, color = Muted, fontSize=14.sp) }
}
@Composable fun CategoryRow(categories: List<String>, selected: String, modifier: Modifier = Modifier, action: (String) -> Unit) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(categories) { category -> FilterChip(selected == category, { action(category) }, label = { Text(category, fontSize = 14.sp) },modifier=Modifier.heightIn(min=48.dp)) } }
}

@Composable internal fun ExportDialog(dismiss: () -> Unit, story: Boolean = false, canvasSize: ExportSize? = null, export: (ExportOptions) -> Unit) {
    var format by rememberSaveable { mutableStateOf(ExportFormat.JPEG) }
    var resolution by rememberSaveable { mutableStateOf("Original") }
    var custom by rememberSaveable { mutableStateOf("2160") }
    var quality by rememberSaveable { mutableFloatStateOf(95f) }
    var metadata by rememberSaveable { mutableStateOf(false) }
    var sizePreset by rememberSaveable {mutableStateOf("Livre")}
    var targetWidth by rememberSaveable {mutableStateOf("1080")}
    var targetHeight by rememberSaveable {mutableStateOf("1920")}
    var advanced by rememberSaveable {mutableStateOf(false)}
    val edge = if(sizePreset=="Personalizado") 0 else when (resolution) { "1080p" -> 1920; "1440p" -> 2560; "4K" -> 3840; "Custom" -> custom.toIntOrNull() ?: -1; else -> 0 }
    val baseSize = ExportSize(targetWidth.toIntOrNull() ?: -1, targetHeight.toIntOrNull() ?: -1)
    val outputSize = when {
        sizePreset=="Livre" -> null
        sizePreset=="Personalizado" || edge !in 0..8192 -> baseSize
        else -> baseSize.atLongEdge(edge)
    }
    val valid = (edge==0 || edge in 64..8192) && (outputSize==null || outputSize.isValid)
    val shownSize = outputSize ?: canvasSize?.let {if(edge in 0..8192) it.atLongEdge(edge) else null}
    AlertDialog(onDismissRequest = dismiss, title = { Text("Salvar foto na galeria", fontSize = 23.sp) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Sua foto será salva como uma cópia. O original fica preservado.",fontSize=16.sp,lineHeight=23.sp)
            if(sizePreset!="Personalizado") {
                Text("Tamanho da foto",fontSize=16.sp)
                CategoryRow(if(advanced) listOf("Original","1080p","1440p","4K","Custom") else listOf("Original","1440p","4K"),resolution,Modifier.testTag("export-resolution")) {resolution=it}
                Text(if(resolution=="Original") "Original é recomendado: mantém o tamanho da sua foto editada." else "O tamanho escolhido amplia ou reduz a imagem; aumentar o tamanho sozinho não recupera detalhes.",fontSize=14.sp,color=Muted)
                if(resolution=="Custom") OutlinedTextField(custom,{custom=it.filter(Char::isDigit).take(4)},label={Text("Maior lado em px (64–8192)")},singleLine=true)
            }
            TextButton(onClick={advanced=!advanced},modifier=Modifier.testTag("export-advanced")) {Text(if(advanced) "Ocultar opções avançadas" else "Opções avançadas",fontSize=16.sp)}
            if(advanced) {
            Text("Dimensões de publicação",fontSize=16.sp)
            CategoryRow(listOf("Livre","Story","Feed","Quadrado","Perfil","Wallpaper","Personalizado"),sizePreset,Modifier.testTag("export-publication")) {value->sizePreset=value;val dimensions=when(value) {"Feed"->1080 to 1350;"Quadrado","Perfil"->1080 to 1080;else->1080 to 1920};targetWidth=dimensions.first.toString();targetHeight=dimensions.second.toString()}
            if(sizePreset!="Livre") {Text("A imagem inteira cabe nas dimensões, com margens se necessário.",color=Muted,fontSize=14.sp);if(sizePreset=="Personalizado") {OutlinedTextField(targetWidth,{targetWidth=it.filter(Char::isDigit).take(4)},label={Text("Largura")});OutlinedTextField(targetHeight,{targetHeight=it.filter(Char::isDigit).take(4)},label={Text("Altura")})}}
            Text("Formato", color = Muted, fontSize=14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ExportFormat.entries.forEach { value -> FilterChip(format == value, { format = value }, label = { Text(value.name) }) } }
            Text(if(format==ExportFormat.JPEG) "JPEG preenche áreas transparentes com branco." else "Este formato mantém áreas transparentes.",color=Muted,fontSize=14.sp)
            }
            if(valid) {
                Text(shownSize?.let {"Saída: ${it.width} × ${it.height} px"} ?: if(edge>0) "Saída: maior lado de $edge px" else "Saída: dimensões originais da foto editada", modifier=Modifier.testTag("export-size"), fontSize=14.sp)
                if(!story && (edge>0 || outputSize!=null)) Text("Fotos menores serão ampliadas para o tamanho escolhido. A ampliação não recupera detalhes ausentes.", color=Muted, fontSize=14.sp)
            } else Text("Escolha dimensões de 64 a 8192 px por lado, até 32 MP.", color=MaterialTheme.colorScheme.error, fontSize=14.sp)
            if(advanced) {
            if (format != ExportFormat.PNG) {
                Text("Qualidade ${quality.toInt()}%", fontSize=14.sp)
                Slider(quality, { quality = it }, valueRange = 1f..100f)
                if (format == ExportFormat.WEBP && quality == 100f) Text("WebP sem perdas no Android 11+", color = Muted, fontSize=14.sp)
            } else Text("PNG preserva transparência e usa compressão sem perdas.", color = Muted, fontSize=14.sp)
            if (format == ExportFormat.JPEG && !story) Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(metadata, { metadata = it }); Text("Incluir EXIF de câmera e data\nSem localização GPS", fontSize=14.sp) }
            }
            Text("Você encontrará a foto na galeria, na pasta Luma Studio. O rascunho editável fica em Projetos.", color = Muted, fontSize = 14.sp,lineHeight=21.sp)
        }
    }, confirmButton = { TextButton(onClick = { export(ExportOptions(format, quality.toInt(), edge, metadata && format == ExportFormat.JPEG,outputSize?.width ?: 0,outputSize?.height ?: 0)) }, enabled = valid) { Text("Salvar na galeria",fontSize=16.sp) } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar",fontSize=16.sp) } })
}
