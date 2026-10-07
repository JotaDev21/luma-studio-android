package studio.luma.core

import kotlin.test.*

class EnhancementEndpointTest {
    @Test fun localComputerAndHttpsEndpointsAreAccepted() {
        assertEquals("http://192.168.100.190:8765",EnhancementEndpoint.normalize(" http://192.168.100.190:8765/ "))
        for(url in listOf("http://127.0.0.1:8765","http://10.0.2.2:8765","http://172.16.0.2:8765","https://ai.example.com")) assertEquals(url,EnhancementEndpoint.normalize(url))
    }
    @Test fun credentialsPathsAndPublicCleartextAreRejected() {
        for(url in listOf("http://8.8.8.8","http://example.com","http://172.32.0.2","http://192.168.999.1","https://user:pass@example.com","https://example.com/path","https://example.com?token=secret","https://example.com#x","file:///photo.png","https://example.com:99999")) assertFailsWith<IllegalArgumentException>(url) {EnhancementEndpoint.normalize(url)}
    }
}
