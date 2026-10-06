package studio.luma.core

fun EffectPreset.remapAssets(names:Map<String,String>)=copy(asset=asset?.let {names[it] ?: it})
fun EffectLayer.remapAssets(names:Map<String,String>)=copy(preset=preset.remapAssets(names))
fun FilterPreset.assetNames():Set<String> = effects.mapNotNull {it.preset.asset}.toSet()
fun FilterPreset.remapAssets(names:Map<String,String>)=copy(effects=effects.map {it.remapAssets(names)})
fun LocalMask.assetNames():Set<String> = effects.mapNotNull {it.preset.asset}.toSet()+(filter?.preset?.assetNames() ?: emptySet())
fun LocalMask.remapAssets(names:Map<String,String>)=copy(filter=filter?.copy(preset=filter.preset.remapAssets(names)),effects=effects.map {it.remapAssets(names)})
fun StoryLayer.assetNames():Set<String> = listOfNotNull(asset,typography.fontAsset).toSet()+masks.flatMap {it.assetNames()}
fun StoryLayer.remapAssets(names:Map<String,String>)=copy(asset=asset?.let {names[it] ?: it},typography=typography.copy(fontAsset=typography.fontAsset?.let {names[it] ?: it}),masks=masks.map {it.remapAssets(names)})
fun EditState.assetNames():Set<String> = (story?.layers?.flatMap {it.assetNames()} ?: emptyList()).toSet()+listOfNotNull(story?.backgroundAsset)+effects.mapNotNull {it.preset.asset}+masks.flatMap {it.assetNames()}+(filter?.preset?.assetNames() ?: emptySet())
fun StoryTemplate.assetNames()=editState().assetNames()
fun StoryTemplate.remapAssets(names:Map<String,String>)=copy(layers=layers.map {it.remapAssets(names)},backgroundAsset=backgroundAsset?.let {names[it] ?: it},filter=filter?.copy(preset=filter.preset.remapAssets(names)),effects=effects.map {it.remapAssets(names)})
fun StylePreset.assetNames():Set<String> = layers.flatMap {it.assetNames()}.toSet()+orderedEffects().mapNotNull {it.preset.asset}+(filter?.preset?.assetNames() ?: emptySet())
fun StylePreset.remapAssets(names:Map<String,String>)=copy(layers=layers.map {it.remapAssets(names)},filter=filter?.copy(preset=filter.preset.remapAssets(names)),effects=effects.map {it.remapAssets(names)},effectLayers=effectLayers.map {it.remapAssets(names)})

object CompositionTools {
    fun snap(transform:LayerTransform,width:Int,height:Int,tolerance:Float=16f):LayerTransform {
        fun coordinate(value:Float,extent:Float,canvas:Float):Float {
            val candidates=listOf(0f,canvas*.12f,canvas/2-extent/2,canvas*.88f-extent,canvas-extent)
            val nearest=candidates.minBy {kotlin.math.abs(value-it)}
            return if(kotlin.math.abs(value-nearest)<=tolerance) nearest else value
        }
        return transform.copy(x=coordinate(transform.x,transform.width,width.toFloat()),y=coordinate(transform.y,transform.height,height.toFloat()))
    }
    fun align(document:StoryDocument,id:String,horizontal:String?=null,vertical:String?=null):StoryDocument = document.copy(layers=document.layers.map {l->if(l.id!=id || l.locked) l else l.copy(transform=l.transform.copy(x=when(horizontal) {"left"->0f;"center"->(document.width-l.transform.width)/2;"right"->document.width-l.transform.width;else->l.transform.x},y=when(vertical) {"top"->0f;"center"->(document.height-l.transform.height)/2;"bottom"->document.height-l.transform.height;else->l.transform.y}))})
}
