package parser.tenderClasses

import java.util.*

data class UmzMark(
    val purNumT: String,
    val href: String,
    val purName: String,
    val pubDateT: Date,
    val endDate: Date,
    val okpd: String,
    val quant: String,
    val okei: String,
    val nameCus: String,
    val nmck: String,
    val status: String
)
