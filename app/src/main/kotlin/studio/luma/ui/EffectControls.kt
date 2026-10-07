@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package studio.luma.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import studio.luma.StudioUi
import studio.luma.StudioViewModel

@Composable fun EffectControls(state:StudioUi,vm:StudioViewModel) {
    var category by remember { mutableStateOf("Todos") }
    var selected by remember { mutableStateOf<String?>(null) }
    var stack by remember { mutableStateOf(false) }
    var search by remember {mutableStateOf(false)}
    var query by remember {mutableStateOf("")}
    var flashSettings by remember {mutableStateOf(false)}
    val catalog=state.effects.sortedBy {if(it.meta.category=="Flash") 0 else 1}
    val layer=state.edit.effects.firstOrNull { it.id==selected } ?: state.edit.effects.lastOrNull()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Combine seu look",color=Muted,fontSize=11.sp)
            Row {
                IconButton(onClick={search=true},modifier=Modifier.size(32.dp)) {Icon(Icons.Rounded.Search,"Buscar efeitos",modifier=Modifier.size(18.dp))}
                TextButton(onClick={stack=true},contentPadding=PaddingValues(0.dp)) { Text("Camadas (${state.edit.effects.size})",fontSize=11.sp) }
            }
        }
        CatalogCategories(listOf("Todos","Favoritos","Flash")+catalog.map { it.meta.category }.distinct(),category,{category=it},"effect-categories")
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(catalog.filter { category=="Todos" || category=="Favoritos" && it.meta.id in state.favorites || it.meta.category==category },key={it.meta.id}) { preset ->
                Surface(shape=RoundedCornerShape(12.dp),color=Color(0xFF303036)) {
                    Column(Modifier.width(116.dp).testTag("effect-${preset.meta.id}").combinedClickable(onClick={selected=null;vm.addEffect(preset)},onLongClick={vm.toggleFavorite(preset.meta.id)}).padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Icon(when(preset.type) { "flash"->Icons.Rounded.FlashOn;"grain","dust"->Icons.Rounded.Grain; "frame"->Icons.Rounded.CropSquare; "blur"->Icons.Rounded.BlurOn; else->Icons.Rounded.AutoAwesome },null,tint=Lilac,modifier=Modifier.size(20.dp))
                        Text(preset.meta.name,fontSize=10.sp,maxLines=2,modifier=Modifier.heightIn(min=28.dp))
                        Text(if(preset.meta.id in state.favorites) "♥ favorito" else "+ adicionar",color=Muted,fontSize=9.sp)
                    }
                }
            }
        }
        if(layer!=null) {
            LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) { items(state.edit.effects,key={it.id}) { effect -> FilterChip(layer.id==effect.id,{selected=effect.id},label={Text(effect.preset.meta.name,fontSize=10.sp)},modifier=Modifier.testTag("effect-layer-${effect.id}")) } }
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Intensidade",color=Muted,fontSize=10.sp)
                Slider(layer.intensity,{value->vm.changeEffect(layer.id,{it.copy(intensity=value)},false)},Modifier.weight(1f).height(32.dp).testTag("effect-intensity"),onValueChangeFinished={vm.finishGesture()})
                Text("${(layer.intensity*100).toInt()}%",color=Acid,fontSize=10.sp)
            }
            if(layer.preset.type=="flash" && "hardness" in layer.preset.parameters) TextButton(onClick={flashSettings=true},contentPadding=PaddingValues(0.dp),modifier=Modifier.height(28.dp)) {Text("Ajustar luz do flash",fontSize=11.sp)}
        } else Text("Toque para adicionar. Pressione Camadas para ajustar a ordem.",color=Muted,fontSize=10.sp,modifier=Modifier.padding(vertical=10.dp))
    }
    if(search) AlertDialog(onDismissRequest={search=false},title={Text("Buscar efeitos")},text={Column {
        OutlinedTextField(query,{query=it},singleLine=true,placeholder={Text("Nome, categoria ou tags")})
        LazyColumn(Modifier.heightIn(max=320.dp)) {
            items(catalog.filter {preset->catalogMatches(preset.meta,query)},key={it.meta.id}) {preset->Row(verticalAlignment=Alignment.CenterVertically) {
                TextButton(onClick={selected=null;vm.addEffect(preset);search=false},modifier=Modifier.weight(1f)) {Text(preset.meta.name,fontSize=12.sp)}
                IconButton(onClick={vm.toggleFavorite(preset.meta.id)}) {Icon(if(preset.meta.id in state.favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favoritar ${preset.meta.name}",tint=Acid)}
            }}
        }
    }},confirmButton={TextButton(onClick={search=false}) {Text("Fechar")}})
    if(flashSettings && layer!=null && "hardness" in layer.preset.parameters) AlertDialog(onDismissRequest={flashSettings=false},title={Text(layer.preset.meta.name)},text={
        Column(Modifier.heightIn(max=390.dp).verticalScroll(rememberScrollState())) {
            Text("Ajuste a luz na sua foto. A intensidade mistura o efeito inteiro com a edição anterior.",color=Muted,fontSize=12.sp)
            layer.preset.parameters.filterKeys {it!="amount"}.forEach {(key,value)->
                Text(if(key=="radius") "Área iluminada" else effectParameterLabel(key),fontSize=12.sp,modifier=Modifier.padding(top=8.dp))
                Slider(value,{v->vm.changeEffect(layer.id,{it.copy(preset=it.preset.copy(parameters=it.preset.parameters+(key to v)))},false)},onValueChangeFinished={vm.finishGesture()},modifier=Modifier.height(32.dp).testTag("flash-$key"))
            }
        }
    },confirmButton={TextButton(onClick={vm.finishGesture();flashSettings=false}) {Text("Pronto")}})
    if(stack) AlertDialog(onDismissRequest={stack=false},title={Text("Stack de efeitos")},text={
        LazyColumn(Modifier.heightIn(max=430.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            items(state.edit.effects,key={it.id}) { effect ->
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        IconButton(onClick={vm.changeEffect(effect.id,{it.copy(enabled=!it.enabled)})},modifier=Modifier.size(30.dp)) { Icon(if(effect.enabled) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,"Ativar ${effect.preset.meta.name}",tint=Acid) }
                        Text(effect.preset.meta.name,Modifier.weight(1f),fontSize=12.sp)
                        IconButton(onClick={vm.moveEffect(effect.id,-1)},modifier=Modifier.size(30.dp)) { Icon(Icons.Rounded.KeyboardArrowUp,"Subir efeito") }
                        IconButton(onClick={vm.moveEffect(effect.id,1)},modifier=Modifier.size(30.dp)) { Icon(Icons.Rounded.KeyboardArrowDown,"Descer efeito") }
                        IconButton(onClick={vm.removeEffect(effect.id)},modifier=Modifier.size(30.dp)) { Icon(Icons.Rounded.DeleteOutline,"Remover efeito") }
                    }
                    if(effect.preset.type=="timestamp") OutlinedTextField(effect.preset.text,{text->vm.changeEffect(effect.id,{it.copy(preset=it.preset.copy(text=text.take(120)))})},label={Text("Data / texto")},singleLine=true)
                    effect.preset.parameters.filterKeys { it!="amount" && it!="style" }.forEach { (key,value) ->
                        Text(effectParameterLabel(key),color=Muted,fontSize=10.sp)
                        Slider(value,{v->vm.changeEffect(effect.id,{it.copy(preset=it.preset.copy(parameters=it.preset.parameters+(key to v)))},false)},onValueChangeFinished={vm.finishGesture()},modifier=Modifier.height(28.dp))
                    }
                }
            }
            if(state.edit.effects.isEmpty()) item { Text("Adicione efeitos pelo catálogo.") }
        }
    },confirmButton={TextButton(onClick={stack=false}) { Text("Pronto") }})
}

private fun effectParameterLabel(key:String)=when(key) {
    "radius"->"Raio";"hardness"->"Luz dura";"falloff"->"Bordas escuras"
    "warmth"->"Temperatura · frio / quente";"tint"->"Tonalidade · verde / violeta";"grain"->"Grão"
    "x"->"Posição horizontal";"y"->"Posição vertical";"threshold"->"Limiar";"seed"->"Variação"
    "angle"->"Posição";"width"->"Borda";"color"->"Cor";"blend"->"Mistura";"offset"->"Deslocamento";else->"Tamanho"
}
