package parser.tenderClasses

import java.util.*

data class Borets(
    val purNum: String,
    val href: String,
    val purName: String,
    var pubDate: Date,
    val endDate: Date,
    val cusName: String,
    val delivTerm: String,
)
