package studio.luma

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import studio.luma.core.*
import studio.luma.data.*
import studio.luma.renderer.*

enum class Page { HOME, LIBRARY, PROJECTS, TEMPLATES, STYLES, PACKS, EDITOR }
enum class DraftState { WAITING, SAVING, SAVED, FAILED }
enum class AiAvailability { UNKNOWN, CHECKING, READY, LOADING, OCCUPIED, UNAVAILABLE }
data class EnhancementProposal(val file:java.io.File,val before:Bitmap,val after:Bitmap,val width:Int,val height:Int)
data class StudioUi(
    val page: Page = Page.HOME, val filters: List<FilterPreset> = emptyList(), val favorites: Set<String> = emptySet(),
    val projects: List<Project> = emptyList(), val project: Project? = null,
    val effects: List<EffectPreset> = emptyList(),
    val styles:List<StylePreset> = emptyList(),val library:LibrarySnapshot=LibrarySnapshot(),val notice:String?=null,
    val templates: List<StoryTemplate> = emptyList(), val selectedLayer:String?=null,
    val source: Bitmap? = null, val preview: Bitmap? = null, val before: Bitmap? = null,
    val maskEditor:Boolean=false,val maskLayerId:String?=null,val maskSource:Bitmap?=null,val maskPreview:Bitmap?=null,val selectedMask:String?=null,
    val thumbnailRevision:Int=0,
    val edit: EditState = EditState(), val thumbnails: Map<String, Bitmap> = emptyMap(),
    val smartProposal:SmartProposal?=null,val recentStyles:List<String> = emptyList(),
    val batchProgress:Int=0,val batchTotal:Int=0,
    val enhancementConnected:Boolean=false,val enhancementEndpoint:String="",val enhancing:Boolean=false,val enhancementProposal:EnhancementProposal?=null,
    val pairingProposal:EnhancementPairing?=null,val aiAvailability:AiAvailability=AiAvailability.UNKNOWN,
    val draftState:DraftState=DraftState.SAVED,
    val busy: Boolean = true, val rendering: Boolean = false, val error: String? = null,
    val canUndo: Boolean = false, val canRedo: Boolean = false, val savedEdit: EditState = EditState(), val exported: Uri? = null
) { val dirty get() = project != null && edit != savedEdit }

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val store = StudioStore(application)
    private val drafts=ProjectDraftStore(application)
    private var draftJob:Job?=null
    private var draftRevision=0
    private val renderer = GpuRenderer()
    private val thumbnailRenderer=GpuRenderer()
    private val thumbnailSlots=Semaphore(2)
    private var filterJob:Job?=null
    private var filterRequest=0
    private val stories=StoryRenderer(renderer)
    private val exporter = ExportEngine(application, store, renderer,stories)
    private val enhancementClient=EnhancementClient()
    private val enhancementConnections=EnhancementConnectionStore(application)
    private var enhancementJob:Job?=null
    private var healthJob:Job?=null
    private val mutable = MutableStateFlow(StudioUi())
    val ui = mutable.asStateFlow()
    private var history = EditHistory(EditState())
    private var previewJob: Job? = null
    private var generation = 0
    private var beforeTransform: Transform? = null
    private var favoriteJob: Job? = null
    private var referencePreview:Bitmap?=null
    private var stopBatch=false
    private val focusCache=linkedMapOf<String,PhotoFocus>()
    init { viewModelScope.launch { try {
        val filters=store.catalog(); val effects=store.effects(); val templates=store.templates();val styles=store.styles();val library=store.snapshot(); val favorites=store.favorites(); val projects=store.list()
        refreshEnhancementConnection()
        mutable.update { it.copy(filters = filters, effects=effects, templates=templates,styles=styles,library=library, favorites = favorites, projects = projects, recentStyles=getApplication<Application>().getSharedPreferences("studio",0).getString("recentStyles","")!!.split(",").filter {it.isNotBlank()},busy = false) }
    } catch (e: Exception) { fail(e) } } }

    fun page(page: Page) { if (!ui.value.busy) mutable.update { it.copy(page = page) } }
    fun clearError() { mutable.update { it.copy(error = null) } }
    fun clearNotice() {mutable.update {it.copy(notice=null)}}
    private fun refreshEnhancementConnection() {
        val connection=enhancementConnections.load()
        mutable.update {it.copy(enhancementConnected=connection!=null,enhancementEndpoint=connection?.endpoint ?: "",aiAvailability=AiAvailability.UNKNOWN)}
    }
    fun reviewEnhancementQr(value:String) {
        if(ui.value.busy) return
        try {val pairing=EnhancementPairing.parse(value);mutable.update {it.copy(pairingProposal=pairing)}}
        catch(e:Exception) {mutable.update {it.copy(error=e.message ?: "Este QR não é uma conexão do Luma.")}}
    }
    fun cancelPairing() {mutable.update {it.copy(pairingProposal=null)}}
    fun confirmPairing() {
        if(ui.value.busy) return
        val proposal=ui.value.pairingProposal ?: return
        try {proposal.requireFresh();cancelPairing();connectEnhancement(proposal.endpoint,proposal.code)}
        catch(e:Exception) {cancelPairing();mutable.update {it.copy(error=e.message)}}
    }
    fun checkEnhancementPc() {
        val connection=enhancementConnections.load() ?: return
        healthJob?.cancel()
        mutable.update {it.copy(aiAvailability=AiAvailability.CHECKING)}
        healthJob=viewModelScope.launch {
            val status=try {enhancementClient.health(connection.endpoint)} catch(e:CancellationException) {throw e} catch(e:Exception) {AiAvailability.UNAVAILABLE}
            if(enhancementConnections.load()?.endpoint==connection.endpoint) mutable.update {it.copy(aiAvailability=status)}
        }
    }
    fun connectEnhancement(endpoint:String,code:String)=viewModelScope.launch {
        if(ui.value.busy) return@launch
        mutable.update {it.copy(busy=true)}
        try {
            val connection=enhancementClient.pair(endpoint,code)
            withContext(Dispatchers.IO) {enhancementConnections.save(connection)}
            refreshEnhancementConnection()
            mutable.update {it.copy(busy=false,notice="Conexão salva. Confira se a IA do PC está pronta.")}
            checkEnhancementPc()
        } catch(e:Exception) {fail(e)}
    }
    fun disconnectEnhancement() {if(!ui.value.busy) {healthJob?.cancel();enhancementConnections.clear();refreshEnhancementConnection()}}
    fun cancelEnhancement() {enhancementJob?.cancel()}
    fun discardEnhancement() {
        ui.value.enhancementProposal?.file?.delete()
        mutable.update {it.copy(enhancementProposal=null)}
    }
    fun enhancePhoto(scale:Int=2,faces:Boolean=true,strength:Float=.7f) {
        val state=ui.value
        if(state.busy || state.rendering || state.enhancementProposal!=null) return
        val project=state.project ?: return
        if(state.edit.story!=null) return
        val before=state.preview ?: return
        val connection=enhancementConnections.load() ?: run {mutable.update {it.copy(error="Conecte seu PC para melhorar com IA.")};return}
        require(scale in listOf(2,4) && strength in .1f..1f)
        finishGesture()
        mutable.update {it.copy(busy=true,enhancing=true,exported=null)}
        enhancementJob=viewModelScope.launch {
            val cache=getApplication<Application>().cacheDir
            val input=java.io.File.createTempFile("luma-ai-input-",".png",cache)
            val output=java.io.File.createTempFile("luma-ai-result-",".png",cache)
            var keepOutput=false
            try {
                previewJob?.cancelAndJoin()
                val original=withContext(Dispatchers.IO) {ImageSource.decode(store.source(project),2048)}
                val rendered=try {renderer.render(original,state.edit,2048,store.directory(project.id))} finally {original.recycle()}
                try {withContext(Dispatchers.IO) {input.outputStream().use {check(rendered.compress(Bitmap.CompressFormat.PNG,100,it))}}} finally {rendered.recycle()}
                enhancementClient.enhance(connection,input,output,scale,faces,strength)
                val size=withContext(Dispatchers.IO) {ImageSource.dimensions(output)}
                val after=withContext(Dispatchers.IO) {ImageSource.decode(output,1280)}
                ensureActive()
                mutable.update {it.copy(enhancementProposal=EnhancementProposal(output,before,after,size.first,size.second))}
                keepOutput=true
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {fail(e)}
            finally {input.delete();if(!keepOutput) output.delete();mutable.update {it.copy(busy=false,enhancing=false)};render()}
        }
    }
    fun applyEnhancement()=viewModelScope.launch {
        val state=ui.value
        if(state.busy) return@launch
        val proposal=state.enhancementProposal ?: return@launch
        val original=state.project ?: return@launch
        mutable.update {it.copy(busy=true)}
        var created:Project?=null
        try {
            draftJob?.cancelAndJoin()
            val saved=original.copy(schemaVersion=8,edit=state.edit,history=history.snapshot(),updatedAt=System.currentTimeMillis())
            store.save(saved,state.preview)
            drafts.clear(original.id)
            mutable.update {it.copy(project=saved,savedEdit=state.edit)}
            val imported=store.import(Uri.fromFile(proposal.file))
            created=imported
            val improved=imported.copy(name=(original.name+" · IA").take(100))
            store.save(improved)
            openInternal(improved)
            discardEnhancement()
            mutable.update {it.copy(projects=store.list(),notice="Cópia melhorada salva. O projeto original continua em Projetos.")}
        } catch(e:Exception) {created?.let {store.deleteProject(it.id)};fail(e)}
    }
    private fun fail(e: Exception) { if (e is CancellationException) throw e; mutable.update { it.copy(busy = false, rendering = false, error = e.message ?: "Não foi possível concluir esta operação.") } }
    fun import(uri: Uri, filter: FilterPreset? = null,style:StylePreset?=null) = viewModelScope.launch {
        if (ui.value.busy) return@launch
        mutable.update { it.copy(busy = true) }
        try { openInternal(store.import(uri), filter);if(style!=null) applyStyle(style) } catch (e: Exception) { fail(e) }
    }
    fun open(project: Project) = viewModelScope.launch {
        if (ui.value.busy) return@launch
        mutable.update { it.copy(busy = true) }
        try { openInternal(project) } catch (e: Exception) { fail(e) }
    }
    private suspend fun openInternal(project: Project, filter: FilterPreset? = null) {
        draftJob?.cancelAndJoin()
        val previous=ui.value
        if(previous.dirty && previous.project?.id!=project.id) writeDraft(previous)
        val recovered=drafts.read(project)
        val working=recovered ?: project
        filterJob?.cancel();filterRequest++;previewJob?.cancelAndJoin(); generation++; beforeTransform = null
        val source = if(working.edit.story!=null) null else withContext(Dispatchers.IO) { ImageSource.decode(store.source(project), 1280) }
        val edit = if (filter != null) working.edit.copy(filter = FilterSelection(filter,filter.savedIntensity),adjustments=filter.savedAdjustments ?: working.edit.adjustments,color=filter.savedColor ?: working.edit.color) else working.edit
        filter?.let {store.copyAssets(it.assetNames(),store.directory(project.id))}
        history = EditHistory(working.edit,restored=working.history); history.commit(edit)
        mutable.update { it.copy(page = Page.EDITOR, project = project, source = source, preview = null, before = null,
            smartProposal=null,thumbnailRevision=it.thumbnailRevision+1,edit = edit, savedEdit = project.edit, thumbnails = emptyMap(), selectedLayer=edit.story?.layers?.lastOrNull()?.id, maskEditor=false,maskLayerId=null,maskSource=null,maskPreview=null,selectedMask=null,busy = false, canUndo = history.canUndo, canRedo = history.canRedo, exported = null,draftState=DraftState.SAVED,notice=if(recovered!=null) "Suas últimas alterações foram recuperadas." else it.notice) }
        if(filter!=null) scheduleDraft()
        render()
    }
    private suspend fun writeDraft(state:StudioUi) {
        val project=state.project ?: return
        if(state.dirty) {
            val snapshot=EditHistory(history.current,restored=history.snapshot())
            // Include an unfinished slider gesture in recovery without changing live undo.
            snapshot.commit(state.edit)
            drafts.write(project,project.copy(schemaVersion=8,edit=state.edit,history=snapshot.snapshot(),updatedAt=System.currentTimeMillis()))
        } else drafts.clear(project.id)
    }
    private fun scheduleDraft(delayMs:Long=900) {
        draftJob?.cancel()
        val revision=++draftRevision
        mutable.update {it.copy(draftState=DraftState.WAITING)}
        draftJob=viewModelScope.launch {
            delay(delayMs)
            val state=ui.value
            if(state.project==null || state.page!=Page.EDITOR) return@launch
            try {
                mutable.update {it.copy(draftState=DraftState.SAVING)}
                writeDraft(state)
                ensureActive()
                if(revision==draftRevision) mutable.update {it.copy(draftState=DraftState.SAVED)}
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(revision==draftRevision) mutable.update {it.copy(draftState=DraftState.FAILED)}}
        }
    }
    fun flushDraft() {if(!ui.value.busy && ui.value.page==Page.EDITOR) scheduleDraft(0)}
    fun keepDraftAndClose()=viewModelScope.launch {
        if(ui.value.busy) return@launch
        mutable.update {it.copy(busy=true)}
        try {
            filterJob?.cancelAndJoin();finishGesture();draftJob?.cancelAndJoin()
            writeDraft(ui.value)
            closeEditorInternal()
        } catch(e:Exception) {fail(e)}
    }
    fun update(next: EditState, commit: Boolean = true) {
        if (ui.value.busy) return
        next.validate()
        val previous=ui.value
        val target=next.story?.layers?.firstOrNull {it.id==previous.maskLayerId}
        val leaveMasks=previous.maskEditor && previous.maskLayerId!=null && (target==null || target.locked || target.asset!=previous.edit.story?.layers?.firstOrNull {it.id==previous.maskLayerId}?.asset)
        val changedPreviewContext=previous.edit.copy(filter=null)!=next.copy(filter=null)
        if (commit) history.commit(next)
        mutable.update { it.copy(edit = next,thumbnailRevision=it.thumbnailRevision+if(changedPreviewContext) 1 else 0,thumbnails=if(changedPreviewContext) emptyMap() else it.thumbnails, canUndo = history.canUndo, canRedo = history.canRedo, exported = null) }
        if(leaveMasks) mutable.update {it.copy(maskEditor=false,maskLayerId=null,maskSource=null,maskPreview=null,selectedMask=null)}
        scheduleDraft()
        render()
    }
    fun finishGesture() { history.commit(ui.value.edit); mutable.update { it.copy(canUndo = history.canUndo, canRedo = history.canRedo) };if(ui.value.project!=null) scheduleDraft() }
    fun undo() { update(history.undo(), false) }
    fun redo() { update(history.redo(), false) }
    fun applyFilter(preset: FilterPreset?):Job {
        filterJob?.cancel();val request=++filterRequest;val project=ui.value.project
        val job=viewModelScope.launch(start=CoroutineStart.UNDISPATCHED) {
            if(ui.value.busy || project==null) return@launch
            try {
                val assets=preset?.assetNames() ?: emptySet()
                if(assets.isNotEmpty()) store.copyAssets(assets,store.directory(project.id))
                ensureActive()
                if(request!=filterRequest || ui.value.project?.id!=project.id || ui.value.busy) return@launch
                finishGesture()
                val current=ui.value.edit
                update(current.copy(filter=preset?.let {FilterSelection(it,it.savedIntensity)},adjustments=preset?.savedAdjustments ?: current.adjustments,color=preset?.savedColor ?: current.color))
            } catch(e:Exception) {fail(e)}
        }
        filterJob=job;return job
    }
    private suspend fun refreshLibrary() {
        val filters=store.catalog();val effects=store.effects();val templates=store.templates();val styles=store.styles();val library=store.snapshot()
        mutable.update {it.copy(filters=filters,effects=effects,templates=templates,styles=styles,library=library)}
    }
    private fun libraryAction(action:suspend ()->String)=viewModelScope.launch {
        if(ui.value.busy) return@launch;mutable.update {it.copy(busy=true)}
        try {val message=action();refreshLibrary();mutable.update {it.copy(busy=false,notice=message)}} catch(e:Exception) {fail(e)}
    }
    fun saveCurrentFilter(name:String) {
        val edit=ui.value.edit
        libraryAction {val preset=(edit.filter?.preset ?: FilterPreset(meta=AssetMeta("neutral","Neutral","Custom"))).copy(meta=AssetMeta("my_filter_${java.util.UUID.randomUUID()}",name,"Custom",tags=listOf("custom")),savedAdjustments=edit.adjustments,savedColor=edit.color,savedIntensity=edit.filter?.intensity ?: 1f);store.saveFilter(preset,ui.value.project?.let {store.directory(it.id)});"Filtro salvo na biblioteca."}
    }
    fun saveCurrentStyle(name:String,includeGraphics:Boolean) {
        val edit=ui.value.edit;val story=edit.story
        libraryAction {store.saveStyle(StylePreset(meta=AssetMeta("my_style_${java.util.UUID.randomUUID()}",name,"Meus estilos",tags=listOf("custom")),filter=edit.filter,adjustments=edit.adjustments,effectLayers=edit.effects,layers=if(includeGraphics) story?.layers?.filter {it.type!="photo"} ?: emptyList() else emptyList(),width=story?.width ?: 1080,height=story?.height ?: 1920,color=edit.color),ui.value.project?.let {store.directory(it.id)});"Estilo salvo no Style Lab."}
    }
    fun saveCurrentTemplate(name:String,placeholders:Set<String>,editableText:Set<String>) {
        val project=ui.value.project?.copy(edit=ui.value.edit) ?: return
        libraryAction {store.saveTemplate(project,name,placeholders,editableText);"Template salvo em Meus templates."}
    }
    fun importPack(uri:Uri)=libraryAction {val pack=store.importPack(uri);"${pack.name}: ${pack.itemIds.size} itens adicionados."}
    fun exportLibrary(uri:Uri)=libraryAction {store.exportLibrary(uri);"Pack salvo no local escolhido."}
    fun createCollage(settings:CollageSettings) {
        val doc=CollageEngine.create(settings)
        createStory(StoryTemplate(meta=AssetMeta("collage","Colagem","Collage"),width=doc.width,height=doc.height,layers=doc.layers,background=doc.background,collage=settings))
    }
    fun changeCollage(settings:CollageSettings,commit:Boolean=true) {
        try {changeStory(commit) {CollageEngine.reflow(it,settings)}} catch(e:Exception) {fail(e)}
    }
    private suspend fun photoAsStory(project:Project,edit:EditState):EditState {
        val dimensions=withContext(Dispatchers.IO) {ImageSource.dimensions(store.source(project))};val crop=edit.transform.crop
        var width=dimensions.first*(crop.right-crop.left);var height=dimensions.second*(crop.bottom-crop.top)
        if(edit.transform.quarterTurns%2!=0) {val old=width;width=height;height=old}
        val scale=1920f/maxOf(width,height);val w=(width*scale).toInt().coerceIn(64,8192);val h=(height*scale).toInt().coerceIn(64,8192)
        val asset=store.copySourceAsLayer(project);val id=java.util.UUID.randomUUID().toString()
        return edit.copy(transform=Transform(),masks=emptyList(),story=StoryDocument(w,h,layers=listOf(StoryLayer(id,"photo",LayerTransform(0f,0f,w.toFloat(),h.toFloat()),asset=asset,photoTransform=edit.transform,masks=edit.masks))))
    }
    fun convertPhotoToStory()=viewModelScope.launch {
        if(ui.value.busy || ui.value.edit.story!=null) return@launch;val project=ui.value.project ?: return@launch
        mutable.update {it.copy(busy=true)}
        try {val edit=photoAsStory(project,ui.value.edit);mutable.update {it.copy(busy=false,selectedLayer=edit.story?.layers?.firstOrNull()?.id)};update(edit)} catch(e:Exception) {fail(e)}
    }
    fun applyStyle(style:StylePreset)=viewModelScope.launch {
        if(ui.value.busy) return@launch;val project=ui.value.project ?: return@launch
        mutable.update {it.copy(busy=true)}
        try {
            style.validate();var edit=ui.value.edit
            if(style.layers.isNotEmpty() && edit.story==null) edit=photoAsStory(project,edit)
            val doc=edit.story
            if(doc!=null) {
                val retained=doc.layers.filterNot {it.styleElement}
                require(retained.size+style.layers.size<=80) {"O estilo ultrapassa o limite de 80 camadas."}
                val added=style.layers.map {layer->val t=layer.transform;val scale=minOf(doc.width.toFloat()/style.width,doc.height.toFloat()/style.height);layer.copy(id=java.util.UUID.randomUUID().toString(),styleElement=true,typography=layer.typography.copy(size=(layer.typography.size*scale).coerceIn(8f,400f)),transform=t.copy(x=(t.x*doc.width/style.width).coerceIn(-16384f,16384f),y=(t.y*doc.height/style.height).coerceIn(-16384f,16384f),width=(t.width*doc.width/style.width).coerceIn(8f,16384f),height=(t.height*doc.height/style.height).coerceIn(8f,16384f)))}
                edit=edit.copy(story=doc.copy(layers=retained+added))
            }
            store.copyAssets(style.assetNames(),store.directory(project.id))
            val next=edit.copy(color=style.color,adjustments=style.adjustments,filter=style.filter,effects=style.orderedEffects().map {it.copy(id=java.util.UUID.randomUUID().toString())})
            val recent=(listOf(style.meta.id)+ui.value.recentStyles).distinct().take(12)
            getApplication<Application>().getSharedPreferences("studio",0).edit().putString("recentStyles",recent.joinToString(",")).apply()
            mutable.update {it.copy(busy=false,recentStyles=recent)};update(next)
        } catch(e:Exception) {fail(e)}
    }
    fun createStory(template:StoryTemplate?=null)=viewModelScope.launch {
        if(ui.value.busy) return@launch
        mutable.update { it.copy(busy=true) }
        try { openInternal(store.createStory(template)) } catch(e:Exception) { fail(e) }
    }
    fun selectLayer(id:String?) { mutable.update { it.copy(selectedLayer=id) } }
    fun changeStory(commit:Boolean=true,change:(StoryDocument)->StoryDocument) {
        ui.value.edit.story?.let { update(ui.value.edit.copy(story=change(it)),commit) }
    }
    fun changeLayer(id:String,change:(StoryLayer)->StoryLayer,commit:Boolean=true) {
        changeStory(commit) { story->story.copy(layers=story.layers.map { if(it.id==id) change(it) else it }) }
    }
    fun addLayer(type:String,content:String="",shape:String="rect") {
        val story=ui.value.edit.story ?: return
        if(story.layers.size>=80) { mutable.update { it.copy(error="Limite de 80 camadas por Story.") }; return }
        val id=java.util.UUID.randomUUID().toString()
        val w=story.width*(if(type=="sticker") .28f else .7f); val h=if(type=="sticker") w else story.height*.15f
        val layer=StoryLayer(id,type,LayerTransform(story.width*.15f,story.height*.35f,w,h),content=content,shape=shape,typography=Typography(size=if(type=="sticker") 200f else 72f))
        changeStory { it.copy(layers=it.layers+layer) }; selectLayer(id)
    }
    fun deleteLayer(id:String) { changeStory { it.copy(layers=it.layers.filterNot { l->l.id==id }) }; selectLayer(null) }
    fun duplicateLayer(id:String) {
        val story=ui.value.edit.story ?: return; val layer=story.layers.firstOrNull { it.id==id } ?: return
        if(story.layers.size>=80) return
        val copy=layer.copy(id=java.util.UUID.randomUUID().toString(),locked=false,transform=layer.transform.copy(x=(layer.transform.x+30).coerceIn(-16384f,16384f),y=(layer.transform.y+30).coerceIn(-16384f,16384f)))
        changeStory { it.copy(layers=it.layers+copy) }; selectLayer(copy.id)
    }
    fun moveLayer(id:String,delta:Int) {
        val layers=ui.value.edit.story?.layers?.toMutableList() ?: return
        val from=layers.indexOfFirst { it.id==id }; val to=from+delta
        if(from>=0 && to in layers.indices) { java.util.Collections.swap(layers,from,to); changeStory { it.copy(layers=layers) } }
    }
    fun addStoryPhotos(uris:List<Uri>,replaceId:String?=null)=viewModelScope.launch {
        if(ui.value.busy || uris.isEmpty()) return@launch
        val project=ui.value.project ?: return@launch; var story=ui.value.edit.story ?: return@launch
        val replacements=story.layers.count {it.type=="photo" && (it.id==replaceId || replaceId==null && it.asset==null)}
        if(story.collage!=null && story.layers.count {it.type=="photo"}+uris.size-replacements>20) {mutable.update {it.copy(error="Limite de 20 fotos por colagem.")};return@launch}
        if(uris.size>replacements+80-story.layers.size) { mutable.update {it.copy(error="Essas fotos ultrapassam o limite de 80 camadas. Escolha menos fotos.")};return@launch }
        mutable.update { it.copy(busy=true) }
        try {
            val assets=uris.map { store.addPhoto(project,it) }.toMutableList()
            var selected=replaceId
            val layers=story.layers.map { layer ->
                if(layer.type=="photo" && assets.isNotEmpty() && (layer.id==replaceId || replaceId==null && layer.asset==null)) {
                    selected=layer.id; layer.copy(asset=assets.removeAt(0),placeholder=false,masks=emptyList(),photoFit="cover")
                } else layer
            }.toMutableList()
            for(asset in assets) {
                if(layers.size>=80) break
                val id=java.util.UUID.randomUUID().toString(); selected=id
                layers.add(StoryLayer(id,"photo",LayerTransform(story.width*.1f,story.height*.15f,story.width*.8f,story.height*.6f),asset=asset))
            }
            story=story.copy(layers=layers)
            mutable.update { it.copy(busy=false,selectedLayer=selected) }
            update(ui.value.edit.copy(story=story))
        } catch(e:Exception) { fail(e) }
    }
    private suspend fun analyzeFocus(project:Project,asset:String?,masks:List<LocalMask>):PhotoFocus {
        val file=if(asset==null) store.source(project) else java.io.File(store.directory(project.id),asset)
        val key=file.absolutePath
        focusCache[key]?.let {return it}
        val bitmap=withContext(Dispatchers.IO) {ImageSource.decode(file,1280)}
        val result=try {FocusAnalyzer().analyze(bitmap,masks.firstOrNull {it.type=="person"}?.raster)} finally {bitmap.recycle()}
        focusCache[key]=result
        while(focusCache.size>32) focusCache.remove(focusCache.keys.first())
        return result
    }
    fun proposeSmartCrop(outputAspect:Float?=null,layerId:String?=null)=smartAction {state,project->
        val doc=state.edit.story
        val layer=doc?.layers?.firstOrNull {it.id==layerId}
        require(doc==null || (layer!=null && layer.type=="photo" && layer.visible && !layer.locked && layer.asset!=null)) {"Escolha uma foto visível e desbloqueada."}
        val focus=analyzeFocus(project,layer?.asset,layer?.masks ?: state.edit.masks)
        val next:EditState
        val whole:Boolean
        if(layer!=null) {
            val framed=SmartFraming.frame(layer,focus);whole=framed.photoFit=="contain"
            next=state.edit.copy(story=doc!!.copy(layers=doc.layers.map {if(it.id==layer.id) framed else it}))
        } else {
            val t=state.edit.transform;val c=t.crop;val raw=focus.width.toFloat()/focus.height*(c.right-c.left)/(c.bottom-c.top)
            val aspect=outputAspect ?: if(t.quarterTurns%2==0) raw else 1f/raw
            val crop=SmartFraming.crop(focus,aspect,t);whole=crop==null
            next=state.edit.copy(transform=t.copy(crop=crop ?: Crop()))
        }
        val explanation=if(whole) {
            if(focus.faces.isEmpty()) "Este formato é estreito demais para a foto. A sugestão mantém a imagem inteira." else if(layer!=null) "Nem todos os rostos detectados cabem nesta moldura. A foto inteira aparece com margens." else "Nem todos os rostos detectados cabem neste formato. A sugestão mantém a foto inteira; você pode escolher outro formato."
        } else when {focus.faces.isNotEmpty()->"Enquadramento com espaço para ${countLabel(focus.faces.size,"rosto detectado","rostos detectados")}.";focus.person->"Enquadramento orientado pela pessoa detectada.";else->"Nenhum rosto ou pessoa detectado: enquadramento central."}
        Triple("Recorte inteligente",explanation,next)
    }
    fun proposeSmartLayout()=smartAction {state,project->
        val doc=requireNotNull(state.edit.story)
        val photos=doc.layers.filter {it.type=="photo" && !it.locked && it.visible}
        require(photos.size in 2..20 && photos.any {it.asset!=null}) {"Adicione fotos a 2–20 molduras visíveis e desbloqueadas."}
        val analysis=linkedMapOf<String,PhotoFocus>()
        for(photo in photos) photo.asset?.let {asset->analysis.getOrPut(asset) {analyzeFocus(project,asset,photo.masks)}}
        val next=SmartFraming.organize(doc,analysis)
        val changed=photos.count {old->next.layers.first {it.id==old.id}.asset!=old.asset}
        val margins=next.layers.count {it.id in photos.map {photo->photo.id} && it.asset!=null && it.photoFit=="contain"}
        val faces=analysis.values.sumOf {it.faces.size}
        val aspects=photos.filter {it.asset!=null}.map {analysis.getValue(it.asset!!).aspect(it.photoTransform)}
        val orientations="Fotos: ${countLabel(aspects.count {it<.9f},"vertical","verticais")}, ${countLabel(aspects.count {it>1.1f},"horizontal","horizontais")} e ${countLabel(aspects.count {it in .9f..1.1f},"quadrada","quadradas")}."
        val detected=if(faces==0) "Nenhum rosto detectado; confira os recortes." else "${countLabel(faces,"rosto detectado","rostos detectados")}."
        Triple("Organização inteligente","$orientations ${countLabel(changed,"moldura atualizada","molduras atualizadas")}. $detected"+(if(margins>0) " ${countLabel(margins,"foto com margens","fotos com margens")} para preservar os rostos." else ""),state.edit.copy(story=next))
    }
    private fun countLabel(count:Int,singular:String,plural:String)="$count ${if(count==1) singular else plural}"

    private fun smartAction(action:suspend (StudioUi,Project)->Triple<String,String,EditState>)=viewModelScope.launch {
        val state=ui.value;val project=state.project ?: return@launch
        if(state.busy || state.smartProposal!=null || state.maskEditor) return@launch
        mutable.update {it.copy(busy=true)}
        try {
            previewJob?.cancelAndJoin();generation++
            val (title,explanation,next)=action(state,project);next.validate()
            suspend fun draw(edit:EditState)=edit.story?.let {stories.render(it,store.directory(project.id),edit)} ?: renderer.render(requireNotNull(state.source),edit,directory=store.directory(project.id))
            val before=state.preview?.takeUnless {state.rendering} ?: draw(state.edit)
            val preview=draw(next)
            mutable.update {it.copy(busy=false,rendering=false,smartProposal=SmartProposal(title,explanation,state.edit,next,before,preview,if(title=="Estética de referência") referencePreview else null))}
        } catch(e:Exception) {fail(e);render()}
    }
    fun discardSmartProposal() {
        if(ui.value.busy || ui.value.smartProposal==null) return
        mutable.update {it.copy(smartProposal=null)};render()
    }
    fun applySmartProposal() {
        if(ui.value.busy) return
        val proposal=ui.value.smartProposal ?: return
        mutable.update {it.copy(smartProposal=null)}
        if(ui.value.edit!=proposal.base) {mutable.update {it.copy(error="A edição mudou. Gere uma nova sugestão.")};render();return}
        history.commit(proposal.base)
        update(proposal.edit)
    }
    fun maskPhotoEdit():EditState {
        val state=ui.value;val layer=state.edit.story?.layers?.firstOrNull {it.id==state.maskLayerId}
        return state.edit.copy(story=null,transform=layer?.photoTransform ?: state.edit.transform,masks=layer?.masks ?: state.edit.masks)
    }
    fun openMasks(layerId:String?=null)=viewModelScope.launch {
        if(ui.value.busy || ui.value.maskEditor) return@launch
        val state=ui.value;val project=state.project ?: return@launch
        val layer=state.edit.story?.layers?.firstOrNull {it.id==layerId}
        if(state.edit.story!=null && (layer==null || layer.type!="photo" || layer.asset==null || layer.locked)) return@launch
        mutable.update {it.copy(busy=true)}
        try {
            previewJob?.cancelAndJoin();generation++
            val source=if(layer!=null) withContext(Dispatchers.IO) {ImageSource.decode(java.io.File(store.directory(project.id),requireNotNull(layer.asset)),1280)} else requireNotNull(state.source) {"Reabra a foto para editar suas máscaras."}
            mutable.update {it.copy(busy=false,maskEditor=true,maskLayerId=layerId,maskSource=source,maskPreview=null,selectedMask=(layer?.masks ?: it.edit.masks).lastOrNull()?.id)}
            render()
        } catch(e:Exception) {fail(e)}
    }
    fun closeMasks() {
        if(ui.value.busy) return
        mutable.update {it.copy(maskEditor=false,maskLayerId=null,maskSource=null,maskPreview=null,selectedMask=null)};render()
    }
    fun selectMask(id:String) {mutable.update {it.copy(selectedMask=id)}}
    fun automaticMask(background:Boolean=false,cutout:Boolean=false)=viewModelScope.launch {
        val state=ui.value
        if(state.busy || !state.maskEditor) return@launch
        val masks=maskPhotoEdit().masks
        if(masks.size>=8) {mutable.update {it.copy(error="Limite de 8 máscaras por foto.")};return@launch}
        val source=state.maskSource ?: return@launch
        mutable.update {it.copy(busy=true)}
        try {
            val raster=masks.firstOrNull {it.type=="person"}?.raster ?: PeopleSegmenter().detect(source)
            val mask=LocalMask(java.util.UUID.randomUUID().toString(),(if(cutout) "Pessoa recortada" else if(background) "Fundo" else "Pessoa")+" ${masks.size+1}",type="person",raster=raster,inverted=background,cutout=cutout)
            mutable.update {it.copy(busy=false)};setMasks(masks+mask);selectMask(mask.id)
        } catch(e:Exception) {fail(e)}
    }
    private fun setMasks(masks:List<LocalMask>,commit:Boolean=true) {
        val id=ui.value.maskLayerId
        if(id!=null) changeLayer(id,{it.copy(masks=masks)},commit) else update(ui.value.edit.copy(masks=masks),commit)
    }
    fun addMask(type:String) {
        val masks=maskPhotoEdit().masks
        if(masks.size>=8) {mutable.update {it.copy(error="Limite de 8 máscaras por foto.")};return}
        val mask=LocalMask(java.util.UUID.randomUUID().toString(),when(type) {"radial"->"Circular";"linear"->"Degradê";else->"Pincel"}+" ${masks.size+1}",type)
        setMasks(masks+mask);selectMask(mask.id)
    }
    fun changeMask(id:String,commit:Boolean=true,change:(LocalMask)->LocalMask) {setMasks(maskPhotoEdit().masks.map {if(it.id==id) change(it) else it},commit)}
    fun deleteMask(id:String) {val masks=maskPhotoEdit().masks.filterNot {it.id==id};setMasks(masks);mutable.update {it.copy(selectedMask=masks.lastOrNull()?.id)}}
    fun moveMask(id:String,delta:Int) {
        val masks=maskPhotoEdit().masks.toMutableList();val from=masks.indexOfFirst {it.id==id};val to=from+delta
        if(from>=0 && to in masks.indices) {java.util.Collections.swap(masks,from,to);setMasks(masks)}
    }
    fun brushStroke(id:String,stroke:BrushStroke) {
        val mask=maskPhotoEdit().masks.firstOrNull {it.id==id} ?: return
        if(mask.strokes.size>=100 || mask.strokes.sumOf {it.points.size}+stroke.points.size>10000) {mutable.update {it.copy(error="Esta máscara chegou ao limite de traços. Crie outro pincel para continuar.")};return}
        val actual=if(mask.type=="person" && mask.inverted) stroke.copy(erase=!stroke.erase) else stroke
        changeMask(id) {it.copy(strokes=it.strokes+actual)}
    }
    fun addEffect(preset: EffectPreset) = viewModelScope.launch {
        try {
        ui.value.project?.let {store.copyAssets(setOfNotNull(preset.asset),store.directory(it.id))}
        val effects=ui.value.edit.effects
        if(effects.size>=16) { mutable.update { it.copy(error="Você pode combinar até 16 efeitos.") }; return@launch }
        update(ui.value.edit.copy(effects=effects+EffectLayer(java.util.UUID.randomUUID().toString(),if(preset.type=="timestamp" && preset.text.isBlank()) preset.copy(text=java.text.SimpleDateFormat("dd MM yyyy · HH:mm",java.util.Locale.ROOT).format(java.util.Date())) else preset)))
        } catch(e:Exception) {fail(e)}
    }
    fun changeEffect(id:String, change:(EffectLayer)->EffectLayer, commit:Boolean=true) {
        update(ui.value.edit.copy(effects=ui.value.edit.effects.map { if(it.id==id) change(it) else it }),commit)
    }
    fun removeEffect(id:String) { update(ui.value.edit.copy(effects=ui.value.edit.effects.filterNot { it.id==id })) }
    fun moveEffect(id:String, delta:Int) {
        val effects=ui.value.edit.effects.toMutableList(); val from=effects.indexOfFirst { it.id==id }; val to=from+delta
        if(from>=0 && to in effects.indices) { java.util.Collections.swap(effects,from,to); update(ui.value.edit.copy(effects=effects)) }
    }
    fun toggleFavorite(id: String) {
        val values = ui.value.favorites.let { if (id in it) it - id else it + id }
        mutable.update { it.copy(favorites = values) }
        favoriteJob?.cancel(); favoriteJob = viewModelScope.launch { try { store.saveFavorites(values) } catch (e: Exception) { fail(e) } }
    }
    private fun render() {
        previewJob?.cancel()
        val token = ++generation
        previewJob = viewModelScope.launch {
            mutable.update { it.copy(rendering = true) }
            delay(35)
            try {
                val state = ui.value.edit; val source = ui.value.source; val project=ui.value.project ?: return@launch
                if(ui.value.maskEditor) {
                    val output=renderer.render(ui.value.maskSource ?: return@launch,maskPhotoEdit(),directory=store.directory(project.id))
                    if(token!=generation) {output.recycle();return@launch}
                    mutable.update {it.copy(maskPreview=output,rendering=false)};return@launch
                }
                val story=state.story
                val output = if(story!=null) stories.render(story,store.directory(project.id),state) else renderer.render(source ?: return@launch, state,directory=store.directory(project.id))
                if (token != generation) { output.recycle(); return@launch }
                val before = if(state.story!=null) null else if (ui.value.before == null || state.transform != beforeTransform) renderer.render(source!!, EditState(transform = state.transform)) else ui.value.before
                if(token!=generation) {output.recycle();if(before!==ui.value.before) before?.recycle();return@launch}
                beforeTransform = state.transform
                mutable.update { it.copy(preview = output, before = before, rendering = false) }
            } catch (e: Exception) { fail(e) }
        }
    }
    /** Separate EGL queue: visible cards cannot delay the main photo's renderer. */
    suspend fun thumbnail(preset:FilterPreset):Bitmap? {
        val context=ui.value;context.thumbnails[preset.meta.id]?.let {return it}
        val source=context.source ?: return null;val project=context.project ?: return null
        val revision=context.thumbnailRevision
        return try {thumbnailSlots.withPermit {
            delay(70);currentCoroutineContext().ensureActive()
            if(ui.value.source!==source || ui.value.thumbnailRevision!=revision) return@withPermit null
            val edit=context.edit.copy(filter=FilterSelection(preset,preset.savedIntensity),adjustments=preset.savedAdjustments ?: context.edit.adjustments,color=preset.savedColor ?: context.edit.color)
            val small=withContext(Dispatchers.Default) {Bitmap.createScaledBitmap(source,(source.width*(224f/maxOf(source.width,source.height))).toInt().coerceAtLeast(1),(source.height*(224f/maxOf(source.width,source.height))).toInt().coerceAtLeast(1),true)}
            val bitmap=try {
                if(preset.assetNames().isNotEmpty()) store.copyAssets(preset.assetNames(),store.directory(project.id))
                thumbnailRenderer.render(small,edit,maxEdge=224,directory=store.directory(project.id))
            } finally {if(small!==source) small.recycle()}
            if(ui.value.source!==source || ui.value.thumbnailRevision!=revision) {bitmap.recycle();null} else {
                mutable.update {it.copy(thumbnails=(it.thumbnails+(preset.meta.id to bitmap)).entries.toList().takeLast(60).associate {entry->entry.toPair()})};bitmap
            }
        }} catch(e:Exception) {if(e is CancellationException) throw e;null}
    }
    fun save(name: String = ui.value.project?.name ?: "Sem título") = viewModelScope.launch {
        if (ui.value.busy) return@launch
        val project = ui.value.project ?: return@launch
        val edit = ui.value.edit
        mutable.update { it.copy(busy = true) }
        try {
            draftJob?.cancelAndJoin()
            history.commit(edit)
            previewJob?.join()
            val updated = project.copy(schemaVersion=8,history=history.snapshot(),name = name.trim().take(100).ifEmpty { "Sem título" }, updatedAt = System.currentTimeMillis(), edit = edit)
            store.save(updated, ui.value.preview)
            drafts.clear(project.id)
            mutable.update { it.copy(project = updated, savedEdit = edit, projects = store.list(), busy = false,draftState=DraftState.SAVED) }
        } catch (e: Exception) { fail(e) }
    }
    fun export(options: ExportOptions) = viewModelScope.launch {
        if (ui.value.busy) return@launch
        val project = ui.value.project?.copy(edit = ui.value.edit) ?: return@launch
        mutable.update { it.copy(busy = true, exported = null) }
        try { val result = exporter.export(project, options); mutable.update { it.copy(busy = false, exported = result) } } catch (e: Exception) { fail(e) }
    }
    fun closeEditor(discard: Boolean = false) = viewModelScope.launch {
        if (ui.value.busy || ui.value.dirty && !discard) return@launch
        mutable.update {it.copy(busy=true)}
        try {
            draftJob?.cancelAndJoin()
            if(discard) ui.value.project?.let {drafts.clear(it.id)}
            closeEditorInternal()
        } catch(e:Exception) {fail(e);render()}
    }
    private suspend fun closeEditorInternal() {
        discardEnhancement()
        filterJob?.cancelAndJoin();filterRequest++;previewJob?.cancelAndJoin(); generation++
        val projects=store.list()
        mutable.update {it.copy(busy=false,rendering=false,smartProposal=null,page=Page.HOME,project=null,source=null,preview=null,before=null,thumbnails=emptyMap(),projects=projects,draftState=DraftState.SAVED)}
    }
    fun importResource(uri:Uri,kind:String,name:String)=libraryAction {store.importResource(uri,kind,name);"Recurso importado."}
    fun duplicateLibrary(id:String)=libraryAction {
        val library=ui.value.library
        fun meta(value:AssetMeta)=value.copy(id="copy_${java.util.UUID.randomUUID()}",name=(value.name+" cópia").take(100))
        library.filters.firstOrNull {it.meta.id==id}?.let {store.saveFilter(it.copy(meta=meta(it.meta)))}
        library.styles.firstOrNull {it.meta.id==id}?.let {store.saveStyle(it.copy(meta=meta(it.meta)))}
        library.templates.firstOrNull {it.meta.id==id}?.let {store.duplicateTemplate(it.copy(meta=meta(it.meta)))}
        "Cópia adicionada à biblioteca."
    }
    fun removeLibrary(id:String)=libraryAction {store.removeLibrary(setOf(id));"Item removido da biblioteca."}
    fun deleteProject(id:String)=viewModelScope.launch {try {if(ui.value.project?.id==id) draftJob?.cancelAndJoin();store.deleteProject(id);mutable.update {it.copy(projects=store.list())}} catch(e:Exception) {fail(e)}}
    fun resourceFile(name:String)=store.assetFile(name)
    fun prepareResource(resource:LocalAsset)=viewModelScope.launch {try {ui.value.project?.let {store.copyAssets(setOf(resource.asset),store.directory(it.id))}} catch(e:Exception) {fail(e)}}
    fun useResource(resource:LocalAsset)=viewModelScope.launch {
        val project=ui.value.project ?: return@launch
        try {
            store.copyAssets(setOf(resource.asset),store.directory(project.id))
            when(resource.kind) {
                "lut"->{val lut=withContext(Dispatchers.IO) {CubeParser.parse(store.assetFile(resource.asset).readText())};update(ui.value.edit.copy(color=ui.value.edit.color.copy(lut=lut)))}
                "font"->{val id=ui.value.selectedLayer ?: error("Selecione um texto para aplicar a fonte.");require(ui.value.edit.story?.layers?.any {it.id==id && it.type in setOf("text","sticker") && !it.locked}==true) {"Selecione um texto desbloqueado."};changeLayer(id,{it.copy(typography=it.typography.copy(fontAsset=resource.asset))})}
                "texture","frame"->addEffect(EffectPreset(meta=resource.meta,type="texture",parameters=mapOf("amount" to .5f,"blend" to if(resource.kind=="frame") 1f else .5f),asset=resource.asset))
                "sticker"->{if(ui.value.edit.story==null) update(photoAsStory(project,ui.value.edit));addLayer("photo");ui.value.selectedLayer?.let {id->changeLayer(id,{it.copy(asset=resource.asset)})}}
            }
        } catch(e:Exception) {fail(e)}
    }
    fun snapLayer(id:String) {val doc=ui.value.edit.story ?: return;changeLayer(id,{it.copy(transform=CompositionTools.snap(it.transform,doc.width,doc.height))},false);finishGesture()}
    fun alignLayer(id:String,horizontal:String?=null,vertical:String?=null) {changeStory {CompositionTools.align(it,id,horizontal,vertical)}}
    fun surprise() {ui.value.styles.filter {it.meta.id.startsWith("luma-style-")}.randomOrNull()?.let {applyStyle(it)}}
    fun drawing(stroke:DrawingStroke) {
        val doc=ui.value.edit.story ?: return
        val existing=doc.layers.firstOrNull {it.id==ui.value.selectedLayer && it.type=="drawing" && !it.locked}
        if(existing==null) {
            if(doc.layers.size>=80) return
            val layer=StoryLayer(java.util.UUID.randomUUID().toString(),"drawing",LayerTransform(0f,0f,doc.width.toFloat(),doc.height.toFloat()),strokes=listOf(stroke))
            changeStory {it.copy(layers=it.layers+layer)};selectLayer(layer.id);return
        }
        if(existing.strokes.size>=200 || existing.strokes.sumOf {it.points.size}+stroke.points.size>20000) {mutable.update {it.copy(error="Limite de traços. Crie outra camada de desenho.")};return}
        val t=existing.transform;val angle=-t.rotation*Math.PI/180
        val local=stroke.copy(points=stroke.points.map {point->val x=point.x*doc.width-t.x-t.width/2;val y=point.y*doc.height-t.y-t.height/2;Point(((x*kotlin.math.cos(angle)-y*kotlin.math.sin(angle)+t.width/2)/t.width).toFloat().coerceIn(0f,1f),((x*kotlin.math.sin(angle)+y*kotlin.math.cos(angle)+t.height/2)/t.height).toFloat().coerceIn(0f,1f))},radius=(stroke.radius*minOf(doc.width,doc.height)/minOf(t.width,t.height)).coerceIn(.001f,.2f))
        changeLayer(existing.id,{it.copy(strokes=it.strokes+local)})
    }
    fun music(title:String,artist:String,lyrics:String,progress:Float) {
        val doc=ui.value.edit.story ?: return;if(doc.layers.size+6>80) return
        fun text(value:String,y:Float,size:Float)=StoryLayer(java.util.UUID.randomUUID().toString(),"text",LayerTransform(doc.width*.1f,doc.height*y,doc.width*.8f,doc.height*.1f),content=value,typography=Typography(size=size,alignment="left"))
        val cover=StoryLayer(java.util.UUID.randomUUID().toString(),"photo",LayerTransform(doc.width*.1f,doc.height*.18f,doc.width*.8f,doc.height*.38f),placeholder=true)
        val track=StoryLayer(java.util.UUID.randomUUID().toString(),"shape",LayerTransform(doc.width*.1f,doc.height*.78f,doc.width*.8f,8f),color=0xFF666666.toInt())
        val played=track.copy(id=java.util.UUID.randomUUID().toString(),color=-1,transform=track.transform.copy(width=(track.transform.width*progress).coerceAtLeast(8f)))
        changeStory {it.copy(layers=it.layers+listOf(cover,text(title,.59f,64f),text(artist,.65f,40f),text(lyrics,.83f,36f),track,played))};selectLayer(cover.id)
    }
    fun cancelBatch() {stopBatch=true}
    fun batch(uris:List<Uri>,style:StylePreset?,options:ExportOptions)=viewModelScope.launch {
        if(ui.value.busy || uris.isEmpty()) return@launch
        stopBatch=false;val sources=uris.take(20);mutable.update {it.copy(busy=true,batchProgress=0,batchTotal=sources.size)}
        var done=0
        try {
            style?.validate();require(style==null || style.layers.isEmpty()) {"No lote, escolha um estilo apenas de cor e efeitos."}
            for(uri in sources) {
                if(stopBatch) break
                val p=store.import(uri);var edit=p.edit
                if(style!=null) {store.copyAssets(style.assetNames(),store.directory(p.id));edit=edit.copy(filter=style.filter,adjustments=style.adjustments,color=style.color,effects=style.orderedEffects())}
                val h=EditHistory(p.edit);h.commit(edit);val next=p.copy(edit=edit,history=h.snapshot())
                val output=exporter.export(next,options)
                val decoded=withContext(Dispatchers.IO) {ImageSource.decode(store.source(p),480)}
                val preview=try {renderer.render(decoded,edit,directory=store.directory(p.id))} finally {decoded.recycle()}
                try {store.save(next,preview)} finally {preview.recycle()}
                done++;mutable.update {it.copy(batchProgress=done,exported=output)}
            }
            mutable.update {it.copy(busy=false,batchTotal=0,projects=store.list(),notice=if(stopBatch) "Lote interrompido: $done fotos concluídas." else "$done fotos exportadas e projetos guardados.")}
        } catch(e:Exception) {mutable.update {it.copy(batchTotal=0)};fail(Exception("$done fotos concluídas. ${e.message}",e))}
    }
    fun reference(uri:Uri)=smartAction {state,project->
        val copied=store.addPhoto(project,uri);val bitmap=withContext(Dispatchers.IO) {ImageSource.decode(java.io.File(store.directory(project.id),copied),256)}
        val source=state.preview ?: state.source ?: error("Aguarde a prévia.")
        val treatment=ReferenceMatcher.match(source,bitmap);referencePreview=bitmap
        Triple("Estética de referência","Aproximação local das cores e da distribuição de luz. Revise a prévia; composição, textura e identidade da foto permanecem suas.",state.edit.copy(color=treatment))
    }
    fun thumbFile(project: Project) = store.thumbnail(project)
    suspend fun templatePreview(template:StoryTemplate)=stories.render(template.document(),store.templateDirectory(),template.editState(),maxEdge=320)
    override fun onCleared() { super.onCleared(); ui.value.enhancementProposal?.file?.delete(); CoroutineScope(Dispatchers.Default).launch { stories.close(); renderer.close();thumbnailRenderer.close() } }
}
