package me.stageguard.obms


public sealed class RefactoredException(
    public val code: String,
    override val cause: Throwable? = null
) : IllegalStateException(cause) {
    override val message: String?
        get() = code

    public abstract val outgoingMessage: String

    public fun suppress(throwable: Throwable): RefactoredException = this.also { addSuppressed(throwable) }
}

// unhandled
public class UnhandledException(override val cause: Throwable? = null) : RefactoredException("UNHANDLED_EXCEPTION") {
    override val message: String
        get() = "Unhandled exception: $cause"
    override val outgoingMessage: String
        get() = "发生了未知错误：$cause"
}

// osu!api related exception
/**
 * timeout when requesting osu!api.
 */
public class ApiRequestTimeoutException(public val url: String) : RefactoredException("API_REQUEST_TIMEOUT") {
    override val message: String
        get() = "Request osu!api timeout for url $url"
    override val outgoingMessage: String
        get() = "请求 osu!api 超时，请重试。"
}

/**
 * can't deserialize response json text.
 */
public class BadResponseException(public val url: String, public val respondText: String) :
    RefactoredException("API_BAD_RESPONSE") {
    override val message: String
        get() = "Bad respond for requesting $url, respond text: $respondText"
    override val outgoingMessage: String
        get() = "请求 osu!api 时出现错误，请重试。"
}

/**
 * query specific beatmap score but returned an empty result.
 */
public class BeatmapScoreEmptyException(public val bid: Int) : RefactoredException("SCORE_EMPTY") {
    override val outgoingMessage: String
        get() = "查询谱面 $bid 分数的结果为空，你并未游玩过这张谱面。"
}

/**
 * replay is not available.
 */
public class ReplayNotAvailable(public val scoreId: Long) : RefactoredException("REPLAY_NOT_AVAILABLE") {
    override val outgoingMessage: String
        get() = "无法获取回放，可能该谱面分数的全球排名大于 1000。"
}

/**
 * replay is not available.
 */
public class UnsupportedReplayFormatException(public val scoreId: Long, public val format: String) :
    RefactoredException("UNSUPPORTED_REPLAY_FORMAT") {
    override val message: String
        get() = "Unsupported replay compressing of score $scoreId: $format."
    override val outgoingMessage: String
        get() = "无法获取回放，未知的压缩格式。"
}

/*
 * query user scores but returned an empty result.
 */
public class UserScoreEmptyException(public val qq: Long) : RefactoredException("USER_SCORE_EMPTY") {
    override val outgoingMessage: String
        get() = "查询最近成绩的结果为空，你未在 24h 内连线游玩过 osu!std 的模式的 ranked 谱面。"
}

/**
 * online image doesn't have eTag header.
 * this exception is processed internally.
 */
public class ImageMissingETagException(public val url: String) : RefactoredException("IMAGE_MISSING_ETAG") {
    override val message: String
        get() = "Online image is missing eTag, url: $url"
    override val outgoingMessage: String
        get() = String()
}

/**
 * invalid verify link
 */
public class InvalidVerifyLinkException(public val token: String) : RefactoredException("INVALID_VERIFY_LINK") {
    override val message: String
        get() = "Invalid verify token: $token"
    override val outgoingMessage: String
        get() = "无效的认证链接，可能该链接已被处理。"
}

// function related
/**
 * User doesn't bind his account.
 */
public class NotBindException(public val qq: Long) : RefactoredException("NOT_BIND") {
    override val outgoingMessage: String
        get() = "用户 $qq 未绑定账号，请输入 .bind 进行绑定（无需输入你的 osu!Id）。"
}

/*
 * authorization failed or invalid authorization info
 */
public class InvalidTokenException(public val qq: Long) : RefactoredException("INVALID_TOKEN") {
    override val message: String
        get() = "OAuth token of user $qq is invalid."
    override val outgoingMessage: String
        get() = "用户 $qq 的绑定已失效，请输入 .bind 或通知对方重新绑定以更新令牌。"
}

/**
 * can't read image file.
 * this exception is processed internally.
 */
public class ImageReadException(public val path: String) : RefactoredException("IMAGE_READ") {
    override val message: String
        get() = "Cannot read image $path"
    override val outgoingMessage: String
        get() = String()
}

/**
 * invalid input from qq message
 */
public class InvalidInputException(public val input: String) : RefactoredException("INVALID_INPUT") {
    override val outgoingMessage: String
        get() = "输入格式有误，请检查指令。"
}

// processor related
/**
 * can't process beatmap file.
 */
public class BeatmapParseException(public val bid: Int) : RefactoredException("BEATMAP_PARSE") {
    override val message: String
        get() = "Cannot parse beatmap $bid."
    override val outgoingMessage: String
        get() = "无法解析谱面 $bid。"
}

/**
 * can't process replay file.
 */
public class ReplayParseException(public val scoreId: Long) : RefactoredException("REPLAY_PARSE") {
    override val message: String
        get() = "Cannot parse replay of score $scoreId."
    override val outgoingMessage: String
        get() = "无法解析成绩 $scoreId 的回放。"
}