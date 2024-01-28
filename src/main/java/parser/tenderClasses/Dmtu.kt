package parser.tenderClasses

import java.util.*

data class Dmtu(
    val purNum: String,
    val href: String,
    val purName: String,
    val pubDate: Date,
    val endDate: Date,
    val pwName: String,
    val attachments: List<AttachDmtu>,
    val status: String
)

data class AttachDmtu(val Url: String, val Name: String)
