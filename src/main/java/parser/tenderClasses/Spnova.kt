package parser.tenderClasses

import java.util.*

data class Spnova(
    val purNum: String,
    val href: String,
    val purName: String,
    val endDate: Date,
    val placingWayName: String,
    val pubDate: Date,
    val nmck: String,
    val doc: String
) {}
