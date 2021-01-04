package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.AttachDmtu
import parser.tenderClasses.Dmtu
import parser.tenders.TenderDmtu
import parser.tools.formatterOnlyDate

class ParserDmtu : IParser, ParserAbstract() {
    val url = "https://anomtu.ru/zakupki/"
    override fun parser() = parse { parserDmtu() }
    private fun parserDmtu() {
        val pageTen = downloadFromUrl(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("div.table_wrapp > div.row_td:gt(0)")
        tenders.forEach {
            try {
                parsingTender(it)
            } catch (e: Exception) {
                logger("Error in parserDmtu function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTender(e: Element) {
        val purName = e.selectFirst("div:eq(0)")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("purName not found"); return }
        val purNum = e.selectFirst("div.number")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("purNum not found"); return }
        if (purNum == "") return
        val pwName = e.selectFirst("div:eq(2)")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("purName not found"); return }
        val urlTender = e.selectFirst("div:eq(6) a")?.attr("href")?.trim { it <= ' ' }
            ?: run { logger("urlTender not found on $purName"); return }
        val status = e.selectFirst("div:eq(5)")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("status not found"); return }
        val pubDateT = e.selectFirst("div.pod_zaya")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("pubDateT not found"); return }
        val datePub = pubDateT.getDateFromString(formatterOnlyDate)
        val endDateT = e.selectFirst("div:eq(4)")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("endDateT not found"); return }
        val dateEnd = endDateT.getDateFromString(formatterOnlyDate)
        val attachmentsUrl = e.select("div:eq(6) a")
        val attachents = mutableListOf<AttachDmtu>()
        attachmentsUrl.forEach { element ->
            val url = element.attr("href")?.trim { it <= ' ' } ?: return@forEach
            val name = element.text() ?: return@forEach
            attachents.add(AttachDmtu(url, name))
        }
        val tt = Dmtu(purNum, urlTender, purName, datePub, dateEnd, pwName, attachents, status)
        val t = TenderDmtu(tt)
        ParserTender(t)
    }
}