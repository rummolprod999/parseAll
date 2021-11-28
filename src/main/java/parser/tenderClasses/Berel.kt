package parser.tenderClasses

import java.util.*

data class Berel(
    val purNum: String,
    val href: String,
    val purName: String,
    var pubDate: Date,
    val endDate: Date
)
