package parser.tenderClasses

import java.util.*

data class Akbars(
    val purNum: String,
    val href: String,
    val purName: String,
    var pubDate: Date,
    val endDate: Date,
    val nmck: String,
    val delivTerm: String,
    val attachHref: String,
) {}
