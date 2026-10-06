package studio.luma.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.*
import java.util.Base64
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream

class RasterMaskTest {
    @Test fun compactConfidenceAndManualCorrectionsRoundTrip() {
        val alpha=ByteArray(256*256) {if(it%256 in 60..190) 255.toByte() else 0}
        val raster=RasterMask.fromAlpha(256,256,alpha)
        assertTrue(raster.data.length<2000);assertContentEquals(alpha,raster.decode())
        val m=LocalMask("person","Pessoa",type="person",raster=raster,inverted=true,cutout=true,threshold=.6f,strokes=listOf(BrushStroke(listOf(Point(.1f,.1f)),erase=true)))
        val edit=EditState(masks=listOf(m));edit.validate()
        assertEquals(edit,ProjectJson.decodeFromString<EditState>(ProjectJson.encodeToString(edit)))
        assertEquals(m.geometry(),m.copy(cutout=false,strength=.2f,inverted=false).geometry())
    }
    @Test fun truncatedOversizedTrailingAndMalformedMapsAreRejected() {
        val good=RasterMask.fromAlpha(10,10,ByteArray(100))
        assertFailsWith<IllegalArgumentException> {good.copy(width=257).validate()}
        assertFailsWith<IllegalArgumentException> {good.copy(height=11).validate()}
        assertFailsWith<IllegalArgumentException> {good.copy(data="not-base64").validate()}
        val packed=Base64.getDecoder().decode(good.data)
        assertFailsWith<IllegalArgumentException> {good.copy(data=Base64.getEncoder().encodeToString(packed.copyOf(packed.size-1))).validate()}
        assertFailsWith<IllegalArgumentException> {good.copy(data=Base64.getEncoder().encodeToString(packed+byteArrayOf(0))).validate()}
        val bytes=ByteArrayOutputStream();DeflaterOutputStream(bytes).use {it.write(ByteArray(1_000_000))}
        assertFailsWith<IllegalArgumentException> {RasterMask(1,1,Base64.getEncoder().encodeToString(bytes.toByteArray())).validate()}
    }
    @Test fun rasterBelongsOnlyToAutomaticMasksAndOldProjectsRemainCompatible() {
        val raster=RasterMask.fromAlpha(1,1,byteArrayOf(255.toByte()))
        assertFailsWith<IllegalArgumentException> {LocalMask("a","a",type="person").validate()}
        assertFailsWith<IllegalArgumentException> {LocalMask("a","a",raster=raster).validate()}
        assertFailsWith<IllegalArgumentException> {LocalMask("a","a",cutout=true).validate()}
        assertFailsWith<IllegalArgumentException> {LocalMask("a","a",type="person",raster=raster,threshold=Float.NaN).validate()}
        val old=ProjectJson.decodeFromString<Project>("""{"schemaVersion":5,"id":"a1234567-1234-1234-1234-123456789abc","createdAt":1,"updatedAt":2,"edit":{"masks":[{"id":"a","name":"a","type":"radial"}]}}""")
        old.validate();assertNull(old.edit.masks.single().raster);assertFalse(old.edit.masks.single().cutout)
    }
}
