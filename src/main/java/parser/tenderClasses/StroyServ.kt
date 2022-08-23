package parser.tenderClasses

import java.util.*

data class StroyServ(
    val purNum: String,
    val href: String,
    val purName: String,
    var pubDate: Date,
    val endDate: Date,
    val cusName: String,
    val delivTerm: String,
)
