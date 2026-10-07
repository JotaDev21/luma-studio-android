package studio.luma.core

import org.junit.Test
import org.junit.Assert.*
import java.net.URLEncoder

class EnhancementPairingTest {
    private fun qr(server:String="http://192.168.100.190:8765",code:String="001973",expires:Long=1600)=
        "luma://connect?v=1&server=${URLEncoder.encode(server,"UTF-8")}&code=$code&expires=$expires"
    @Test fun acceptsPrivateServerAndHttpsWithoutLosingLeadingZeroes() {
        assertEquals(EnhancementPairing("http://192.168.100.190:8765","001973",1600),EnhancementPairing.parse(qr(),1000))
        assertEquals("https://photos.example.com",EnhancementPairing.parse(qr("https://photos.example.com"),1000).endpoint)
    }
    @Test fun rejectsWrongLinksDuplicatesCredentialsUnexpectedFieldsAndExpiredCodes() {
        listOf(qr().replace("luma:","https:"),qr()+"&code=111111",qr()+"&token=secret",qr("http://8.8.8.8"),qr("https://user:pass@photos.example.com"),qr("http://192.168.1.2/private"),qr(code="12a456"),qr(expires=1000),qr(expires=1800),qr().replace("v=1","v=2"),qr()+"#fragment").forEach {value->
            assertTrue(value,runCatching {EnhancementPairing.parse(value,1000)}.isFailure)
        }
        val pairing=EnhancementPairing.parse(qr(),1000)
        assertTrue(runCatching {pairing.requireFresh(1600)}.isFailure)
    }
}
