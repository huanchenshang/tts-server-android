package com.github.jing332.common.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.ln
import kotlin.math.pow

object StringUtils {
    const val WARNING_EMOJI = """⚠️"""

    private val silentPattern by lazy { Pattern.compile("[\\s\\p{C}\\p{P}\\p{Z}\\p{S}]") }
    private val splitSentencesRegex by lazy { Pattern.compile("[。？?！!;；]") }

    fun formattedDate(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    /**
     *  是否为不发音的字符串
     */
    fun isSilent(s: String): Boolean {
        return silentPattern.matcher(s).replaceAll("").isEmpty()
    }

    /**
     * 分割长句并保留分隔符
     */
    fun splitSentences(s: String): List<String> {
        val m = splitSentencesRegex.matcher(s)
        val list = splitSentencesRegex.split(s)
        //保留分隔符
        if (list.isNotEmpty()) {
            var count = 0
            while (count < list.size) {
                if (m.find()) {
                    list[count] += m.group()
                }
                count++
            }
        }

        return list.filter { it.replace("”", "").isNotBlank() }
    }

    /**
     * 限制字符串长度
     */
    fun String.limitLength(maxLength: Int = 20, suffix: String = ""): String {
        return if (length >= maxLength)
            substring(0, maxLength) + suffix
        else this
    }

}

/**
 * json字符串头尾加[ ]
 */
fun String.toJsonListString(): String {
    var s = this.trim().removeSuffix(",")
    if (!startsWith("[")) s = "[$s"
    if (!endsWith("]")) s += "]"
    return s
}


/**
 * 字符串中汉字数量
 */
fun String.lengthOfChinese(): Int {
    var count = 0
    val c: CharArray = toCharArray()
    for (i in c.indices) {
        val len = Integer.toBinaryString(c[i].code)
        if (len.length > 8) count++
    }
    return count
}

/**
 * 转为html粗体标签
 */
fun String.toHtmlBold(): String {
    return "<b>$this</b>"
}

fun String.toHtmlItalic(): String {
    return "<i>$this</i>"
}

fun String.toHtmlSmall(): String {
    return "<small>$this</small>"
}

fun String.appendHtmlBr(count: Int = 1): String {
    val strBuilder = StringBuilder(this)
    for (i in (1..count)) {
        strBuilder.append("<br>")
    }
    return strBuilder.toString()
}

fun String.toNumberInt(): Int {
    return this.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
}

fun String.bytesToReadable(bytes: Long): String {
    val kb = 1024
    val mb = kb * 1024
    val gb = mb * 1024

    return when {
        bytes < mb -> "${"%.2f".format(bytes.toDouble() / kb)} KB"
        bytes < gb -> "${"%.2f".format(bytes.toDouble() / mb)} MB"
        else -> "$bytes B"
    }
}

/**
 * @return first char upper case
 */
fun String.firstCharUpperCase(): String {
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

/**
 *  Input 3166 alpha-2 [Locale.getCountry]
 */
fun String.toCountryFlagEmoji(): String {
    if (this.length != 2) {
        return this
    }
    if (!this[0].isLetter() || !this[1].isLetter()) {
        return this
    }
    val firstLetter = Character.codePointAt(this.uppercase(), 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(this.uppercase(), 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
}

fun String.limitLength(max: Int): String {
    return if (this.length > max) this.substring(0, max) else this
}

fun String.fromCookie(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val cookies = this.split(";")

    for (cookie in cookies) {
        val parts = cookie.split("=")
        if (parts.size == 2) {
            map[parts[0].trim()] = parts[1].trim()
        }
        if (parts.size == 1) {
            map[parts[0].trim()] = ""
        }
    }

    return map
}


fun Int.sizeToReadable(locale: Locale = Locale.getDefault()): String =
    this.toLong().sizeToReadable(locale)

fun Long.sizeToReadable(locale: Locale = Locale.getDefault()): String {
    val bytes = this
    val unit = 1024
    if (bytes < unit) return "$bytes B"
    val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
    val pre = "KMGTPE"[exp - 1] + "i"
    return String.format(locale, "%.1f %sB", bytes / unit.toDouble().pow(exp.toDouble()), pre)
}