package studio.luma.core

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.math.*
import kotlin.test.*

class SmartFramingTest {
    private fun asset(i:Int)="photo_a1234567-1234-1234-1234-${i.toString().padStart(12,'0')}"
    private fun photo(i:Int,w:Float,h:Float)=StoryLayer("slot_$i","photo",LayerTransform(i*100f,20f,w,h),asset=asset(i))
    @Test fun offCenterFaceKeepsHeadroomInsteadOfBeingCutByCentralCrop() {
        val face=Crop(.72f,.06f,.9f,.26f);val focus=PhotoFocus(1600,900,listOf(face),Point(.81f,.16f))
        val crop=assertNotNull(SmartFraming.crop(focus,9f/16))
        assertTrue(crop.left<=face.left && crop.right>=face.right && crop.top<=face.top && crop.bottom>=face.bottom)
        assertTrue(crop.left>.6f)
        assertEquals(9f/16,1600f/900*(crop.right-crop.left)/(crop.bottom-crop.top),.00001f)
    }
    @Test fun distantFacesUseEntirePhotoAndRecoverFacesOutsideManualCrop() {
        val focus=PhotoFocus(1600,900,listOf(Crop(.05f,.1f,.2f,.3f),Crop(.8f,.1f,.95f,.3f)))
        assertNull(SmartFraming.crop(focus,9f/16))
        val layer=photo(1,90f,160f).copy(photoTransform=Transform(Crop(.4f,0f,.6f,1f)),photoZoom=3f,photoPanX=1f)
        val framed=SmartFraming.frame(layer,focus)
        assertEquals("contain",framed.photoFit);assertEquals(Crop(),framed.photoTransform.crop)
        assertEquals(1f,framed.photoZoom);assertEquals(0f,framed.photoPanX)
        assertEquals(layer.asset,framed.asset)
    }
    @Test fun personAndCenterFallbackRespectEveryRotationAndFlip() {
        val focus=PhotoFocus(1200,800,center=Point(.8f,.5f),person=true)
        for(turn in 0..3) for(flip in listOf(false,true)) {
            val transform=Transform(quarterTurns=turn,flipX=flip,flipY=!flip)
            val crop=assertNotNull(SmartFraming.crop(focus,.8f,transform));crop.validate()
            val raw=1.5f*(crop.right-crop.left)/(crop.bottom-crop.top)
            assertEquals(.8f,if(turn%2==0) raw else 1f/raw,.00001f)
            val framed=SmartFraming.frame(photo(1,80f,100f).copy(photoTransform=transform),focus)
            assertEquals(turn,framed.photoTransform.quarterTurns);assertEquals(flip,framed.photoTransform.flipX);assertEquals(!flip,framed.photoTransform.flipY)
        }
        val central=assertNotNull(SmartFraming.crop(PhotoFocus(1200,800),1f))
        assertEquals((1f-(central.right-central.left))/2,central.left,.00001f)
    }
    @Test fun assignmentMatchesOrientationsAndPreservesLockedHiddenAndGraphicLayers() {
        val layers=listOf(photo(1,200f,100f),photo(2,100f,200f),photo(3,100f,100f),photo(4,100f,100f).copy(locked=true),photo(5,100f,100f).copy(visible=false),StoryLayer("text","text",LayerTransform(0f,0f,100f,40f),content="Meu texto"))
        val focus=mapOf(asset(1) to PhotoFocus(100,200),asset(2) to PhotoFocus(200,100),asset(3) to PhotoFocus(100,100))
        val doc=StoryDocument(layers=layers);val organized=SmartFraming.organize(doc,focus)
        assertEquals(listOf(asset(2),asset(1),asset(3)),organized.layers.take(3).map {it.asset})
        assertEquals(doc.layers.drop(3),organized.layers.drop(3));assertEquals(doc.layers.map {it.id},organized.layers.map {it.id})
        assertEquals(doc.layers.map {it.transform},organized.layers.map {it.transform})
    }
    @Test fun emptyFrameCanReceivePhotoAndItsMasksWithoutLosingFrameAppearance() {
        val mask=LocalMask("mask","Local",type="radial")
        val original=photo(1,200f,100f).copy(masks=listOf(mask),photoTransform=Transform(flipX=true))
        val empty=photo(2,100f,200f).copy(asset=null,placeholder=true,appearance=LayerAppearance(border=10f,bottomBorder=30f),transform=LayerTransform(0f,0f,100f,200f,15f,.4f))
        val doc=SmartFraming.organize(StoryDocument(layers=listOf(original,empty)),mapOf(asset(1) to PhotoFocus(100,200)))
        assertNull(doc.layers[0].asset);assertTrue(doc.layers[0].masks.isEmpty())
        assertEquals(original.asset,doc.layers[1].asset);assertEquals(listOf(mask),doc.layers[1].masks)
        assertTrue(doc.layers[1].photoTransform.flipX);assertEquals(empty.appearance,doc.layers[1].appearance);assertEquals(empty.transform,doc.layers[1].transform)
        assertEquals(.5f,SmartFraming.contentAspect(empty),.00001f)
    }
    @Test fun globalAssignmentHasMinimumCostAndHandlesTwentyPhotos() {
        val sources=listOf(.4f,.8f,1.2f,2.5f);val frames=listOf(1f,.5f,2f,1.5f)
        val layers=frames.mapIndexed {i,r->photo(i,r*100,100f)}
        val focus=sources.mapIndexed {i,r->asset(i) to PhotoFocus((r*1000).toInt(),1000)}.toMap()
        fun cost(order:List<Int>)=order.indices.sumOf {slot->abs(ln((sources[order[slot]]/frames[slot]).toDouble()))}
        fun permutations(values:List<Int>):List<List<Int>> = if(values.isEmpty()) listOf(emptyList()) else values.flatMap {i->permutations(values-i).map {listOf(i)+it}}
        val result=SmartFraming.organize(StoryDocument(layers=layers),focus)
        val order=result.layers.map {l->layers.indexOfFirst {it.asset==l.asset}}
        assertEquals(permutations(layers.indices.toList()).minOf(::cost),cost(order),.000001)
        val twenty=List(20) {i->photo(i,if(i%2==0) 80f else 160f,100f)}
        val all=twenty.associate {it.asset!! to PhotoFocus(if(it.id.removePrefix("slot_").toInt()%2==0) 160 else 80,100)}
        val large=SmartFraming.organize(StoryDocument(layers=twenty),all)
        assertEquals(twenty.map {it.asset}.toSet(),large.layers.map {it.asset}.toSet());assertEquals(20,large.layers.map {it.asset}.distinct().size)
    }
    @Test fun containmentPersistsAndOldVersionsDefaultToCover() {
        val edit=EditState(story=StoryDocument(layers=listOf(photo(1,100f,100f).copy(photoFit="contain"))))
        val project=Project(id="a1234567-1234-1234-1234-123456789abc",createdAt=1,updatedAt=2,edit=edit)
        assertEquals(project,ProjectJson.decodeFromString<Project>(ProjectJson.encodeToString(project)));project.validate()
        for(version in 1..6) {
            val old=ProjectJson.decodeFromString<Project>("""{"schemaVersion":$version,"id":"a1234567-1234-1234-1234-123456789abc","createdAt":1,"updatedAt":2,"edit":{"story":{"layers":[{"id":"a","type":"photo","transform":{"x":0,"y":0,"width":100,"height":100}}]}}}""")
            old.validate();assertEquals("cover",old.edit.story!!.layers.single().photoFit)
        }
        val history=EditHistory(EditState());history.commit(edit);assertEquals(EditState(),history.undo());assertEquals(edit,history.redo())
        assertFailsWith<IllegalArgumentException> {photo(1,100f,100f).copy(photoFit="other").validate()}
    }
}
