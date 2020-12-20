package parser.tenderClasses

import java.util.*

data class Vgtrk(
    val purNum: String,
    val href: String,
    val purName: String,
    var pubDate: Date,
    val endDate: Date,
    val state: String,
    val Nmck: String,
    val cusName: String
) {
}