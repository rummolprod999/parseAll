package parser.tenderClasses

import java.util.*

data class KurganKhim(
    val purNum: String,
    val href: String,
    val purName: String,
    var pubDate: Date,
    val endDate: Date,
    val attachments: Map<String, String>,
    val orgName: String,
)
