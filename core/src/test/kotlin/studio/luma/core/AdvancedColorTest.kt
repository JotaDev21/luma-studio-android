package studio.luma.core
import kotlin.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.*
class AdvancedColorTest {
    private val identity="""TITLE "Identity"
LUT_3D_SIZE 2
0 0 0
1 0 0
0 1 0
1 1 0
0 0 1
1 0 1
0 1 1
1 1 1"""
    @Test fun cubeTrilinearChannelsDomainsAndMalformedFiles() {
        val lut=CubeParser.parse(identity);assertEquals(listOf(.3f,.6f,.8f),lut.sample(listOf(.3f,.6f,.8f)))
        val domain=lut.copy(domainMin=listOf(-1f,-1f,-1f),domainMax=listOf(1f,1f,1f));assertEquals(listOf(.5f,.5f,.5f),domain.sample(listOf(0f,0f,0f)))
        listOf(identity.replace("SIZE 2","SIZE 33"),identity+"\n0 0 0",identity.replace("1 1 1","NaN 1 1"),identity.replace("LUT_3D_SIZE","LUT_1D_SIZE")).forEach {assertFails {CubeParser.parse(it)}}
    }
    @Test fun historyAndAdvancedParametersSurviveRestartWithRedo() {
        val first=EditState(color=ColorTreatment(hsl=List(8) {HslBand(saturation=it/10f)},lut=CubeParser.parse(identity)),adjustments=Adjustments(clarity=.2f,denoise=.3f))
        val second=first.copy(color=first.color.copy(rolloff=.4f));val h=EditHistory(EditState());h.commit(first);h.commit(second);h.undo()
        val p=Project(id="a1234567-1234-1234-1234-123456789abc",createdAt=1,updatedAt=1,edit=h.current,history=h.snapshot())
        val restored=ProjectJson.decodeFromString<Project>(ProjectJson.encodeToString(p));restored.validate();val rh=EditHistory(restored.edit,restored=restored.history);assertEquals(second,rh.redo());assertEquals(first,rh.undo());assertEquals(EditState(),rh.undo())
    }
    @Test fun nestedResourcesAreCollectedRemappedAndPacked() {
        val font="font_a1234567-1234-1234-1234-123456789abc";val texture="photo_b1234567-1234-1234-1234-123456789abc"
        val effect=EffectPreset(meta=AssetMeta("texture","Texture","Custom"),type="texture",parameters=emptyMap(),asset=texture)
        val layer=StoryLayer("text","text",LayerTransform(0f,0f,200f,100f),content="Hello",typography=Typography(fontAsset=font))
        val template=StoryTemplate(meta=AssetMeta("template","Template","Custom"),layers=listOf(layer),effects=listOf(EffectLayer("e",effect)))
        val pack=LibraryPack(meta=AssetMeta("pack","Pack","Custom"),templates=listOf(template));assertEquals(setOf(font,texture),pack.assetNames())
        val output=ByteArrayOutputStream();val bytes=pack.assetNames().associateWith {byteArrayOf(1,2,3)};PackCodec.write(pack,bytes,output);assertEquals(pack,PackCodec.read(ByteArrayInputStream(output.toByteArray())).manifest)
        val renamed=template.remapAssets(mapOf(font to "font_c1234567-1234-1234-1234-123456789abc"));assertTrue(font !in renamed.assetNames())
    }
    @Test fun alignmentRespectsLockedLayersAndSnapsAtMargins() {
        val t=LayerTransform(133f,12f,200f,100f);assertEquals(129.6f,CompositionTools.snap(t,1080,1920).x,.001f)
        val l=StoryLayer("a","shape",t,locked=true);assertEquals(l,CompositionTools.align(StoryDocument(layers=listOf(l)),"a","right","bottom").layers.single())
    }
}
