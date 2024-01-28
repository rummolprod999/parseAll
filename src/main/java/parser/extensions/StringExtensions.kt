package parser.extensions

import org.apache.commons.codec.digest.DigestUtils
import java.text.Format
import java.text.SimpleDateFormat
import java.time.ZoneId
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
    } catch (e: Exception) {}
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

fun String.getDateFromString(format: Format, datePub: Date = Date(0L)): Date {
    var d = Date(0L)
    if (this == "") return d
    try {
        d = format.parseObject(this) as Date
    } catch (_: Exception) {}
    if (datePub != Date(0L)) {
        d = Date.from(datePub.toInstant().atZone(ZoneId.systemDefault()).plusDays(2).toInstant())
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
    } catch (e: Exception) {}
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
    } catch (e: Exception) {}
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
    } catch (e: Exception) {}

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
    } catch (e: Exception) {}
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

fun String.replaceDateBoretsEnd(): String {
    when {
        this.contains("января") -> return this.replace("января", ".01.")
        this.contains("февраля") -> return this.replace("февраля", ".02.")
        this.contains("марта") -> return this.replace("марта", ".03.")
        this.contains("апреля") -> return this.replace("апреля", ".04.")
        this.contains("мая") -> return this.replace("мая", ".05.")
        this.contains("июня") -> return this.replace("июня", ".06.")
        this.contains("июля") -> return this.replace("июля", ".07.")
        this.contains("августа") -> return this.replace("августа", ".08.")
        this.contains("сентября") -> return this.replace("сентября", ".09.")
        this.contains("октября") -> return this.replace("октября", ".10.")
        this.contains("ноября") -> return this.replace("ноября", ".11.")
        this.contains("декабря") -> return this.replace("декабря", ".12.")
    }
    return this
}

fun String.replaceDateBorets(): String {
    when {
        this.contains("январь") -> return this.replace("январь", "01")
        this.contains("февраль") -> return this.replace("февраль", "02")
        this.contains("март") -> return this.replace("март", "03")
        this.contains("апрель") -> return this.replace("апрель", "04")
        this.contains("май") -> return this.replace("май", "05")
        this.contains("июнь") -> return this.replace("июнь", "06")
        this.contains("июль") -> return this.replace("июль", "07")
        this.contains("август") -> return this.replace("август", "08")
        this.contains("сентябрь") -> return this.replace("сентябрь", "09")
        this.contains("октябрь") -> return this.replace("октябрь", "10")
        this.contains("ноябрь") -> return this.replace("ноябрь", "11")
        this.contains("декабрь") -> return this.replace("декабрь", "12")
    }
    return this
}

fun String.md5(): String {
    return DigestUtils.md5Hex(this).toUpperCase()
}
