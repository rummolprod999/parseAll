package parser.tenderClasses

import java.util.*

data class RusSalt(
    val purNum: String,
    val href: String,
    val purName: String,
    val endDate: Date,
    var pubDate: Date,
    var status: String,
    val delivTerm: String
)
