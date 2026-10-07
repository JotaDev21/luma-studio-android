package studio.luma.ui

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*

internal val LocalReducedMotion=staticCompositionLocalOf {false}
internal fun accessibilityPreferences(context:Context)=context.getSharedPreferences("accessibility",Context.MODE_PRIVATE)
internal fun colorName(color:Int)=when(color) {
    0->"Transparente";-1->"Branco";0xFF151518.toInt()->"Preto";0xFFE8E3D7.toInt()->"Creme";0xFFD5FF69.toInt()->"Verde claro";0xFFB7A9F5.toInt()->"Lilás";0xFFEA8EA8.toInt()->"Rosa";0xFF668A92.toInt()->"Azul acinzentado";0xFF846854.toInt()->"Marrom";else->"Cor #${(color and 0xFFFFFF).toString(16).padStart(6,'0')}"
}

@Composable internal fun rememberReducedMotion():Boolean {
    val context=LocalContext.current
    val preferences=remember(context) {accessibilityPreferences(context)}
    var selected by remember {mutableStateOf(preferences.getBoolean("reduceMotion",false))}
    fun systemReduced()=Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f
    var system by remember {mutableStateOf(systemReduced())}
    DisposableEffect(context,preferences) {
        val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener {_,key->if(key=="reduceMotion") selected=preferences.getBoolean(key,false)}
        preferences.registerOnSharedPreferenceChangeListener(listener)
        val observer=object:ContentObserver(Handler(Looper.getMainLooper())) {override fun onChange(selfChange:Boolean) {system=systemReduced()}}
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),false,observer)
        onDispose {preferences.unregisterOnSharedPreferenceChangeListener(listener);context.contentResolver.unregisterContentObserver(observer)}
    }
    return selected || system
}

@Composable internal fun useScrollableEditor():Boolean {
    val density=LocalDensity.current
    val height=LocalWindowInfo.current.containerSize.height
    return density.fontScale>=1.3f || height>0 && height/density.density<640f
}

@Composable internal fun AccessibilityDialog(dismiss:()->Unit) {
    val context=LocalContext.current
    val preferences=remember(context) {accessibilityPreferences(context)}
    var reduce by remember {mutableStateOf(preferences.getBoolean("reduceMotion",false))}
    AlertDialog(onDismissRequest=dismiss,title={Text("Facilidade de uso")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("O Luma acompanha o tamanho de letra escolhido no Android. Com letras maiores, os editores podem ser rolados para alcançar todas as ações.",fontSize=16.sp)
            Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Text("Reduzir movimento",fontSize=16.sp,modifier=Modifier.weight(1f))
                Switch(reduce,{reduce=it;preferences.edit().putBoolean("reduceMotion",it).apply()},modifier=Modifier.testTag("reduce-motion").semantics {contentDescription="Reduzir movimento"})
            }
            Text("Trocas de filtro ficam imediatas e indicadores de espera ficam parados. A preferência de animação do Android também é respeitada.",fontSize=14.sp,color=Muted)
            Text("Leitor de tela: use os nomes dos controles, as etapas do Story e os ajustes de posição. Gestos de arrastar não são necessários para posicionar as camadas.",fontSize=16.sp)
            Text("Pincel e desenho livre continuam sendo ferramentas visuais. Para selecionar pessoas ou áreas, use Pessoa / fundo ou os controles numéricos da máscara.",fontSize=14.sp,color=Muted)
        }
    },confirmButton={TextButton(onClick=dismiss) {Text("Entendi")}})
}

@Composable internal fun LumaProgressIndicator(modifier:Modifier=Modifier,color:Color=MaterialTheme.colorScheme.primary,strokeWidth:Dp=4.dp) {
    if(LocalReducedMotion.current) Icon(Icons.Rounded.HourglassTop,"Em andamento",modifier.semantics {progressBarRangeInfo=ProgressBarRangeInfo.Indeterminate},tint=color)
    else CircularProgressIndicator(modifier,color=color,strokeWidth=strokeWidth)
}

@Composable internal fun LumaLinearProgressIndicator(modifier:Modifier=Modifier,color:Color=MaterialTheme.colorScheme.primary,trackColor:Color=MaterialTheme.colorScheme.surfaceVariant) {
    if(LocalReducedMotion.current) HorizontalDivider(modifier.semantics {contentDescription="Em andamento";progressBarRangeInfo=ProgressBarRangeInfo.Indeterminate},color=color)
    else LinearProgressIndicator(modifier,color=color,trackColor=trackColor)
}
