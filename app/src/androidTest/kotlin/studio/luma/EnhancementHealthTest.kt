package studio.luma

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import studio.luma.data.EnhancementClient

@RunWith(AndroidJUnit4::class)
class EnhancementHealthTest {
    @Test fun pcHealthReportsReadyLoadingAndOccupiedWithoutImageOrCredential()=runBlocking {
        val server=MockWebServer();server.start()
        try {
            val client=EnhancementClient()
            val address="http://127.0.0.1:${server.port}"
            listOf(false to false,true to true,true to false).zip(listOf(AiAvailability.LOADING,AiAvailability.OCCUPIED,AiAvailability.READY)).forEach {(flags,expected)->
                server.enqueue(MockResponse().setBody("{\"service\":\"Luma AI\",\"ready\":${flags.first},\"busy\":${flags.second}}"))
                assertEquals(expected,client.health(address))
                val request=server.takeRequest()
                assertEquals("GET",request.method);assertEquals("/health",request.path)
                assertEquals(0L,request.bodySize);assertNull(request.getHeader("Authorization"))
            }
            server.enqueue(MockResponse().setBody("{\"service\":\"Unknown\",\"ready\":true,\"busy\":false}"))
            assertTrue(runCatching {client.health(address)}.isFailure)
            server.enqueue(MockResponse().setBody("x".repeat(4097)))
            assertTrue(runCatching {client.health(address)}.isFailure)
        } finally {server.shutdown()}
    }
}
