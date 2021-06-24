package parser.tenderClasses

import java.util.*

data class Gns(
    val purNum: String,
    val href: String,
    val purName: String,
    var pubDate: Date,
    val endDate: Date,
    val status: String,
    val region: String,
    val cusName: String,
    val biddingDate: Date,
)
