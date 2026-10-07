package studio.luma

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.net.URLEncoder

@RunWith(AndroidJUnit4::class)
class PairingQrFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun show() {
        assertTrue(compose.activity.packageName.endsWith(".qa"))
        compose.runOnUiThread {compose.activity.setShowWhenLocked(true);compose.activity.setTurnScreenOn(true)}
        compose.waitUntil(30000) {!compose.activity.studio.ui.value.busy}
    }
    @Test fun validQrShowsServerAndCancelDoesNotConnectOrSendAnything() {
        val vm=compose.activity.studio
        val endpoint="http://192.168.1.250:8765"
        val value="luma://connect?v=1&server=${URLEncoder.encode(endpoint,"UTF-8")}&code=001973&expires=${System.currentTimeMillis()/1000+500}"
        compose.runOnUiThread {vm.disconnectEnhancement();vm.reviewEnhancementQr(value)}
        compose.onNodeWithTag("ai-qr-server").assertTextEquals(endpoint)
        compose.onNodeWithTag("ai-qr-confirm").assertIsEnabled()
        assertFalse(vm.ui.value.enhancementConnected)
        assertFalse(vm.ui.value.enhancing)
        compose.onNodeWithText("Cancelar").performClick()
        assertNull(vm.ui.value.pairingProposal)
        assertFalse(vm.ui.value.enhancementConnected)
    }
    @Test fun unrelatedOrExpiredQrShowsActionableErrorWithoutConnecting() {
        val vm=compose.activity.studio
        compose.runOnUiThread {vm.disconnectEnhancement();vm.reviewEnhancementQr("https://example.com")}
        compose.onNodeWithText("Este QR não é uma conexão do Luma.").assertExists()
        compose.onNodeWithText("Entendi").performClick()
        val value="luma://connect?v=1&server=http%3A%2F%2F192.168.1.250%3A8765&code=001973&expires=1"
        compose.runOnUiThread {vm.reviewEnhancementQr(value)}
        compose.onNodeWithText("Este QR expirou. Abra Ver conexão Luma IA no PC para gerar outro.").assertExists()
        compose.onNodeWithText("Entendi").performClick()
        assertNull(vm.ui.value.pairingProposal)
        assertFalse(vm.ui.value.enhancementConnected)
    }
}
