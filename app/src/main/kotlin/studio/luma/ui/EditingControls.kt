package studio.luma.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.*
import androidx.compose.ui.semantics.*
import studio.luma.StudioUi
import studio.luma.StudioViewModel
import studio.luma.core.*
import kotlin.math.abs
import kotlin.math.hypot

internal data class AdjustmentSpec(val name: String, val min: Float, val max: Float, val default: Float, val read: (Adjustments) -> Float, val write: (Adjustments, Float) -> Adjustments)
internal val adjustments = listOf(
    AdjustmentSpec("Exposição", -3f, 3f, 0f, { it.exposure }, { a,v -> a.copy(exposure = v) }),
    AdjustmentSpec("Brilho", -1f, 1f, 0f, { it.brightness }, { a,v -> a.copy(brightness = v) }),
    AdjustmentSpec("Contraste", 0f, 2f, 1f, { it.contrast }, { a,v -> a.copy(contrast = v) }),
    AdjustmentSpec("Saturação", 0f, 2f, 1f, { it.saturation }, { a,v -> a.copy(saturation = v) }),
    AdjustmentSpec("Vibrance", -1f, 1f, 0f, { it.vibrance }, { a,v -> a.copy(vibrance = v) }),
    AdjustmentSpec("Temperatura", -1f, 1f, 0f, { it.temperature }, { a,v -> a.copy(temperature = v) }),
    AdjustmentSpec("Tint", -1f, 1f, 0f, { it.tint }, { a,v -> a.copy(tint = v) }),
    AdjustmentSpec("Highlights", -1f, 1f, 0f, { it.highlights }, { a,v -> a.copy(highlights = v) }),
    AdjustmentSpec("Sombras", -1f, 1f, 0f, { it.shadows }, { a,v -> a.copy(shadows = v) }),
    AdjustmentSpec("Brancos", -1f, 1f, 0f, { it.whites }, { a,v -> a.copy(whites = v) }),
    AdjustmentSpec("Pretos", -1f, 1f, 0f, { it.blacks }, { a,v -> a.copy(blacks = v) }),
    AdjustmentSpec("Gamma", .2f, 3f, 1f, { it.gamma }, { a,v -> a.copy(gamma = v) }),
    AdjustmentSpec("Fade", 0f, 1f, 0f, { it.fade }, { a,v -> a.copy(fade = v) }),
    AdjustmentSpec("Clareza", -1f, 1f, 0f, { it.clarity }, { a,v -> a.copy(clarity=v) }),
    AdjustmentSpec("Textura", -1f, 1f, 0f, { it.texture }, { a,v -> a.copy(texture=v) }),
    AdjustmentSpec("Nitidez", 0f, 2f, 0f, { it.sharpness }, { a,v -> a.copy(sharpness=v) }),
    AdjustmentSpec("Desembaçar", -1f, 1f, 0f, { it.dehaze }, { a,v -> a.copy(dehaze=v) }),
    AdjustmentSpec("Redução de ruído", 0f, 1f, 0f, { it.denoise }, { a,v -> a.copy(denoise=v) }),
    AdjustmentSpec("Vinheta", 0f, 1f, 0f, { it.vignette }, { a,v -> a.copy(vignette = v) })
)

@Composable fun AdjustmentControls(state: StudioUi, vm: StudioViewModel) {
    var selected by remember { mutableStateOf("Exposição") }
    val spec = adjustments.first { it.name == selected }
    val a = state.edit.adjustments
    Column(Modifier.fillMaxWidth().heightIn(min=153.dp).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CategoryRow(adjustments.map { it.name }, selected) { selected = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(selected, fontSize=14.sp)
            Text(java.lang.String.format(java.util.Locale.ROOT, "%.2f", spec.read(a)), color = Acid, fontSize=14.sp)
        }
        Slider(spec.read(a), { value -> vm.update(state.edit.copy(adjustments = spec.write(a, value)), false) }, onValueChangeFinished = { vm.finishGesture() }, valueRange = spec.min..spec.max, modifier = Modifier.heightIn(min=48.dp).semantics {contentDescription=selected})
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { vm.update(state.edit.copy(adjustments = spec.write(a, spec.default))) }, contentPadding = PaddingValues(0.dp)) { Text("Redefinir controle", fontSize=14.sp) }
            TextButton(onClick = { vm.update(state.edit.copy(adjustments = Adjustments())) }, contentPadding = PaddingValues(0.dp)) { Text("Limpar ajustes", fontSize=14.sp, color = Muted) }
        }
    }
}

@Composable fun CropCanvas(state: StudioUi, vm: StudioViewModel) {
    val latest by rememberUpdatedState(state)
    val source = state.source ?: return
    Image(source.asImageBitmap(), "Fonte original para recorte", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
    Canvas(Modifier.fillMaxSize().pointerInput(source) {
        var corner = -1
        var current = Crop()
        detectDragGestures(
            onDragStart = { position ->
                current = latest.edit.transform.crop
                val corners = listOf(Offset(current.left * size.width, current.top * size.height), Offset(current.right * size.width, current.top * size.height), Offset(current.left * size.width, current.bottom * size.height), Offset(current.right * size.width, current.bottom * size.height))
                val nearest = corners.indices.minBy { (corners[it] - position).getDistance() }
                corner = if ((corners[nearest] - position).getDistance() < 48.dp.toPx()) nearest else -1
            },
            onDragEnd = { vm.finishGesture() }, onDragCancel = { vm.finishGesture() },
            onDrag = { change, drag ->
                change.consume()
                val dx = drag.x / size.width; val dy = drag.y / size.height
                val c = current
                current = when (corner) {
                    0 -> c.copy(left = (c.left + dx).coerceIn(0f, c.right - .02f), top = (c.top + dy).coerceIn(0f, c.bottom - .02f))
                    1 -> c.copy(right = (c.right + dx).coerceIn(c.left + .02f, 1f), top = (c.top + dy).coerceIn(0f, c.bottom - .02f))
                    2 -> c.copy(left = (c.left + dx).coerceIn(0f, c.right - .02f), bottom = (c.bottom + dy).coerceIn(c.top + .02f, 1f))
                    3 -> c.copy(right = (c.right + dx).coerceIn(c.left + .02f, 1f), bottom = (c.bottom + dy).coerceIn(c.top + .02f, 1f))
                    else -> { val moveX = dx.coerceIn(-c.left, 1f - c.right); val moveY = dy.coerceIn(-c.top, 1f - c.bottom); Crop(c.left + moveX, c.top + moveY, c.right + moveX, c.bottom + moveY) }
                }
                vm.update(latest.edit.copy(transform = latest.edit.transform.copy(crop = current)), false)
            }
        )
    }) {
        val c = state.edit.transform.crop
        val left = c.left * size.width; val top = c.top * size.height; val right = c.right * size.width; val bottom = c.bottom * size.height
        val shade = Color.Black.copy(alpha = .55f)
        drawRect(shade, size = Size(size.width, top))
        drawRect(shade, Offset(0f, bottom), Size(size.width, (size.height - bottom).coerceAtLeast(0f)))
        drawRect(shade, Offset(0f, top), Size(left, bottom - top))
        drawRect(shade, Offset(right, top), Size((size.width - right).coerceAtLeast(0f), bottom - top))
        drawRect(Acid, Offset(left, top), Size(right - left, bottom - top), style = Stroke(2.dp.toPx()))
        for (i in 1..2) {
            drawLine(Color.White.copy(alpha = .4f), Offset(left + (right - left) * i / 3f, top), Offset(left + (right - left) * i / 3f, bottom), 1f)
            drawLine(Color.White.copy(alpha = .4f), Offset(left, top + (bottom - top) * i / 3f), Offset(right, top + (bottom - top) * i / 3f), 1f)
        }
        listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom)).forEach { drawCircle(Acid, 5.dp.toPx(), it) }
    }
}

@Composable fun CropControls(state: StudioUi, vm: StudioViewModel) {
    var smart by remember { mutableStateOf(false) }
    var edges by remember {mutableStateOf(false)}
    val transform = state.edit.transform
    val crop = transform.crop
    val source = state.source
    val sourceRatio = (source?.width ?: 1).toFloat() / (source?.height ?: 1)
    val croppedRatio = sourceRatio * (crop.right - crop.left) / (crop.bottom - crop.top)
    val outputRatio = if (transform.quarterTurns % 2 == 0) croppedRatio else 1f / croppedRatio
    val ratios = listOf("Livre" to 0f, "9:16" to 9f/16, "4:5" to .8f, "1:1" to 1f, "1.91:1" to 1.91f)
    val selected = ratios.firstOrNull { it.second > 0 && abs(it.second - outputRatio) < .015f }?.first ?: "Livre"
    Column(Modifier.fillMaxWidth().heightIn(min=153.dp).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryRow(ratios.map { it.first }, selected) { value ->
            val desired = ratios.first { it.first == value }.second
            val target = if (transform.quarterTurns % 2 == 0 || desired == 0f) desired else 1f / desired
            val w = if (target == 0f) 1f else minOf(1f, target / sourceRatio)
            val h = if (target == 0f) 1f else minOf(1f, sourceRatio / target)
            vm.update(state.edit.copy(transform = transform.copy(crop = Crop((1-w)/2, (1-h)/2, (1+w)/2, (1+h)/2))))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = { vm.update(state.edit.copy(transform = transform.copy(quarterTurns = (transform.quarterTurns + 1) % 4))) }) { Icon(Icons.AutoMirrored.Rounded.RotateRight, "Girar 90 graus") }
            IconButton(onClick = { vm.update(state.edit.copy(transform = transform.copy(flipX = !transform.flipX))) }) { Icon(Icons.Rounded.Flip, "Espelhar horizontalmente", tint = if (transform.flipX) Acid else Muted) }
            IconButton(onClick = { vm.update(state.edit.copy(transform = transform.copy(flipY = !transform.flipY))) }) { Icon(Icons.Rounded.FlipCameraAndroid, "Espelhar verticalmente", tint = if (transform.flipY) Acid else Muted) }
            IconButton(onClick = { vm.update(state.edit.copy(transform = Transform())) }) { Icon(Icons.Rounded.RestartAlt, "Redefinir transformação") }
        }
        TextButton(onClick = { smart = true }, modifier = Modifier.heightIn(min=48.dp), contentPadding = PaddingValues(0.dp)) { Text("Recorte inteligente", fontSize = 14.sp) }
        TextButton(onClick={edges=true},modifier=Modifier.heightIn(min=48.dp)) {Text("Ajustar bordas sem arrastar",fontSize=14.sp)}
    }
    if(smart) SmartCropFormatDialog(outputRatio,vm) {smart=false}
    if(edges) AlertDialog(onDismissRequest={vm.finishGesture();edges=false},title={Text("Bordas do recorte")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Ajuste cada borda. A foto original fica preservada.",fontSize=16.sp)
            fun change(next:Crop) {vm.update(vm.ui.value.edit.copy(transform=vm.ui.value.edit.transform.copy(crop=next)),false)}
            AdvancedSlider("Borda esquerda",crop.left,0f..maxOf(crop.left,crop.right-.02f),{change(crop.copy(left=it))},vm::finishGesture)
            AdvancedSlider("Borda direita",crop.right,minOf(crop.right,crop.left+.02f)..1f,{change(crop.copy(right=it))},vm::finishGesture)
            AdvancedSlider("Borda superior",crop.top,0f..maxOf(crop.top,crop.bottom-.02f),{change(crop.copy(top=it))},vm::finishGesture)
            AdvancedSlider("Borda inferior",crop.bottom,minOf(crop.bottom,crop.top+.02f)..1f,{change(crop.copy(bottom=it))},vm::finishGesture)
        }
    },confirmButton={TextButton(onClick={vm.finishGesture();edges=false}) {Text("Pronto")}})
}
