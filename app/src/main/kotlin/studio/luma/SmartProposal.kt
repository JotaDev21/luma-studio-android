package studio.luma

import android.graphics.Bitmap
import studio.luma.core.EditState

/** Only lives in the editor. Acceptance commits parameters once; cancelling commits nothing. */
data class SmartProposal(val title:String,val explanation:String,val base:EditState,val edit:EditState,val before:Bitmap,val preview:Bitmap,val reference:Bitmap?=null)
