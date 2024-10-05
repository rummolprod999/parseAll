package parser.tenderClasses

import java.util.*

data class Psfond(
    val purNum: String,
    val href: String,
    val purName: String,
    val pubDate: Date,
    val endDate: Date,
    val delivPlace: String,
    val attachments: List<AttachPsfond>,
    val status: String,
)

data class AttachPsfond(
    val Url: String,
    val Name: String,
)
