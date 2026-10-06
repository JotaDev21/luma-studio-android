package studio.luma.ui

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import studio.luma.*
import studio.luma.core.StoryTemplate
import kotlinx.coroutines.CancellationException

@Composable fun Templates(state:StudioUi,vm:StudioViewModel,modifier:Modifier) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Novos") }
    val templates=state.templates.filter { template ->
        (category=="Todos" || category=="Novos" && template.meta.id.startsWith("social-template-") || category=="Favoritos" && template.meta.id in state.favorites || category=="Meus templates" && state.library.templates.any {it.meta.id==template.meta.id} || category==template.meta.category) && catalogMatches(template.meta,query)
    }
    LazyColumn(modifier.fillMaxSize().testTag("template-list"),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Text("Seu próximo Story.",fontSize=30.sp,fontFamily=FontFamily.Serif) }
        item { Text("Escolha um layout, preencha as fotos e personalize cada detalhe.",color=Muted,fontSize=13.sp) }
        item { OutlinedTextField(query,{query=it.take(100)},Modifier.fillMaxWidth().testTag("template-search"),placeholder={Text("Buscar: viagem, música, photo dump…",fontSize=12.sp)},singleLine=true,leadingIcon={Icon(Icons.Rounded.Search,null)}) }
        item { CatalogCategories(listOf("Novos","Todos","Favoritos","Meus templates")+state.templates.map {it.meta.category},category,{category=it},"template-categories") }
        item { Text("${templates.size} templates · ${state.templates.size} no catálogo",color=Muted,fontSize=11.sp) }
        item { OutlinedButton(onClick={vm.createStory()},modifier=Modifier.fillMaxWidth()) { Text("Criar Story em branco") } }
        if(templates.isEmpty()) item { Text("Nenhum template encontrado.",color=Muted) }
        items(templates.chunked(2),key={it.first().meta.id}) { row ->
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                row.forEach { template -> TemplateCard(template,state,vm,Modifier.weight(1f)) }
                if(row.size==1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun TemplateCard(template:StoryTemplate,state:StudioUi,vm:StudioViewModel,modifier:Modifier) {
    val bitmap by produceState<Bitmap?>(null,template.meta.id) {
        try { value=vm.templatePreview(template) } catch(e:Exception) { if(e is CancellationException) throw e }
    }
    Column(modifier,verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(template.width.toFloat()/template.height).clip(RoundedCornerShape(14.dp)).background(Panel).testTag("template-${template.meta.id}").clickable(enabled=!state.busy) {vm.createStory(template)}) {
            bitmap?.let { Image(it.asImageBitmap(),template.meta.name,Modifier.fillMaxSize()) }
            IconButton(onClick={vm.toggleFavorite(template.meta.id)},modifier=Modifier.align(Alignment.TopEnd)) {
                Icon(if(template.meta.id in state.favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favoritar ${template.meta.name}",tint=Acid)
            }
        }
        Text(template.meta.name,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
        Text("${template.document().missingPhotos} fotos · ${catalogLabel(template.meta.category)}",fontSize=10.sp,color=Muted)
    }
}
