package parser.tenderClasses

import java.util.*

data class ZakazRf(
    val purNum: String,
    val href: String,
    val purName: String,
    val pubDate: Date,
    val endDate: Date,
    val okei: String,
    val price: String,
    val quantity: String,
    val sum: String,
    val orgName: String,
    val region: String,
    val delivPlace: String,
    val status: String
) {
}