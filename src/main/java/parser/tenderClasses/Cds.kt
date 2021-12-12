package parser.tenderClasses

import java.util.*

data class Cds(
    val purNum: String,
    val href: String,
    val purName: String,
    val contact: String,
    var pubDate: Date,
    val endDate: Date,
    val delivTerm: String,
    val delivPlace: String
) {}
