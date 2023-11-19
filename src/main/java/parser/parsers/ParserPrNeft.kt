package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDateFromString
import parser.extensions.md5
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.PrNeft
import parser.tenders.TenderPrNeft
import parser.tools.formatterOnlyDate

class ParserPrNeft : IParser, ParserAbstract() {
    companion object WebCl {
        const val BaseUrl = "https://www.prneft.ru/tendery/obyavlennyie-tenderyi/"
    }

    override fun parser() = parse { parserPrNeft() }

    fun parserPrNeft() {
        val pageTen = downloadFromUrl(BaseUrl)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", BaseUrl)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        htmlTen.select("table.table tr")?.drop(1)?.forEach {
            try {
                parserTender(it)
            } catch (e: Exception) {
                logger("error in  ${this::class.simpleName}", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(el: Element) {
        val purName =
            el.selectFirst("td:eq(1)")?.text()?.trim { it <= ' ' }
                ?: run {
                    logger("purName was not found", el.text())
                    throw Exception("purName was not found")
                }
        var purNum = el.selectFirst("td:eq(0)")?.text()?.trim { it <= ' ' } ?: ""
        if (purNum == "") {
            purNum = purName.md5()
        }
        var href = el.selectFirst("td:eq(1)")?.attr("href") ?: throw Exception("href was not found")
        href = "http://www.prneft.ru$href"
        var endDateT = el.selectFirst("td:eq(4)")?.text()?.trim { it <= ' ' } ?: "BAD"
        if (endDateT == "") {
            endDateT = "BAD"
        }
        val dateEnd = endDateT.getDateFromString(formatterOnlyDate)
        val pubDateT =
            el.selectFirst("td:eq(2)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("pubDateT not found")
                    return
                }
        val datePub = pubDateT.getDateFromString(formatterOnlyDate)
        val attachments = mutableMapOf<String, String>()
        el.select("td:eq(5) a").forEach {
            val urlAtt = "http://www.prneft.ru${it.attr("href")}"
            val attName = it.text()
            if (attName != "") {
                attachments[attName] = urlAtt
            }
        }
        val tn = PrNeft(purNum, href, purName, datePub, dateEnd, attachments)
        val t = TenderPrNeft(tn)
        ParserTender(t)
    }
}
