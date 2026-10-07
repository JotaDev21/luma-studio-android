package studio.luma

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import studio.luma.core.*
import studio.luma.data.*
import studio.luma.ui.StudioApp
import java.io.File

@RunWith(AndroidJUnit4::class)
class UsabilityFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun ready(vm:StudioViewModel=compose.activity.studio)=compose.waitUntil(45000) {!vm.ui.value.busy && !vm.ui.value.rendering}
    @Before fun show() {
        assertTrue(InstrumentationRegistry.getInstrumentation().targetContext.packageName.endsWith(".qa"))
        compose.runOnUiThread {compose.activity.setShowWhenLocked(true);compose.activity.setTurnScreenOn(true);compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
        compose.waitUntil(30000) {compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()};ready()
    }
    private fun capture(name:String) {
        val path=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: return
        File(path).mkdirs()
        File(path,name).outputStream().use {compose.onNode(isRoot()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    private fun withPhoto(action:(StudioViewModel,StudioStore,Project)->Unit) {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity)
        val file=File(compose.activity.cacheDir,"usability-portrait.png")
        val bitmap=InstrumentationRegistry.getInstrumentation().context.assets.open("portrait-nasa.jpg").use {BitmapFactory.decodeStream(it)}
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        val original=runBlocking {store.import(Uri.fromFile(file))}
        try {compose.runOnUiThread {vm.open(original)};ready();action(vm,store,original)}
        finally {compose.runOnUiThread {vm.closeEditor(true)};ready();runBlocking {store.deleteProject(original.id)};file.delete()}
    }
    @Test fun homeOffersThreeClearStartingActionsAndStoriesOfferModelsOrBlank() {
        compose.onNodeWithTag("home-photo").assertIsDisplayed()
        compose.onNodeWithTag("home-story").assertIsDisplayed()
        compose.onNodeWithTag("home-collage").assertIsDisplayed()
        compose.onNodeWithText("Photo editor").assertDoesNotExist()
        capture("easy-home.png")
        compose.onNodeWithTag("home-story").performClick()
        assertEquals(Page.TEMPLATES,compose.activity.studio.ui.value.page)
        compose.onNodeWithText("Criar Story em branco").performScrollTo().performClick();ready()
        val vm=compose.activity.studio;val id=vm.ui.value.project!!.id
        try {assertNotNull(vm.ui.value.edit.story)}
        finally {compose.runOnUiThread {vm.closeEditor(true)};ready();runBlocking {StudioStore(compose.activity).deleteProject(id)}}
    }
    @Test fun draftRecoversInANewViewModelAndDiscardKeepsExplicitSaveAndSource()=withPhoto {vm,store,base->
        val bytes=store.source(base).readBytes()
        val expected=base.edit.copy(adjustments=Adjustments(exposure=.42f))
        compose.runOnUiThread {vm.update(expected,false);vm.flushDraft()};ready()
        compose.waitUntil(10000) {vm.ui.value.draftState==DraftState.SAVED}
        assertEquals(base.edit,runBlocking {store.list()}.first {it.id==base.id}.edit)
        val owner=ViewModelStore()
        lateinit var fresh:StudioViewModel
        compose.runOnUiThread {fresh=ViewModelProvider(owner,ViewModelProvider.AndroidViewModelFactory.getInstance(compose.activity.application))[StudioViewModel::class.java]}
        try {
            ready(fresh)
            compose.runOnUiThread {fresh.open(base)};ready(fresh)
            assertEquals(expected,fresh.ui.value.edit)
            assertTrue(fresh.ui.value.canUndo)
            compose.runOnUiThread {fresh.undo()};ready(fresh)
            assertEquals(base.edit,fresh.ui.value.edit)
            compose.runOnUiThread {fresh.closeEditor(true)};ready(fresh)
        } finally {compose.runOnUiThread {owner.clear()}}
        compose.runOnUiThread {vm.update(expected);vm.keepDraftAndClose()};ready()
        assertEquals(Page.HOME,vm.ui.value.page)
        compose.runOnUiThread {vm.open(base)};ready()
        assertEquals(expected,vm.ui.value.edit)
        compose.onNodeWithContentDescription("Mais ferramentas").performClick()
        compose.onNodeWithText("Descartar rascunho").performScrollTo().performClick()
        compose.onNodeWithText("Descartar alterações").performClick();ready()
        assertNull(runBlocking {ProjectDraftStore(compose.activity).read(base)})
        assertEquals(base.edit,runBlocking {store.list()}.first {it.id==base.id}.edit)
        assertArrayEquals(bytes,store.source(base).readBytes())
    }
    @Test fun simpleModeKeepsCommonToolsVisibleAndAdvancedToolsReachable()=withPhoto {vm,_,_->
        compose.onNode(hasText("Luz e cor") and hasAnyAncestor(hasTestTag("photo-tools"))).assertIsDisplayed().performClick()
        compose.onNodeWithTag("photo-adjustment").performSemanticsAction(SemanticsActions.SetProgress) {it(.25f)};ready()
        assertEquals(.25f,vm.ui.value.edit.adjustments.exposure,.01f)
        compose.onNode(hasText("Máscaras") and hasAnyAncestor(hasTestTag("photo-tools"))).assertDoesNotExist()
        compose.onNodeWithContentDescription("Mais ferramentas").performClick()
        compose.onNodeWithText("Mostrar todas as ferramentas").performClick()
        compose.onNodeWithTag("photo-tools").performScrollToNode(hasText("Máscaras"))
        compose.onNode(hasText("Máscaras") and hasAnyAncestor(hasTestTag("photo-tools"))).assertIsDisplayed()
        compose.onNodeWithContentDescription("Mais ferramentas").performClick()
        compose.onNodeWithText("Usar modo simples").performClick()
        compose.onNode(hasText("Máscaras") and hasAnyAncestor(hasTestTag("photo-tools"))).assertDoesNotExist()
        compose.onNodeWithContentDescription("Mais ferramentas").performClick()
        compose.onNodeWithText("Como usar").performClick()
        compose.onNodeWithText("Como usar o Luma").assertIsDisplayed()
        compose.onNodeWithText("Entendi").performClick()
        capture("easy-photo-editor.png")
    }
    @Test fun doubledFontStillAllowsEditingAndSavingToGallery()=withPhoto {vm,store,base->
        val density=compose.activity.resources.displayMetrics.density
        compose.runOnUiThread {compose.activity.setContent {CompositionLocalProvider(LocalDensity provides Density(density,2f)) {StudioApp(vm)}}}
        compose.onNodeWithTag("photo-save").assertIsDisplayed()
        compose.onNodeWithContentDescription("Desfazer").assertHeightIsAtLeast(48.dp)
        compose.onNode(hasText("Luz e cor") and hasAnyAncestor(hasTestTag("photo-tools"))).performScrollTo().performClick()
        compose.onNodeWithTag("photo-adjustment").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {it(.3f)};ready()
        assertEquals(.3f,vm.ui.value.edit.adjustments.exposure,.01f)
        capture("easy-font-200.png")
        compose.onNodeWithTag("photo-save").performScrollTo().performClick()
        compose.onNodeWithText("Salvar na galeria").assertIsDisplayed().performClick();ready()
        assertNull(vm.ui.value.error)
        val exported=vm.ui.value.exported!!
        try {compose.activity.contentResolver.openInputStream(exported).use {input->val image=BitmapFactory.decodeStream(input);assertEquals(480 to 600,image.width to image.height);image.recycle()}}
        finally {compose.activity.contentResolver.delete(exported,null,null)}
        assertTrue(store.source(base).exists())
    }
}
