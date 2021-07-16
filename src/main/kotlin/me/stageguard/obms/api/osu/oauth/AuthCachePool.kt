package me.stageguard.obms.api.osu.oauth

import java.lang.IllegalStateException

object AuthCachePool {
    private val MAPPING = "0123456789abcdef".toByteArray()
    //V: qq account, K: generated token
    private val cache: HashMap<String, Long> = hashMapOf()

    private fun generateNBytes(n: Int) = buildString {
        repeat(n) { append(MAPPING.random()) }
    }

    fun getQQ(token: String) = cache[token] ?: throw IllegalStateException("STATE_TOKEN_NOT_FOUND:$token")

    fun generateToken(qq: Long) = cache.filter {
        it.value == qq
    }.run {
        if(size == 0) {
            generateNBytes(16).also {
                cache[it] = qq
            }
        } else {
            keys.single()
        }
    }

    fun removeTokenCache(token: String) = cache.remove(token)
}