package parser.tenderClasses

import java.util.*

data class Oilb2b(
    val purNum: String,
    val href: String,
    val purName: String,
    val status: String,
    var cusName: String,
    var pubDate: Date,
    var endDate: Date,
    var tenderDate: String,
    val endTenderDate: String,
    var products: MutableList<Oilb2bProduct>,
    var attachments: MutableList<AttachOilb2b>,
)

data class Oilb2bProduct(
    val prodName: String,
    val quant: String,
    val okei: String,
    val extDef: String,
)

data class AttachOilb2b(
    val Url: String,
    val Name: String,
)
