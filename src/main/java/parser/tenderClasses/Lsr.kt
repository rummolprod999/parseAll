package parser.tenderClasses

import java.util.*

data class Lsr(val purNum: String, val hrefT: String, val hrefL: String, val purName: String, var pubDate: Date, val endDate: Date, var status: String, var placingWayName: String, var nameCus: String) {
}