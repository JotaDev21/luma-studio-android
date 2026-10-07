package studio.luma.data

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import studio.luma.core.Project
import studio.luma.core.ProjectJson
import java.io.File

@Serializable private data class ProjectDraft(val baseUpdatedAt:Long,val project:Project)

/** Drafts are separate from explicit saves, so discarding never rewrites the source. */
class ProjectDraftStore(context:Context) {
    private val root=File(context.filesDir,"projects")
    private companion object {val writes=Mutex()}
    private fun file(id:String):AtomicFile {
        require(id.matches(Regex("[a-f0-9-]{36}")))
        return AtomicFile(File(File(root,id),"draft.json"))
    }
    suspend fun read(base:Project):Project?=withContext(Dispatchers.IO) {writes.withLock {
        val draft=file(base.id)
        if(!draft.baseFile.exists() && !File(draft.baseFile.path+".bak").exists()) return@withLock null
        runCatching {
            val saved=ProjectJson.decodeFromString<ProjectDraft>(draft.openRead().bufferedReader().use {it.readText()})
            saved.project.validate()
            require(saved.baseUpdatedAt==base.updatedAt && saved.project.id==base.id)
            saved.project
        }.getOrElse {draft.delete();null}
    }}
    suspend fun write(base:Project,current:Project)=withContext(Dispatchers.IO+NonCancellable) {writes.withLock {
        require(base.id==current.id);current.validate()
        val draft=file(base.id)
        // A pending save must never recreate a deleted project directory.
        val savedFile=AtomicFile(File(draft.baseFile.parentFile,"project.json"))
        check(savedFile.baseFile.exists()) {"O projeto não está mais disponível para salvar."}
        val saved=ProjectJson.decodeFromString<Project>(savedFile.openRead().bufferedReader().use {it.readText()})
        check(saved.id==base.id && saved.updatedAt==base.updatedAt) {"O projeto mudou durante o salvamento. Abra-o novamente."}
        val bytes=ProjectJson.encodeToString(ProjectDraft(base.updatedAt,current)).toByteArray(Charsets.UTF_8)
        val stream=draft.startWrite()
        try {stream.write(bytes);draft.finishWrite(stream)} catch(error:Exception) {draft.failWrite(stream);throw error}
    }}
    suspend fun clear(id:String)=withContext(Dispatchers.IO+NonCancellable) {writes.withLock {file(id).delete()}}
}
