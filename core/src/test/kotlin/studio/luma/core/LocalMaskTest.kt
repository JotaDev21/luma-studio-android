package studio.luma.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.*

class LocalMaskTest {
    @Test fun vectorsRoundTripAndHistoryCommitsOneCompleteStroke() {
        val mask=LocalMask("paint","Meu retoque",inverted=true,feather=.3f,strokes=listOf(BrushStroke(listOf(Point(.1f,.2f),Point(.7f,.6f)),.04f,.6f),BrushStroke(listOf(Point(.3f,.4f)),erase=true)),adjustments=Adjustments(exposure=.7f))
        val edit=EditState(masks=listOf(mask));edit.validate()
        assertEquals(edit,ProjectJson.decodeFromString<EditState>(ProjectJson.encodeToString(edit)))
        val history=EditHistory(EditState());history.commit(edit)
        assertEquals(EditState(),history.undo());assertEquals(edit,history.redo())
        val photo=StoryLayer("photo","photo",LayerTransform(0f,0f,500f,500f),masks=listOf(mask))
        photo.validate();assertFailsWith<IllegalArgumentException> {photo.copy(type="text").validate()}
    }
    @Test fun coordinatesInvertAllCropsFlipsAndRotations() {
        val point=Point(.3f,.6f)
        for(turn in 0..3) for(x in listOf(false,true)) for(y in listOf(false,true)) {
            val t=Transform(Crop(.1f,.2f,.8f,.9f),turn,x,y)
            val result=MaskCoordinates.toSource(MaskCoordinates.toDisplay(point,t),t)
            assertEquals(point.x,result.x,.00001f);assertEquals(point.y,result.y,.00001f)
        }
        assertEquals(Point(.5f,.5f),MaskCoordinates.toSource(Point(0f,0f),Transform(Crop(.5f,.5f,1f,1f))))
    }
    @Test fun invalidAndOversizedMasksAreRejected() {
        val m=LocalMask("a","a")
        assertFailsWith<IllegalArgumentException> {m.copy(feather=Float.NaN).validate()}
        assertFailsWith<IllegalArgumentException> {m.copy(end=m.start).validate()}
        assertFailsWith<IllegalArgumentException> {m.copy(strokes=listOf(BrushStroke(List(2049) {Point(.5f,.5f)}))).validate()}
        assertFailsWith<IllegalArgumentException> {m.copy(strokes=List(101) {BrushStroke(listOf(Point(.5f,.5f)))}).validate()}
        assertFailsWith<IllegalArgumentException> {EditState(masks=listOf(m,m)).validate()}
        assertFailsWith<IllegalArgumentException> {EditState(story=StoryDocument(),masks=listOf(m)).validate()}
        assertFailsWith<IllegalArgumentException> {EditState(masks=List(9) {m.copy(id="$it")}).validate()}
    }
    @Test fun previousProjectSchemasKeepNeutralMasksAndGeometryIgnoresToneChanges() {
        val old=ProjectJson.decodeFromString<Project>("""{"schemaVersion":4,"id":"a1234567-1234-1234-1234-123456789abc","createdAt":1,"updatedAt":2,"edit":{}}""")
        old.validate();assertTrue(old.edit.masks.isEmpty())
        val a=LocalMask("a","a",type="radial")
        assertEquals(a.geometry(),a.copy(name="b",id="b",strength=.1f,inverted=true,enabled=false,adjustments=Adjustments(exposure=1f)).geometry())
    }
}
