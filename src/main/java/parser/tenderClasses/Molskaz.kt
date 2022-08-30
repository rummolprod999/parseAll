package parser.tenderClasses

import java.util.*

data class Molskaz(
    val purNum: String,
    val href: String,
    val purName: String,
    var pubDate: Date,
    val endDate: Date,
    val biddingDate: Date,
    val pwName: String,
    val currency: String,
    val delivTerm: String,
    val delivPlace: String,
    val notice: String,
)
