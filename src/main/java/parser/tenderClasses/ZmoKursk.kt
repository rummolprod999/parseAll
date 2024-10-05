package parser.tenderClasses

import java.util.*

data class ZmoKursk(
    val status: String,
    var purNum: String,
    val purObj: String,
    val nmck: String,
    val pubDate: Date,
    val endDate: Date,
    val url: String,
)
