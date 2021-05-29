package parser.tenderClasses

import java.util.*

data class Aomsz(
    val purNum: String,
    val href: String,
    val purName: String,
    var pubDate: Date,
    val endDate: Date,
    val status: String
)