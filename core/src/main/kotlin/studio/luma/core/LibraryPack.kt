package studio.luma.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.*
import java.util.zip.*

@Serializable data class InstalledPack(val id:String,val name:String,val itemIds:List<String>)
@Serializable data class LibrarySnapshot(val schemaVersion:Int=1,val filters:List<FilterPreset> = emptyList(),val effects:List<EffectPreset> = emptyList(),val templates:List<StoryTemplate> = emptyList(),val styles:List<StylePreset> = emptyList(),val packs:List<InstalledPack> = emptyList(),val resources:List<LocalAsset> = emptyList()) {
    fun validate() {require(schemaVersion==1);filters.forEach {it.validate()};effects.forEach {it.validate()};templates.forEach {it.validate()};styles.forEach {it.validate()};resources.forEach {it.validate()};val ids=filters.map {it.meta.id}+effects.map {it.meta.id}+templates.map {it.meta.id}+styles.map {it.meta.id}+resources.map {it.meta.id};require(ids.size<=3000 && ids.distinct().size==ids.size)}
}
@Serializable data class LibraryPack(val schemaVersion:Int=1,val meta:AssetMeta,val filters:List<FilterPreset> = emptyList(),val effects:List<EffectPreset> = emptyList(),val templates:List<StoryTemplate> = emptyList(),val styles:List<StylePreset> = emptyList(),val resources:List<LocalAsset> = emptyList()) {
    fun validate() {meta.validate();require(schemaVersion==1);val size=filters.size+effects.size+templates.size+styles.size+resources.size;require(size in 1..300);LibrarySnapshot(filters=filters,effects=effects,templates=templates,styles=styles,resources=resources).validate();require(assetNames().size<=80)}
    fun assetNames():Set<String> = templates.flatMap {it.assetNames()}.toSet()+styles.flatMap {it.assetNames()}+filters.flatMap {it.assetNames()}+effects.mapNotNull {it.asset}+resources.map {it.asset}
}
data class PackContent(val manifest:LibraryPack,val assets:Map<String,ByteArray>)

/** Own ZIP format: manifest.json and explicitly referenced image assets only. */
object PackCodec {
    const val MAX_BYTES=64*1024*1024
    private const val MAX_MANIFEST=2*1024*1024
    private const val MAX_ASSET=16*1024*1024
    private val assetPath=Regex("assets/(photo|font|lut)_[a-f0-9-]{36}")
    private class BoundedInput(input:InputStream):FilterInputStream(input) {
        private var count=0L
        private fun checkRead(n:Int):Int {if(n>0) {count+=n;require(count<=MAX_BYTES) {"Pack maior que 64 MB."}};return n}
        override fun read():Int {val b=super.read();if(b>=0) checkRead(1);return b}
        override fun read(b:ByteArray,off:Int,len:Int)=checkRead(super.read(b,off,len))
    }
    fun read(input:InputStream):PackContent {
        val entries=linkedMapOf<String,ByteArray>();var total=0L
        ZipInputStream(BoundedInput(input)).use {zip ->
            while(true) {
                val entry=zip.nextEntry ?: break;val name=entry.name
                require(!entry.isDirectory && (name=="manifest.json" || assetPath.matches(name))) {"Arquivo não permitido no pack: ${name.take(100)}"}
                require(name !in entries && entries.size<81) {"Pack com arquivos duplicados ou em excesso."}
                val limit=if(name=="manifest.json") MAX_MANIFEST else MAX_ASSET
                val out=ByteArrayOutputStream();val buffer=ByteArray(65536)
                while(true) {val n=zip.read(buffer);if(n<0) break;total+=n;require(out.size().toLong()+n<=limit && total<=MAX_BYTES) {"Conteúdo descompactado excede o limite."};out.write(buffer,0,n)}
                entries[name]=out.toByteArray();zip.closeEntry()
            }
        }
        val manifest=ProjectJson.decodeFromString<LibraryPack>(entries.remove("manifest.json")?.toString(Charsets.UTF_8) ?: error("Manifesto ausente."))
        manifest.validate();val assets=entries.mapKeys {it.key.removePrefix("assets/")}
        require(assets.keys==manifest.assetNames()) {"As imagens do pack não correspondem ao manifesto."}
        return PackContent(manifest,assets)
    }
    fun write(manifest:LibraryPack,assets:Map<String,ByteArray>,output:OutputStream) {
        manifest.validate();require(assets.keys==manifest.assetNames())
        val json=ProjectJson.encodeToString(manifest).toByteArray(Charsets.UTF_8)
        require(json.size<=MAX_MANIFEST && assets.values.all {it.size<=MAX_ASSET} && json.size+assets.values.sumOf {it.size.toLong()}<=MAX_BYTES)
        ZipOutputStream(output).use {zip ->
            fun entry(name:String,bytes:ByteArray) {zip.putNextEntry(ZipEntry(name).apply {time=0});zip.write(bytes);zip.closeEntry()}
            entry("manifest.json",json);assets.toSortedMap().forEach {(name,bytes)->entry("assets/$name",bytes)}
        }
    }
}
