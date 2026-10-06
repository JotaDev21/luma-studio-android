package studio.luma.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.luma.core.AssetMeta
import java.text.Normalizer
import java.util.Locale

internal fun catalogLabel(category:String)=when(category) {
    "Couple"->"Casal";"Romantic"->"Românticos";"Travel"->"Viagem";"Gym"->"Treino"
    "Music"->"Música";"Birthday"->"Aniversário";"Magazine"->"Revista";"Dark"->"Escuros"
    "Night"->"Noite";"Memories"->"Memórias";"Aesthetic"->"Aesthetic";"Party"->"Festa"
    "Diary"->"Diário";"Social"->"Social";"Cinematic"->"Cinema";else->collectionName(category)
}
private val marks=Regex("\\p{M}+")
private fun normalized(value:String)=Normalizer.normalize(value,Normalizer.Form.NFD).replace(marks,"").lowercase(Locale.ROOT)
internal fun catalogMatches(meta:AssetMeta,query:String):Boolean {
    val haystack=normalized(meta.name+" "+meta.category+" "+catalogLabel(meta.category)+" "+meta.tags.joinToString(" "))
    return normalized(query).trim().split(Regex("\\s+")).filter {it.isNotEmpty()}.all {it in haystack}
}
@Composable internal fun CatalogCategories(categories:List<String>,selected:String,action:(String)->Unit,tag:String) {
    val focus=LocalFocusManager.current
    LazyRow(Modifier.testTag(tag),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        items(categories.distinct(),key={it}) {category->FilterChip(selected==category,{focus.clearFocus();action(category)},label={Text(catalogLabel(category),fontSize=11.sp)})}
    }
}
