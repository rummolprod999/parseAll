package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.RenCredit
import parser.tenders.TenderRenCredit
import parser.tools.formatter
import parser.tools.formatterOnlyDate

class ParserRenCredit : IParser, ParserAbstract() {
    val url = "https://rencredit.ru/about/tenders/"
    override fun parser() = parse { parserRenCredit() }
    private fun parserRenCredit() {
        val pageTen = downloadFromUrl(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("table.tenders-table tbody tr")
        tenders.forEach {
            try {
                parsingTender(it)
            } catch (e: Exception) {
                logger("Error in parserRenCredit function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTender(e: Element) {
        val purName = e.selectFirst("td:eq(1) a")?.ownText()?.trim { it <= ' ' }
                ?: run { logger("purName not found"); return }
        val purNum = e.selectFirst("td:eq(0) span")?.ownText()?.trim { it <= ' ' }
                ?: run { logger("purNum not found"); return }
        if (purNum == "") return
        val urlTender = e.selectFirst("td:eq(1) a")?.attr("href")?.trim { it <= ' ' }
                ?: run { logger("urlTender not found on $purName"); return }
        val href = "https://rencredit.ru$urlTender"
        val pubDateT = e.selectFirst("td:eq(2)")?.ownText()?.trim { it <= ' ' }
                ?: run { logger("pubDateT not found"); return }
        val datePub = pubDateT.getDateFromString(formatterOnlyDate)
        val endDateT = e.selectFirst("td:eq(3)")?.ownText()?.trim { it <= ' ' }
                ?: run { logger("endDateT not found"); return }
        val dateEnd = endDateT.getDateFromString(formatter)
        val tt = RenCredit(purNum, href, purName, datePub, dateEnd)
        val t = TenderRenCredit(tt)
        ParserTender(t)
    }


}