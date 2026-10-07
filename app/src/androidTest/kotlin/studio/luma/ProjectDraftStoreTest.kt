package studio.luma

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import studio.luma.core.*
import studio.luma.data.*
import java.io.File

@RunWith(AndroidJUnit4::class)
class ProjectDraftStoreTest {
    private fun withProject(action:suspend (StudioStore,ProjectDraftStore,Project)->Unit)=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(context.packageName.endsWith(".qa"))
        val store=StudioStore(context);val drafts=ProjectDraftStore(context)
        val file=File(context.cacheDir,"draft-storage-test.png")
        val bitmap=Bitmap.createBitmap(96,128,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.DKGRAY)
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        val project=store.import(Uri.fromFile(file))
        try {action(store,drafts,project)} finally {if(store.directory(project.id).exists()) store.deleteProject(project.id);file.delete()}
    }
    @Test fun recoveryKeepsHistoryAndSourceAndRejectsStaleOrCorruptDrafts()=withProject {store,drafts,base->
        val original=store.source(base).readBytes()
        val edited=base.edit.copy(adjustments=Adjustments(exposure=.42f))
        val history=EditHistory(base.edit).apply {commit(edited)}
        val current=base.copy(edit=edited,history=history.snapshot(),updatedAt=base.updatedAt+10)
        drafts.write(base,current)
        assertEquals(current,drafts.read(base))
        assertEquals(base.edit,store.list().first {it.id==base.id}.edit)
        val recovered=EditHistory(edited,restored=drafts.read(base)!!.history)
        assertEquals(base.edit,recovered.undo())
        assertArrayEquals(original,store.source(base).readBytes())
        val newer=base.copy(updatedAt=base.updatedAt+20)
        store.save(newer)
        assertNull(drafts.read(newer))
        drafts.write(newer,newer.copy(edit=edited))
        File(store.directory(base.id),"draft.json").writeText("{invalid")
        assertNull(drafts.read(newer))
        assertFalse(File(store.directory(base.id),"draft.json").exists())
        assertEquals(newer,store.list().first {it.id==base.id})
    }
    @Test fun discardClearsDraftAndDelayedWriteCannotRecreateDeletedProject()=withProject {store,drafts,base->
        val changed=base.copy(edit=base.edit.copy(adjustments=Adjustments(exposure=.5f)))
        drafts.write(base,changed);drafts.clear(base.id);assertNull(drafts.read(base))
        store.deleteProject(base.id)
        assertTrue(runCatching {drafts.write(base,changed)}.isFailure)
        assertFalse(store.directory(base.id).exists())
    }
}
