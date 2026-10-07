package studio.luma.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import studio.luma.core.EnhancementEndpoint
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class EnhancementConnection(val endpoint:String,val token:String)

class EnhancementConnectionStore(context:Context) {
    private val preferences=context.getSharedPreferences("enhancement-connection",Context.MODE_PRIVATE)
    private val alias="luma-enhancement-connection"
    private fun key():SecretKey {
        val keys=KeyStore.getInstance("AndroidKeyStore").apply {load(null)}
        (keys.getKey(alias,null) as? SecretKey)?.let {return it}
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun load():EnhancementConnection?=runCatching {
        val endpoint=preferences.getString("endpoint",null) ?: return null
        val encrypted=Base64.decode(preferences.getString("secret",null) ?: return null,Base64.NO_WRAP)
        val iv=Base64.decode(preferences.getString("iv",null) ?: return null,Base64.NO_WRAP)
        val token=Cipher.getInstance("AES/GCM/NoPadding").apply {init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,iv))}.doFinal(encrypted).toString(Charsets.UTF_8)
        EnhancementConnection(EnhancementEndpoint.normalize(endpoint),token)
    }.getOrNull()
    fun save(connection:EnhancementConnection) {
        require(connection.token.length in 32..256)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply {init(Cipher.ENCRYPT_MODE,key())}
        val secret=cipher.doFinal(connection.token.toByteArray(Charsets.UTF_8))
        check(preferences.edit().putString("endpoint",EnhancementEndpoint.normalize(connection.endpoint)).putString("secret",Base64.encodeToString(secret,Base64.NO_WRAP)).putString("iv",Base64.encodeToString(cipher.iv,Base64.NO_WRAP)).commit())
    }
    fun clear() {preferences.edit().clear().apply()}
}
