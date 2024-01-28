package parser.tenderClasses

import java.util.*

data class Dellin(
    val purNum: String,
    val href: String,
    val purName: String,
    var pubDate: Date,
    val endDate: Date,
    val pwName: String,
    val city: String
)
