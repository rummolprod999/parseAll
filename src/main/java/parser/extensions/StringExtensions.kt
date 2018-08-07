package parser.extensions

import java.text.Format
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

fun String.getDataFromRegexp(reg: String): String {
    var st = ""
    try {
        val pattern: Pattern = Pattern.compile(reg)
        val matcher: Matcher = pattern.matcher(this)
        if (matcher.find()) {
            st = matcher.group(1)
        }
    } catch (e: Exception) {
    }
    return st.trim { it <= ' ' }
}

fun String.deleteDoubleWhiteSpace(): String {
    val pattern: Pattern = Pattern.compile("""\s+""")
    val matcher: Matcher = pattern.matcher(this)
    var ss: String = matcher.replaceAll(" ")
    ss = ss.trim { it <= ' ' }
    return ss
}

fun String.getDateFromString(format: Format): Date {
    var d = Date(0L)
    if (this == "") return d
    try {
        d = format.parseObject(this) as Date
    } catch (e: Exception) {
    }

    return d
}

fun String.extractNum(): String {
    var nm = ""
    try {
        val pattern: Pattern = Pattern.compile("\\s+")
        val matcher: Matcher = pattern.matcher(this)
        val ss = matcher.replaceAll("")
        val p = Pattern.compile("""(\d+\.*\d*)""")
        val m = p.matcher(ss)
        if (m.find()) {
            nm = m.group()
        }
    } catch (e: Exception) {
    }
    return nm
}


fun String.extractPrice(): String {
    var nm = ""
    try {
        val pattern: Pattern = Pattern.compile("\\s+")
        val tt = this.replace(',', '.')
        val matcher: Matcher = pattern.matcher(tt)
        val ss = matcher.replaceAll("")
        val p = Pattern.compile("""(\d+\.*\d*)""")
        val m = p.matcher(ss)
        if (m.find()) {
            nm = m.group(1)
        }
    } catch (e: Exception) {
    }
    return nm
}

fun String.tryParseInt(): Boolean {
    return try {
        Integer.parseInt(this)
        true
    } catch (e: NumberFormatException) {
        false
    }

}

fun String.getDateFromFormatOffset(format: SimpleDateFormat, offset: String): Date {
    var d = Date(0L)
    try {
        format.timeZone = TimeZone.getTimeZone(offset)
        d = format.parseObject(this) as Date
    } catch (e: Exception) {
    }

    return d
}

fun String.getGroupFromRegexp(reg: String): String {
    var st = ""
    try {
        val pattern: Pattern = Pattern.compile(reg)
        val matcher: Matcher = pattern.matcher(this)
        if (matcher.find()) {
            st = matcher.group(1)
        }
    } catch (e: Exception) {
    }
    return st.trim { it <= ' ' }
}
