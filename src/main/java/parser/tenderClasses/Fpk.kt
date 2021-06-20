package parser.tenderClasses

import java.util.*

data class Fpk(
    val purNum: String,
    val href: String,
    val purName: String,
    val pubDate: Date,
    val endDate: Date,
    val pwName: String,
    val orgName: String,
    val cusName: String
)