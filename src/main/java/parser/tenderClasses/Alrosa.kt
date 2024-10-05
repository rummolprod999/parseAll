package parser.tenderClasses

import java.util.*

data class Alrosa(
    val purNum: String,
    val href: String,
    val purName: String,
    var pubDate: Date,
    val endDate: Date,
    var status: String,
    val placingWayName: String,
    val nameCus: String,
    val nmck: String,
    val currency: String,
    val orgName: String,
    val contactPerson: String,
    val phone: String,
    val email: String,
    val products: MutableList<AlrosaProduct>,
)
