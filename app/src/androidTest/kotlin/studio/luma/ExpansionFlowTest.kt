package studio.luma

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import studio.luma.core.*
import studio.luma.data.StudioStore
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExpansionFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun show() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        compose.runOnUiThread {compose.activity.setShowWhenLocked(true);compose.activity.setTurnScreenOn(true);compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
        ready()
    }
    private fun ready() {
        compose.waitUntil(45000) {!compose.activity.studio.ui.value.busy && !compose.activity.studio.ui.value.rendering}
        // These regression cases exercise the complete editor; the guide has separate tests.
        if(compose.onAllNodesWithTag("story-guide-full").fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()) compose.onNodeWithTag("story-guide-full").performScrollTo().performClick()
    }
    private fun fixture()=File(compose.activity.cacheDir,"expansion-flow.jpg").apply {
        InstrumentationRegistry.getInstrumentation().context.assets.open("portrait-nasa.jpg").use {input->outputStream().use {input.copyTo(it)}}
    }
    private fun capture(name:String,tag:String?=null) {
        val directory=File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")!!).apply {mkdirs()}
        val node=if(tag==null) compose.onNode(isRoot()) else compose.onNodeWithTag(tag)
        File(directory,name).outputStream().use {node.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun templateSearchFavoritesSixPhotosStoryFilterAndSavedProjectWorkTogether() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity);val file=fixture();var projectId:String?=null
        val favorite="social-template-01-02";val wasFavorite=favorite in vm.ui.value.favorites
        try {
            compose.runOnUiThread {vm.page(Page.TEMPLATES)}
            compose.waitUntil(15000) {compose.onAllNodesWithTag("template-search").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithTag("template-search").performTextReplacement("seis momentos")
            compose.onNodeWithTag("template-$favorite").assertExists()
            if(!wasFavorite) compose.onNodeWithContentDescription("Favoritar Seis momentos").performClick()
            compose.waitUntil(10000) {favorite in vm.ui.value.favorites}
            compose.onNodeWithTag("template-categories").performScrollToNode(hasText("Favoritos"))
            compose.onNode(hasText("Favoritos") and hasAnyAncestor(hasTestTag("template-categories"))).performClick()
            compose.onNodeWithTag("template-$favorite").assertExists()
            compose.onNodeWithTag("template-search").assertIsNotFocused().performTextClearance()
            compose.onNode(hasText("Novos") and hasAnyAncestor(hasTestTag("template-categories"))).performClick()
            compose.onNodeWithTag("template-list").performScrollToIndex(0)
            compose.waitUntil(20000) {compose.onAllNodesWithTag("template-social-template-01-01").fetchSemanticsNodes().isNotEmpty()}
            capture("expanded-template-gallery.png")
            compose.onNodeWithTag("template-list").performScrollToNode(hasTestTag("template-search"))
            compose.onNodeWithTag("template-search").performTextReplacement("seis momentos")
            compose.onNode(hasText("Novos") and hasAnyAncestor(hasTestTag("template-categories"))).performClick()
            compose.onNodeWithTag("template-$favorite").performClick();ready()
            projectId=vm.ui.value.project!!.id;assertEquals(6,vm.ui.value.edit.story!!.missingPhotos)
            compose.runOnUiThread {vm.addStoryPhotos(List(6) {Uri.fromFile(file)})};ready()
            assertEquals(0,vm.ui.value.edit.story!!.missingPhotos)
            compose.onNodeWithTag("story-tools").performScrollToNode(hasText("Looks"))
            compose.onNode(hasText("Looks") and hasAnyAncestor(hasTestTag("story-tools"))).performClick()
            compose.onNodeWithTag("story-look-search").performTextReplacement("valencia")
            compose.onNodeWithTag("story-look-categories").performScrollToNode(hasText("Instagram"))
            compose.onNode(hasText("Instagram") and hasAnyAncestor(hasTestTag("story-look-categories"))).performClick()
            compose.onNodeWithText("Valencia Soft").performClick();ready()
            assertEquals("social-valencia-soft",vm.ui.value.edit.filter!!.preset.meta.id)
            capture("expanded-story-editor.png")
            compose.runOnUiThread {vm.save("Story de seis momentos QA")};ready()
            val saved=runBlocking {store.list().first {it.id==projectId}}
            compose.runOnUiThread {vm.closeEditor()};ready();compose.runOnUiThread {vm.open(saved)};ready()
            assertEquals(saved.edit,vm.ui.value.edit);assertNull(vm.ui.value.error)
        } finally {
            if((favorite in vm.ui.value.favorites)!=wasFavorite) compose.runOnUiThread {vm.toggleFavorite(favorite)}
            compose.runOnUiThread {vm.closeEditor(true)};ready();projectId?.let {runBlocking {store.deleteProject(it)}};file.delete()
        }
    }
    @Test fun instagramLooksAndStyleBrowserApplyUndoAndReplaceGraphicsPreservingManualText() {
        val vm=compose.activity.studio;val store=StudioStore(compose.activity);val file=fixture()
        val project=runBlocking {store.import(Uri.fromFile(file))};val bytes=store.source(project).readBytes()
        val favorite="luma-style-social-12-01";val wasFavorite=favorite in vm.ui.value.favorites
        try {
            compose.runOnUiThread {vm.open(project)};ready()
            compose.onNodeWithTag("all-filters").performClick()
            compose.onNodeWithTag("look-collections").performScrollToNode(hasText("Instagram"))
            compose.onNode(hasText("Instagram") and hasAnyAncestor(hasTestTag("look-collections"))).performClick()
            compose.onNodeWithTag("look-search").performTextReplacement("juno soft")
            compose.onNode(hasTestTag("look-social-juno-soft") and hasAnyAncestor(hasTestTag("look-grid"))).performClick();ready()
            assertEquals("social-juno-soft",vm.ui.value.edit.filter!!.preset.meta.id)
            compose.onNodeWithTag("look-search").performTextClearance()
            compose.onNode(hasText("Instagram") and hasAnyAncestor(hasTestTag("look-collections"))).performClick()
            capture("expanded-instagram-gallery.png","look-browser")
            compose.onNodeWithText("Usar este filtro").performClick()
            compose.onNodeWithContentDescription("Mais ferramentas").performClick()
            compose.onNodeWithText("Estilos prontos").performClick()
            compose.onNodeWithText("Explorar estilos").performClick()
            compose.onNodeWithTag("style-search").performTextReplacement("instantaneo")
            if(!wasFavorite) compose.onNode(hasContentDescription("Favoritar Instantâneo / Natural") and hasAnyAncestor(hasTestTag("style-grid"))).performClick()
            compose.waitUntil(10000) {favorite in vm.ui.value.favorites}
            compose.onNodeWithTag("style-categories").performScrollToNode(hasText("Novos"))
            compose.onNode(hasText("Novos") and hasAnyAncestor(hasTestTag("style-categories"))).performClick()
            capture("expanded-style-browser.png","style-browser")
            compose.onNode(hasTestTag("style-$favorite") and hasAnyAncestor(hasTestTag("style-grid"))).performClick();ready()
            assertNull(vm.ui.value.edit.story);assertEquals("social-gingham-classic",vm.ui.value.edit.filter!!.preset.meta.id)
            compose.onNodeWithContentDescription("Desfazer").performClick();ready()
            assertEquals("social-juno-soft",vm.ui.value.edit.filter!!.preset.meta.id)
            val crop=Transform(Crop(.08f,.05f,.92f,.95f),0,true)
            compose.runOnUiThread {vm.update(vm.ui.value.edit.copy(transform=crop))};ready()
            compose.onNodeWithText("Explorar estilos").performClick()
            compose.onNodeWithTag("style-search").performTextReplacement("social legenda")
            compose.onNode(hasTestTag("style-luma-style-social-01-03") and hasAnyAncestor(hasTestTag("style-grid"))).performClick();ready()
            assertEquals(4,vm.ui.value.edit.story!!.layers.count {it.styleElement})
            assertEquals(crop,vm.ui.value.edit.story!!.layers.first {it.type=="photo"}.photoTransform)
            compose.runOnUiThread {vm.addLayer("text","MEU TEXTO")};ready()
            val second=vm.ui.value.styles.first {it.meta.id=="luma-style-social-08-04"}
            compose.runOnUiThread {vm.applyStyle(second)};ready()
            val doc=vm.ui.value.edit.story!!
            assertEquals(second.layers.size,doc.layers.count {it.styleElement});assertEquals(1,doc.layers.count {it.content=="MEU TEXTO"})
            assertEquals(1,doc.layers.count {it.type=="photo"});assertEquals(crop,doc.layers.first {it.type=="photo"}.photoTransform)
            compose.runOnUiThread {vm.undo()};ready();assertEquals(4,vm.ui.value.edit.story!!.layers.count {it.styleElement})
            assertArrayEquals(bytes,store.source(project).readBytes());assertNull(vm.ui.value.error)
        } finally {
            if((favorite in vm.ui.value.favorites)!=wasFavorite) compose.runOnUiThread {vm.toggleFavorite(favorite)}
            compose.runOnUiThread {vm.closeEditor(true)};ready();runBlocking {store.deleteProject(project.id)};file.delete()
        }
    }
}
