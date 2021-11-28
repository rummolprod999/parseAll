package parser.extensions

import java.util.*

fun Date.dateAddHours(h: Int): Date {
    val cal = Calendar.getInstance()
    cal.time = this
    cal.add(Calendar.HOUR_OF_DAY, h)
    return cal.time
}
