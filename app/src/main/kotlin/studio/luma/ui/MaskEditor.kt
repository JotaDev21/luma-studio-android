package studio.luma.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import androidx.compose.ui.semantics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.luma.*
import studio.luma.core.*
import studio.luma.renderer.MaskRasterizer
import studio.luma.renderer.ImageSource
import kotlin.math.*

@Composable fun MaskEditor(state:StudioUi,vm:StudioViewModel) {
    val edit=vm.maskPhotoEdit();val source=state.maskSource ?: return
    val selected=edit.masks.firstOrNull {it.id==state.selectedMask} ?: edit.masks.lastOrNull()
    var showArea by remember {mutableStateOf(true)}
    var tab by remember {mutableStateOf("Área")}
    var radius by remember {mutableFloatStateOf(.05f)}
    var opacity by remember {mutableFloatStateOf(1f)}
    var erase by remember {mutableStateOf(false)}
    var stack by remember {mutableStateOf(false)}
    var rename by remember {mutableStateOf(false)}
    var adjustment by remember {mutableStateOf("Exposição")}
    var colorPro by remember {mutableStateOf(false)}
    var maskLook by remember {mutableStateOf(false)}
    var automatic by remember {mutableStateOf(false)}
    var original by remember {mutableStateOf(false)}
    val scrollLayout=useScrollableEditor()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().then(if(scrollLayout) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=vm::closeMasks) {Icon(Icons.AutoMirrored.Rounded.ArrowBack,"Voltar à edição")}
            Column(Modifier.weight(1f)) {Text("Máscaras",fontSize=17.sp);Text("Ajustes só na área escolhida",color=Muted,fontSize=14.sp)}
            IconButton(onClick={original=!original}) {Icon(Icons.Rounded.Compare,"Comparar original",tint=if(original) Acid else Muted)}
            IconButton(onClick=vm::undo,enabled=state.canUndo) {Icon(Icons.AutoMirrored.Rounded.Undo,"Desfazer")}
            IconButton(onClick=vm::redo,enabled=state.canRedo) {Icon(Icons.AutoMirrored.Rounded.Redo,"Refazer")}
        }
        MaskPhoto(state,edit,selected,radius,opacity,erase,showArea,original,vm,(if(scrollLayout) Modifier.height(240.dp) else Modifier.weight(1f)).fillMaxWidth().padding(12.dp))
        LazyRow(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            item {TextButton(onClick={showArea=!showArea}) {Text(if(showArea) "Ocultar área" else "Mostrar área",fontSize=14.sp)}}
            item {TextButton(onClick={automatic=true},enabled=edit.masks.size<8) {Text("Pessoa / fundo",fontSize=14.sp)}}
            item {TextButton(onClick={stack=true}) {Text("${edit.masks.size}/8 máscaras",fontSize=14.sp)}}
            if(state.rendering) item {LumaProgressIndicator(Modifier.size(16.dp),strokeWidth=2.dp,color=Acid)}
        }
        LazyRow(Modifier.testTag("mask-add"),contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            items(listOf("Pincel" to "brush","Circular" to "radial","Degradê" to "linear")) {(name,type)->OutlinedButton(onClick={vm.addMask(type)},enabled=edit.masks.size<8) {Text("+ $name",fontSize=14.sp)}}
        }
        if(selected==null) {
            Text("Crie uma máscara. Pinte com o pincel ou arraste os pontos da área circular e do degradê.",Modifier.padding(20.dp),color=Muted,fontSize=14.sp)
        } else {
            LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                items(edit.masks,key={it.id}) {mask->FilterChip(mask.id==selected.id,{vm.selectMask(mask.id)},label={Text(mask.name+(if(!mask.enabled) " · oculta" else ""),fontSize=14.sp)})}
            }
            Row(Modifier.padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
                FilterChip(tab=="Área",{tab="Área"},label={Text("Área",fontSize=14.sp)})
                Spacer(Modifier.width(8.dp));FilterChip(tab=="Ajuste",{tab="Ajuste"},label={Text("Ajuste",fontSize=14.sp)})
                Spacer(Modifier.weight(1f));TextButton(onClick={rename=true}) {Text("Renomear",fontSize=14.sp)}
            }
            Column(Modifier.fillMaxWidth().heightIn(max=if(scrollLayout) 480.dp else 256.dp).padding(horizontal=20.dp).verticalScroll(rememberScrollState())) {
                if(tab=="Ajuste") {
                    val spec=adjustments.first {it.name==adjustment}
                    CategoryRow(adjustments.map {it.name},adjustment) {adjustment=it}
                    MaskSlider(spec.name,spec.read(selected.adjustments),spec.min..spec.max,{v->vm.changeMask(selected.id,false) {it.copy(adjustments=spec.write(it.adjustments,v))}},vm::finishGesture,"local-adjustment")
                    LazyRow {item {TextButton(onClick={colorPro=true}) {Text("Cor Pro")}};item {TextButton(onClick={maskLook=true}) {Text("Filtros e efeitos")}}}
                    GradientControls(selected.fill) {fill->vm.changeMask(selected.id) {it.copy(fill=fill)}}
                    TextButton(onClick={vm.changeMask(selected.id) {it.copy(adjustments=Adjustments(),color=ColorTreatment(),effects=emptyList(),filter=null,fill=null)}}) {Text("Limpar ajustes desta máscara",fontSize=14.sp)}
                } else {
                    when(selected.type) {
                        "brush","person"->{
                            if(selected.type=="person") {
                                Text("Corrija a seleção pintando ou apagando. Para manter o recorte transparente, exporte em PNG ou WebP.",color=Muted,fontSize=14.sp)
                                MaskSlider("Limite da seleção",selected.threshold,.05f.. .95f,{v->vm.changeMask(selected.id,false) {it.copy(threshold=v)}},vm::finishGesture)
                                Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(selected.cutout,{v->vm.changeMask(selected.id) {it.copy(cutout=v)}});Text("Manter só a área selecionada",fontSize=14.sp)}
                            }
                            CategoryRow(listOf("Pintar","Apagar"),if(erase) "Apagar" else "Pintar") {erase=it=="Apagar"}
                            MaskSlider("Tamanho do pincel",radius,.002f.. .3f,{radius=it},{})
                            MaskSlider("Fluxo do pincel",opacity,.05f..1f,{opacity=it},{})
                            TextButton(onClick={vm.changeMask(selected.id) {it.copy(strokes=emptyList())}}) {Text(if(selected.type=="person") "Limpar correções manuais" else "Limpar traços",fontSize=14.sp)}
                        }
                        "radial"->{
                            Text("Arraste o centro na foto para posicionar.",color=Muted,fontSize=14.sp)
                            MaskSlider("Centro horizontal",selected.center.x,0f..1f,{v->vm.changeMask(selected.id,false) {it.copy(center=it.center.copy(x=v))}},vm::finishGesture)
                            MaskSlider("Centro vertical",selected.center.y,0f..1f,{v->vm.changeMask(selected.id,false) {it.copy(center=it.center.copy(y=v))}},vm::finishGesture)
                            MaskSlider("Raio horizontal",selected.radiusX,.002f..1f,{v->vm.changeMask(selected.id,false) {it.copy(radiusX=v)}},vm::finishGesture)
                            MaskSlider("Raio vertical",selected.radiusY,.002f..1f,{v->vm.changeMask(selected.id,false) {it.copy(radiusY=v)}},vm::finishGesture)
                        }
                        "linear"->{
                            Text("Use os pontos na foto ou os controles abaixo. O início recebe o ajuste; o fim encerra a transição.",color=Muted,fontSize=14.sp)
                            MaskSlider("Início horizontal",selected.start.x,0f..1f,{v->vm.changeMask(selected.id,false) {it.copy(start=it.start.copy(x=v))}},vm::finishGesture)
                            MaskSlider("Início vertical",selected.start.y,0f..1f,{v->vm.changeMask(selected.id,false) {it.copy(start=it.start.copy(y=v))}},vm::finishGesture)
                            MaskSlider("Fim horizontal",selected.end.x,0f..1f,{v->vm.changeMask(selected.id,false) {it.copy(end=it.end.copy(x=v))}},vm::finishGesture)
                            MaskSlider("Fim vertical",selected.end.y,0f..1f,{v->vm.changeMask(selected.id,false) {it.copy(end=it.end.copy(y=v))}},vm::finishGesture)
                        }
                    }
                    MaskSlider("Suavização",selected.feather,0f..1f,{v->vm.changeMask(selected.id,false) {it.copy(feather=v)}},vm::finishGesture)
                    Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(selected.inverted,{v->vm.changeMask(selected.id) {it.copy(inverted=v)}});Text("Inverter área",fontSize=14.sp)}
                }
                MaskSlider("Intensidade",selected.strength,0f..1f,{v->vm.changeMask(selected.id,false) {it.copy(strength=v)}},vm::finishGesture)
            }
        }
        Button(onClick=vm::closeMasks,modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp)) {Text("Concluir máscaras")}
    }
    if(colorPro && selected!=null) ColorDialog(selected.color,{colorPro=false},vm::finishGesture,state.maskPreview) {color,commit->vm.changeMask(selected.id,commit) {it.copy(color=color)}}
    if(maskLook && selected!=null) MaskLooks(selected,state,vm) {maskLook=false}
    if(automatic) AlertDialog(onDismissRequest={automatic=false},title={Text("Pessoa e fundo")},text={Column {
        Text("A seleção é feita no celular. Funciona melhor em retratos; confira a área marcada e use o pincel para corrigir bordas.",color=Muted,fontSize=14.sp)
        TextButton(onClick={automatic=false;vm.automaticMask()}) {Text("Selecionar pessoa")}
        TextButton(onClick={automatic=false;vm.automaticMask(background=true)}) {Text("Selecionar fundo")}
        TextButton(onClick={automatic=false;vm.automaticMask(cutout=true)}) {Text("Remover fundo")}
    }},confirmButton={TextButton(onClick={automatic=false}) {Text("Fechar")}})
    if(stack) AlertDialog(onDismissRequest={stack=false},title={Text("Máscaras · ordem de aplicação")},text={LazyColumn {
        items(edit.masks,key={it.id}) {mask->Column {
            TextButton(onClick={vm.selectMask(mask.id);stack=false}) {Text(mask.name)}
            Row {
                IconButton(onClick={vm.changeMask(mask.id) {it.copy(enabled=!it.enabled)}}) {Icon(if(mask.enabled) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,"Ativar máscara")}
                IconButton(onClick={vm.moveMask(mask.id,-1)},enabled=edit.masks.first()!=mask) {Icon(Icons.Rounded.ArrowUpward,"Aplicar antes")}
                IconButton(onClick={vm.moveMask(mask.id,1)},enabled=edit.masks.last()!=mask) {Icon(Icons.Rounded.ArrowDownward,"Aplicar depois")}
                IconButton(onClick={vm.deleteMask(mask.id)}) {Icon(Icons.Rounded.Delete,"Excluir máscara")}
            }
        }}
    }},confirmButton={TextButton(onClick={stack=false}) {Text("Fechar")}})
    if(rename && selected!=null) {
        var name by remember(selected.id) {mutableStateOf(selected.name)}
        AlertDialog(onDismissRequest={rename=false},title={Text("Nome da máscara")},text={OutlinedTextField(name,{name=it.take(100)},singleLine=true)},confirmButton={TextButton(onClick={vm.changeMask(selected.id) {it.copy(name=name.trim())};rename=false},enabled=name.isNotBlank()) {Text("Salvar nome")}},dismissButton={TextButton(onClick={rename=false}) {Text("Cancelar")}})
    }
}

@Composable private fun MaskSlider(label:String,value:Float,range:ClosedFloatingPointRange<Float>,change:(Float)->Unit,finish:()->Unit,tag:String=label) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {Text(label,fontSize=14.sp,modifier=Modifier.weight(1f));Text(java.lang.String.format(java.util.Locale.ROOT,"%.2f",value),fontSize=14.sp,color=Acid)}
    Slider(value,change,onValueChangeFinished=finish,valueRange=range,modifier=Modifier.heightIn(min=48.dp).testTag(tag).semantics {contentDescription=label})
}

@Composable private fun MaskPhoto(state:StudioUi,edit:EditState,mask:LocalMask?,radius:Float,opacity:Float,erase:Boolean,showArea:Boolean,showOriginal:Boolean,vm:StudioViewModel,modifier:Modifier) {
    val source=state.maskSource ?: return
    val photo=state.maskPreview ?: return Box(modifier,contentAlignment=Alignment.Center) {LumaProgressIndicator(color=Acid)}
    var live by remember(mask?.id) {mutableStateOf<List<Point>>(emptyList())}
    val geometry=mask?.geometry()
    val overlay by produceState<android.graphics.Bitmap?>(null,geometry,edit.transform,photo.width,photo.height,mask?.inverted,showArea) {
        value=null
        if(showArea && mask!=null) value=withContext(Dispatchers.Default) {MaskRasterizer.create(mask,edit.transform,source.width,source.height,photo.width,photo.height,mask.inverted)}
    }
    val latestMask by rememberUpdatedState(mask)
    val latestTransform by rememberUpdatedState(edit.transform)
    val latestRadius by rememberUpdatedState(radius)
    val latestOpacity by rememberUpdatedState(opacity)
    val latestErase by rememberUpdatedState(erase)
    val original by produceState<android.graphics.Bitmap?>(null,source,edit.transform,showOriginal) {
        value=if(showOriginal) withContext(Dispatchers.Default) {ImageSource.transform(source,edit.transform)} else null
    }
    BoxWithConstraints(modifier,contentAlignment=Alignment.Center) {
        val ratio=photo.width.toFloat()/photo.height
        val width=minOf(maxWidth,maxHeight*ratio)
        Box(Modifier.width(width).aspectRatio(ratio).clipToBounds().testTag("mask-photo")) {
            Canvas(Modifier.fillMaxSize()) {
                val cell=16.dp.toPx()
                for(y in 0..(size.height/cell).toInt()) for(x in 0..(size.width/cell).toInt()) drawRect(if((x+y)%2==0) Color(0xFF34343A) else Color(0xFF25252A),Offset(x*cell,y*cell),androidx.compose.ui.geometry.Size(cell,cell))
            }
            Image((original ?: photo).asImageBitmap(),if(showOriginal) "Foto original para corrigir seleção" else "Foto com ajustes locais",Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
            if(mask?.enabled==true) overlay?.let {Image(it.asImageBitmap(),null,Modifier.fillMaxSize().graphicsLayer(alpha=.35f*mask.strength),colorFilter=ColorFilter.tint(Color(0xFFFF4B73)),contentScale=ContentScale.FillBounds)}
            Canvas(Modifier.fillMaxSize().pointerInput(mask?.id,source) {
                awaitEachGesture {
                    val down=awaitFirstDown(requireUnconsumed=false)
                    val m=latestMask ?: return@awaitEachGesture
                    val painting=m.type=="brush" || m.type=="person"
                    fun sourcePoint(position:Offset)=MaskCoordinates.toSource(Point((position.x/size.width).coerceIn(0f,1f),(position.y/size.height).coerceIn(0f,1f)),latestTransform)
                    var points=listOf(sourcePoint(down.position))
                    val start=MaskCoordinates.toDisplay(m.start,latestTransform);val end=MaskCoordinates.toDisplay(m.end,latestTransform)
                    val which=(Offset(start.x*size.width,start.y*size.height)-down.position).getDistance()<=(Offset(end.x*size.width,end.y*size.height)-down.position).getDistance()
                    fun move(point:Point) {
                        if(painting) live=points else vm.changeMask(m.id,false) {current->
                            if(m.type=="radial") current.copy(center=point) else if(which && hypot(point.x-current.end.x,point.y-current.end.y)>=.02f) current.copy(start=point) else if(!which && hypot(point.x-current.start.x,point.y-current.start.y)>=.02f) current.copy(end=point) else current
                        }
                    }
                    down.consume();move(points.last())
                    var released=false
                    do {
                        val event=awaitPointerEvent();val change=event.changes.firstOrNull {it.id==down.id} ?: break
                        if(!change.pressed) {released=true;break}
                        val point=sourcePoint(change.position)
                        if(painting && points.size<2048 && hypot(point.x-points.last().x,point.y-points.last().y)>.001f) points=points+point
                        move(point);change.consume()
                    } while(true)
                    if(painting) {if(released) vm.brushStroke(m.id,BrushStroke(points,latestRadius,latestOpacity,latestErase));live=emptyList()} else vm.finishGesture()
                }
            }) {
                fun display(point:Point):Offset {val p=MaskCoordinates.toDisplay(point,edit.transform);return Offset(p.x*size.width,p.y*size.height)}
                if(showArea && mask!=null) {
                    if(mask.type=="radial") {drawCircle(Color.White,8.dp.toPx(),display(mask.center),style=Stroke(2.dp.toPx()))}
                    if(mask.type=="linear") {drawLine(Color.White,display(mask.start),display(mask.end),2.dp.toPx());drawCircle(Acid,7.dp.toPx(),display(mask.start));drawCircle(Color.White,7.dp.toPx(),display(mask.end))}
                    if(live.isNotEmpty()) {
                        val c=if(erase) Color.White.copy(alpha=.5f) else Color(0xFFFF4B73).copy(alpha=.5f)
                        val crop=edit.transform.crop
                        val cropWidth=if(edit.transform.quarterTurns%2==0) source.width*(crop.right-crop.left) else source.height*(crop.bottom-crop.top)
                        val brush=radius*minOf(source.width,source.height)*size.width/cropWidth
                        live.zipWithNext().forEach {(a,b)->drawLine(c,display(a),display(b),brush*2,StrokeCap.Round)}
                        drawCircle(c,brush,display(live.last()))
                    }
                }
            }
        }
    }
}

@Composable private fun MaskLooks(mask:LocalMask,state:StudioUi,vm:StudioViewModel,dismiss:()->Unit) {
    var tab by remember {mutableStateOf("Efeitos")}
    AlertDialog(onDismissRequest=dismiss,title={Text("Tratamento da área")},text={Column {
        CategoryRow(listOf("Efeitos","Filtros"),tab) {tab=it}
        LazyColumn(Modifier.heightIn(max=320.dp)) {
            if(tab=="Efeitos") {
                items(mask.effects,key={it.id}) {e->Column {Text(e.preset.meta.name,color=Acid);AdvancedSlider("Intensidade",e.intensity,0f..1f,{v->vm.changeMask(mask.id,false) {m->m.copy(effects=m.effects.map {if(it.id==e.id) it.copy(intensity=v) else it})}},vm::finishGesture);TextButton(onClick={vm.changeMask(mask.id) {m->m.copy(effects=m.effects.filterNot {it.id==e.id})}}) {Text("Remover")}}}
                items(state.effects.filter {it.asset==null && it.type!="timestamp"},key={it.meta.id}) {preset->TextButton(onClick={if(mask.effects.size<16) vm.changeMask(mask.id) {it.copy(effects=it.effects+EffectLayer(java.util.UUID.randomUUID().toString(),preset))}}) {Text("+ ${preset.meta.name}")}}
            } else {
                item {TextButton(onClick={vm.changeMask(mask.id) {it.copy(filter=null)}}) {Text("Original")}}
                items(state.filters.filter {it.assetNames().isEmpty()},key={it.meta.id}) {preset->TextButton(onClick={vm.changeMask(mask.id) {it.copy(filter=FilterSelection(preset))}}) {Text(preset.meta.name)}}
            }
        }
    }},confirmButton={TextButton(onClick=dismiss) {Text("Pronto")}})
}
