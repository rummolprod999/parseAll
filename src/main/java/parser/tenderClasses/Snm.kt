package parser.tenderClasses

import java.util.*

data class Snm(
    val purNum: String,
    val href: String,
    val purName: String,
    var pubDate: Date,
    var endDate: Date,
    val delivTerm: String,
) {}
