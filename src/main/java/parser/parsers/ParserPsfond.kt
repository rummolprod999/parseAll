package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.extensions.md5
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.AttachPsfond
import parser.tenderClasses.Psfond
import parser.tenders.TenderPsfond
import parser.tools.formatterOnlyDate

class ParserPsfond : IParser, ParserAbstract() {
    val url = "https://psfond.ru/about/tenders/"

    override fun parser() = parse { parserPsfond() }

    private fun parserPsfond() {
        val pageTen = downloadFromUrl(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("div.table  div.row")
        tenders.drop(1).forEach {
            try {
                parsingTender(it)
            } catch (e: Exception) {
                logger("Error in parserDmtu function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTender(e: Element) {
        val purName =
            e.selectFirst("div:eq(0)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }

        val status =
            e.selectFirst("div:eq(3)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("status not found")
                    return
                }
        val dates =
            e.selectFirst("div.item.col3")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("dates not found")
                    return
                }
        val purNum = (dates + purName).md5()
        val pubDateT = dates.getDataFromRegexp("""^(\d{2}\.\d{2}\.\d{4})""")
        val datePub = pubDateT.getDateFromString(formatterOnlyDate)
        val dateEndT = dates.getDataFromRegexp("""(\d{2}\.\d{2}\.\d{4})$""")
        val dateEnd = dateEndT.getDateFromString(formatterOnlyDate)
        val attachmentsUrl = e.select("div:eq(4) a")
        val attachents = mutableListOf<AttachPsfond>()
        attachmentsUrl.forEach { element ->
            val url = element.attr("href")?.trim { it <= ' ' } ?: return@forEach
            val name = element.text() ?: return@forEach
            attachents.add(AttachPsfond("https://psfond.ru" + url, name))
        }
        val delivPlace = e.selectFirst("div:eq(1)")?.ownText()?.trim { it <= ' ' } ?: ""
        val tt =
            Psfond(
                purNum,
                "https://psfond.ru/",
                purName,
                datePub,
                dateEnd,
                delivPlace,
                attachents,
                status
            )
        val t = TenderPsfond(tt)
        ParserTender(t)
    }
}
