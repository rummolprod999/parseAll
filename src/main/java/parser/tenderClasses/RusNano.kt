package parser.tenderClasses

import java.util.*

data class RusNano(
    val purNum: String,
    val href: String,
    val purName: String,
    val pubDate: Date,
    val endDate: Date,
    val pwName: String,
    val orgName: String
)
