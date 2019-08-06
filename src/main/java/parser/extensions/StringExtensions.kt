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

fun String.deleteAllWhiteSpace(): String {
    val pattern: Pattern = Pattern.compile("""\s+""")
    val matcher: Matcher = pattern.matcher(this)
    var ss: String = matcher.replaceAll("")
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

fun String.replaceDateAchi(): String {
    when {
        this.contains("янв") -> return this.replace("янв", "01")
        this.contains("фев") -> return this.replace("фев", "02")
        this.contains("мар") -> return this.replace("мар", "03")
        this.contains("апр") -> return this.replace("апр", "04")
        this.contains("май") -> return this.replace("май", "05")
        this.contains("июн") -> return this.replace("июн", "06")
        this.contains("июл") -> return this.replace("июл", "07")
        this.contains("авг") -> return this.replace("авг", "08")
        this.contains("сен") -> return this.replace("сен", "09")
        this.contains("окт") -> return this.replace("окт", "10")
        this.contains("ноя") -> return this.replace("ноя", "11")
        this.contains("дек") -> return this.replace("дек", "12")
    }
    return this
}
