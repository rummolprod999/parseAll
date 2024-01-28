package parser.tenderClasses

import java.util.*

data class PrNeft(
    val purNum: String,
    val href: String,
    val purName: String,
    var pubDate: Date,
    val endDate: Date,
    val attachments: Map<String, String>,
)
