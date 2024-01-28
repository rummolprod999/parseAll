package parser.tenderClasses

import java.util.*

data class MmkCoal(
    val purNum: String,
    val href: String,
    val purName: String,
    var pubDate: Date,
    val endDate: Date,
    val attachments: Map<String, String>,
    val noticeVer: String
)
