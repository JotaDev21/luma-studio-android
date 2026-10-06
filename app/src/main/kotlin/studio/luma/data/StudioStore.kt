package studio.luma.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import studio.luma.core.*
import studio.luma.renderer.ImageSource
import java.io.File
import java.util.UUID

class StudioStore(private val context: Context) {
    private val projects = File(context.filesDir, "projects").apply { mkdirs() }
    private val library = File(context.filesDir, "library").apply { mkdirs() }
    private val favoritesMutex = Mutex()
    private val libraryMutex=Mutex()
    private val assets=File(library,"assets").apply {mkdirs()}
    private fun asset(name:String):File {require(name.matches(Regex("(photo|font|lut)_[a-f0-9-]{36}")));return File(assets,name)}
    suspend fun snapshot():LibrarySnapshot=withContext(Dispatchers.IO) {
        val file=AtomicFile(File(library,"catalog.json"))
        if(!file.baseFile.exists()) LibrarySnapshot() else ProjectJson.decodeFromString<LibrarySnapshot>(file.openRead().bufferedReader().use {it.readText()}).also {it.validate()}
    }
    private fun saveSnapshot(value:LibrarySnapshot) {value.validate();atomicWrite(File(library,"catalog.json"),ProjectJson.encodeToString(value))}
    fun directory(id: String): File { require(id.matches(Regex("[a-f0-9-]{36}"))); return File(projects, id) }
    fun source(project: Project) = File(directory(project.id), "source")
    fun thumbnail(project: Project) = File(directory(project.id), "thumbnail.jpg")

    suspend fun catalog(): List<FilterPreset> = withContext(Dispatchers.IO) {
        val values = ProjectJson.decodeFromString<List<FilterPreset>>(context.assets.open("filters/presets.json").bufferedReader().use { it.readText() })
        require(values.map { it.meta.id }.distinct().size == values.size)
        values.forEach { it.validate() }; values+snapshot().filters
    }
    suspend fun effects(): List<EffectPreset> = withContext(Dispatchers.IO) {
        ProjectJson.decodeFromString<List<EffectPreset>>(context.assets.open("effects/presets.json").bufferedReader().use { it.readText() }).also { values ->
            require(values.map { it.meta.id }.distinct().size==values.size); values.forEach { it.validate() }
        }+snapshot().effects
    }
    suspend fun templates():List<StoryTemplate> = withContext(Dispatchers.IO) {
        ProjectJson.decodeFromString<List<StoryTemplate>>(context.assets.open("templates/stories.json").bufferedReader().use { it.readText() }).also { values ->
            require(values.map { it.meta.id }.distinct().size==values.size); values.forEach { it.validate() }
        }+snapshot().templates
    }
    suspend fun styles():List<StylePreset> = withContext(Dispatchers.IO) {ProjectJson.decodeFromString<List<StylePreset>>(context.assets.open("styles/presets.json").bufferedReader().use {it.readText()}).also {it.forEach {style->style.validate()}}+snapshot().styles}
    fun templateDirectory()=assets
    suspend fun createStory(template:StoryTemplate?=null):Project=withContext(Dispatchers.IO) {
        val now=System.currentTimeMillis()
        val project=Project(id=UUID.randomUUID().toString(),name=template?.meta?.name ?: "Meu Story",createdAt=now,updatedAt=now,edit=template?.editState() ?: EditState(story=StoryDocument()))
        val dir=directory(project.id).apply {mkdirs()}
        try {template?.assetNames()?.forEach {name->asset(name).copyTo(File(dir,name))};save(project); project}
        catch(e:Exception) {dir.deleteRecursively();throw e}
    }
    suspend fun copySourceAsLayer(project:Project):String=withContext(Dispatchers.IO) {val name="photo_${UUID.randomUUID()}";source(project).copyTo(File(directory(project.id),name));name}
    suspend fun saveFilter(preset:FilterPreset,directory:File?=null)=libraryMutex.withLock {withContext(Dispatchers.IO) {preset.validate();archive(preset.assetNames(),directory);val old=snapshot();saveSnapshot(old.copy(filters=old.filters+preset))}}
    suspend fun saveStyle(preset:StylePreset,directory:File?=null)=libraryMutex.withLock {withContext(Dispatchers.IO) {preset.validate();archive(preset.assetNames(),directory);val old=snapshot();saveSnapshot(old.copy(styles=old.styles+preset))}}
    suspend fun duplicateTemplate(preset:StoryTemplate)=libraryMutex.withLock {withContext(Dispatchers.IO) {preset.validate();val old=snapshot();saveSnapshot(old.copy(templates=old.templates+preset))}}
    suspend fun saveTemplate(project:Project,name:String,placeholders:Set<String>,editableText:Set<String>):StoryTemplate=libraryMutex.withLock {withContext(Dispatchers.IO) {
        val edit=project.edit;val story=edit.story ?: error("Abra um Story para salvar um template.")
        val added=mutableListOf<File>()
        try {
            val fixed=mutableMapOf<String,String>()
            val layers=story.layers.map {layer ->
                when {
                    layer.type=="photo" && layer.id in placeholders->layer.copy(asset=null,placeholder=true,photoZoom=1f,photoPanX=0f,photoPanY=0f,photoTransform=Transform(),masks=emptyList(),photoFit="cover")
                    layer.type=="photo" && layer.asset!=null->{val sourceName=requireNotNull(layer.asset);val target=fixed.getOrPut(sourceName) {val n="photo_${UUID.randomUUID()}";val f=asset(n);File(directory(project.id),sourceName).copyTo(f);added.add(f);n};layer.copy(asset=target,placeholder=false)}
                    layer.type=="text"->layer.copy(placeholder=layer.id in editableText,locked=layer.id !in editableText)
                    else->layer
                }
            }
            val template=StoryTemplate(meta=AssetMeta("my_template_${UUID.randomUUID()}",name,"Meus templates",tags=listOf("custom")),width=story.width,height=story.height,layers=layers,background=story.background,adjustments=edit.adjustments,filter=edit.filter,effects=edit.effects,collage=story.collage,color=edit.color,gradient=story.gradient,backgroundAsset=story.backgroundAsset,backgroundBlur=story.backgroundBlur)
            template.validate();archive(template.assetNames().filterNot {it in fixed.values}.toSet(),directory(project.id));val old=snapshot();saveSnapshot(old.copy(templates=old.templates+template));template
        } catch(e:Exception) {added.forEach {it.delete()};throw e}
    }}
    suspend fun importPack(uri:Uri):InstalledPack=libraryMutex.withLock {withContext(Dispatchers.IO) {
        val content=context.contentResolver.openInputStream(uri)?.use {PackCodec.read(it)} ?: error("Não foi possível abrir o pack.")
        val staging=File(context.cacheDir,"pack_${UUID.randomUUID()}").apply {mkdirs()};val added=mutableListOf<File>()
        try {
            val names=content.assets.mapValues {(name,bytes)->val file=File(staging,name);file.writeBytes(bytes);validateAsset(name,file);"${name.substringBefore('_')}_${UUID.randomUUID()}"}
            val manifest=content.manifest
            fun meta(original:AssetMeta)=original.copy(id="import_${UUID.randomUUID()}")
            val filters=manifest.filters.map {it.remapAssets(names).copy(meta=meta(it.meta))};val effects=manifest.effects.map {it.remapAssets(names).copy(meta=meta(it.meta))}
            val templates=manifest.templates.map {it.remapAssets(names).copy(meta=meta(it.meta))}
            val styles=manifest.styles.map {it.remapAssets(names).copy(meta=meta(it.meta))}
            val resources=manifest.resources.map {it.copy(meta=meta(it.meta),asset=names.getValue(it.asset))}
            val pack=InstalledPack("pack_${UUID.randomUUID()}",manifest.meta.name,filters.map {it.meta.id}+effects.map {it.meta.id}+templates.map {it.meta.id}+styles.map {it.meta.id}+resources.map {it.meta.id})
            val old=snapshot();val next=old.copy(filters=old.filters+filters,effects=old.effects+effects,templates=old.templates+templates,styles=old.styles+styles,resources=old.resources+resources,packs=old.packs+pack);next.validate()
            names.forEach {(oldName,newName)->val target=asset(newName);File(staging,oldName).copyTo(target);added.add(target)}
            saveSnapshot(next);pack
        } catch(e:Exception) {added.forEach {it.delete()};throw e} finally {staging.deleteRecursively()}
    }}
    suspend fun exportLibrary(uri:Uri)=libraryMutex.withLock {withContext(Dispatchers.IO) {
        val current=snapshot();val manifest=LibraryPack(meta=AssetMeta("luma_personal_library","Minha biblioteca Luma","Personal"),filters=current.filters,effects=current.effects,templates=current.templates,styles=current.styles,resources=current.resources)
        manifest.validate();require(manifest.assetNames().sumOf {asset(it).length()}<=PackCodec.MAX_BYTES-2*1024*1024) {"A biblioteca excede 64 MB. Use placeholders para reduzir o pack."}
        val images=manifest.assetNames().associateWith {name->val file=asset(name);require(file.length()<=16*1024*1024) {"Uma imagem fixa excede 16 MB. Use placeholder no template."};file.readBytes()}
        context.contentResolver.openOutputStream(uri)?.use {PackCodec.write(manifest,images,it)} ?: error("Não foi possível salvar o pack.")
    }}
    fun assetFile(name:String)=asset(name)
    private fun validateAsset(name:String,file:File) {
        when(name.substringBefore('_')) {
            "font"->{require(file.length() in 12..5_000_000);val magic=ByteArray(4);java.io.DataInputStream(file.inputStream()).use {it.readFully(magic)};require(magic.contentEquals(byteArrayOf(0,1,0,0)) || String(magic,Charsets.US_ASCII) in listOf("OTTO","true","ttcf")) {"Use fonte TTF ou OTF."};android.graphics.Typeface.createFromFile(file)}
            "lut"->{require(file.length()<=4_000_000);CubeParser.parse(file.readText())}
            else->{val size=ImageSource.dimensions(file);require(size.first.toLong()*size.second<=32_000_000L) {"Imagem excede 32 MP."}}
        }
    }
    private fun archive(names:Set<String>,directory:File?) {
        names.forEach {name->val target=asset(name);if(!target.exists()) {require(directory!=null);File(directory,name).copyTo(target)}}
    }
    suspend fun copyAssets(names:Set<String>,directory:File)=withContext(Dispatchers.IO) {
        names.forEach {name->val file=File(directory,name);if(!file.exists()) asset(name).copyTo(file)}
    }
    suspend fun importResource(uri:Uri,kind:String,name:String):LocalAsset=libraryMutex.withLock {withContext(Dispatchers.IO) {
        require(kind in setOf("font","lut","sticker","texture","frame"))
        val key="${if(kind in setOf("font","lut")) kind else "photo"}_${UUID.randomUUID()}";val file=asset(key)
        try {
            context.contentResolver.openInputStream(uri)?.use {input->file.outputStream().use {output->val buffer=ByteArray(65536);var total=0L;while(true) {val n=input.read(buffer);if(n<0) break;total+=n;require(total<=16_000_000) {"Recurso excede 16 MB."};output.write(buffer,0,n)}}} ?: error("Arquivo indisponível.")
            validateAsset(key,file)
            val resource=LocalAsset(AssetMeta("resource_${UUID.randomUUID()}",name.take(100).ifBlank {"Meu recurso"},"Meus recursos",tags=listOf(kind)),kind,key)
            val old=snapshot();saveSnapshot(old.copy(resources=old.resources+resource));resource
        } catch(e:Exception) {file.delete();throw e}
    }}
    suspend fun removeLibrary(ids:Set<String>)=libraryMutex.withLock {withContext(Dispatchers.IO) {
        val old=snapshot();val removed=ids+old.packs.filter {it.id in ids}.flatMap {it.itemIds}
        val next=old.copy(filters=old.filters.filterNot {it.meta.id in removed},effects=old.effects.filterNot {it.meta.id in removed},templates=old.templates.filterNot {it.meta.id in removed},styles=old.styles.filterNot {it.meta.id in removed},resources=old.resources.filterNot {it.meta.id in removed},packs=old.packs.filterNot {it.id in removed}.map {it.copy(itemIds=it.itemIds.filterNot {id->id in removed})})
        saveSnapshot(next)
        val names=LibraryPack(meta=AssetMeta("gc","GC","Personal"),filters=next.filters,effects=next.effects,templates=next.templates,styles=next.styles,resources=next.resources).assetNames()
        assets.listFiles()?.filter {it.name !in names}?.forEach {it.delete()}
    }}
    suspend fun deleteProject(id:String)=withContext(Dispatchers.IO) {
        val target=directory(id).canonicalFile;require(target.parentFile==projects.canonicalFile);check(target.deleteRecursively()) {"Não foi possível excluir o projeto."}
    }
    suspend fun addPhoto(project:Project,uri:Uri):String=withContext(Dispatchers.IO) {
        val name="photo_${UUID.randomUUID()}"; val file=File(directory(project.id),name)
        try {
            context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { output ->
                val buffer=ByteArray(65536); var total=0L
                while(true) { val n=input.read(buffer); if(n<0) break; total+=n; require(total<=150_000_000) { "Arquivo maior que 150 MB." }; output.write(buffer,0,n) }
            } } ?: error("Não foi possível abrir a foto.")
            ImageSource.dimensions(file); name
        } catch(e:Exception) { file.delete(); throw e }
    }
    suspend fun import(uri: Uri): Project = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString(); val dir = directory(id).apply { mkdirs() }
        try {
            context.contentResolver.openInputStream(uri)?.use { input -> File(dir, "source").outputStream().use { output ->
                val buffer = ByteArray(64 * 1024); var total = 0L
                while (true) { val read = input.read(buffer); if (read < 0) break; total += read
                    require(total <= 150_000_000) { "Arquivo maior que 150 MB." }; output.write(buffer, 0, read) }
            } } ?: error("Não foi possível abrir esta foto.")
            val preview = ImageSource.decode(File(dir, "source"), 1280)
            try { File(dir, "thumbnail.jpg").outputStream().use { preview.compress(Bitmap.CompressFormat.JPEG, 85, it) } } finally { preview.recycle() }
            val now = System.currentTimeMillis()
            val project = Project(id = id, name = "Foto ${java.text.SimpleDateFormat("dd MMM · HH:mm", java.util.Locale.forLanguageTag("pt-BR")).format(java.util.Date(now))}", createdAt = now, updatedAt = now)
            save(project); project
        } catch (e: Exception) { dir.deleteRecursively(); throw e }
    }
    suspend fun list(): List<Project> = withContext(Dispatchers.IO) {
        projects.listFiles()?.filter { it.isDirectory }?.mapNotNull { dir ->
            runCatching { val p = ProjectJson.decodeFromString<Project>(AtomicFile(File(dir, "project.json")).openRead().bufferedReader().use { it.readText() }); p.validate(); require(p.id == dir.name && (p.edit.story!=null || source(p).isFile)); p }.getOrNull()
        }?.sortedByDescending { it.updatedAt } ?: emptyList()
    }
    suspend fun save(project: Project, preview: Bitmap? = null): Unit = withContext(Dispatchers.IO) {
        project.validate(); val dir = directory(project.id).apply { mkdirs() }
        atomicWrite(File(dir, "project.json"), ProjectJson.encodeToString(project))
        preview?.let { File(dir, "thumbnail.jpg").outputStream().use { output ->
            val scale = minOf(1f, 480f / maxOf(it.width, it.height))
            val small = Bitmap.createScaledBitmap(it, (it.width * scale).toInt().coerceAtLeast(1), (it.height * scale).toInt().coerceAtLeast(1), true)
            try { small.compress(Bitmap.CompressFormat.JPEG, 85, output) } finally { if (small !== it) small.recycle() }
        } }
    }
    suspend fun favorites(): Set<String> = withContext(Dispatchers.IO) {
        val file = AtomicFile(File(library, "favorites.json"))
        if (!file.baseFile.exists()) emptySet() else ProjectJson.decodeFromString<List<String>>(file.openRead().bufferedReader().use { it.readText() }).toSet()
    }
    suspend fun saveFavorites(values: Set<String>) = favoritesMutex.withLock { withContext(Dispatchers.IO) { atomicWrite(File(library, "favorites.json"), ProjectJson.encodeToString(values.sorted())) } }
    private fun atomicWrite(file: File, contents: String) {
        val atomic = AtomicFile(file); val stream = atomic.startWrite()
        try { stream.write(contents.toByteArray(Charsets.UTF_8)); atomic.finishWrite(stream) } catch (e: Exception) { atomic.failWrite(stream); throw e }
    }
}
