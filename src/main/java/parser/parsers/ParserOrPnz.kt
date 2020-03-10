package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDateFromString
import parser.extensions.md5
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.OrPnz
import parser.tenders.TenderOrPnz
import parser.tools.formatterOnlyDate

class ParserOrPnz : IParser, ParserAbstract() {
    val url = "https://www.ornpz.ru/tenderyi/predlozheniya/"
    override fun parser() = parse { parserOrPnz() }
    private fun parserOrPnz() {
        val pageTen = downloadFromUrl(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("table.table-tender tbody tr:gt(0)")
        tenders.forEach {
            try {
                parsingTender(it)
            } catch (e: Exception) {
                logger("Error in parserOrPnz function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTender(e: Element) {
        val purName = e.selectFirst("td:eq(2) div")?.ownText()?.trim { it <= ' ' }
                ?: run { logger("purName not found"); return }
        val purNum = purName.md5()
        if (purNum == "") return
        val urlTender = e.selectFirst("td:eq(3) div a")?.attr("href")?.trim { it <= ' ' }
                ?: run { logger("urlTender not found on $purName"); return }
        val href = "https://www.ornpz.ru//$urlTender"
        val pubDateT = e.selectFirst("td:eq(0) div")?.ownText()?.trim { it <= ' ' }
                ?: run { logger("pubDateT not found"); return }
        val datePub = pubDateT.getDateFromString(formatterOnlyDate)
        val endDateT = e.selectFirst("td:eq(1) div")?.ownText()?.trim { it <= ' ' }
                ?: run { logger("endDateT not found"); return }
        val dateEnd = endDateT.getDateFromString(formatterOnlyDate)
        val tt = OrPnz(purNum, href, purName, datePub, dateEnd)
        val t = TenderOrPnz(tt)
        ParserTender(t)
    }
}