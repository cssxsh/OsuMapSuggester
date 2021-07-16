package me.stageguard.obms.database.model

import me.stageguard.obms.database.Database
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column


object OsuUserInfo : IntIdTable("users") {
    val osuId: Column<Int> = integer("osuId").uniqueIndex()
    val osuName: Column<String> = varchar("osuName", 16)
    val qq: Column<Long> = long("qq")
    val token: Column<String> = varchar("token", 1500)
    val tokenExpireUnixSecond: Column<Long> = long("tokenExpiresUnixSecond")
    val refreshToken: Column<String> = varchar("refreshToken", 1500)
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(OsuUserInfo)
    var osuId by OsuUserInfo.osuId
    var osuName by OsuUserInfo.osuName
    var qq by OsuUserInfo.qq
    var token by OsuUserInfo.token
    var tokenExpireUnixSecond by OsuUserInfo.tokenExpireUnixSecond
    var refreshToken by OsuUserInfo.refreshToken
}

fun User.Companion.findByQQ(qq: Long) = Database.query {
    User.find { OsuUserInfo.qq eq qq }.single()
}