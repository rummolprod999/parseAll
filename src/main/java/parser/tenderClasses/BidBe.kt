package parser.tenderClasses

import java.util.*

data class BidBe(
    val purNum: String,
    val href: String,
    val purName: String,
    val pubDate: Date,
    val endDate: Date,
    val payMethod: String,
    val delivPlace: String,
    val status: String,
)
