package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Achi
import parser.tenders.TenderAorti
import parser.tools.formatterOnlyDate
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class ParserAorti : IParser, ParserAbstract() {
    val url = "https://www.aorti.ru/purchases/tenders/?PAGEN_1="
    override fun parser() = parse {
        (1..10).forEach { parserAorti("$url$it") }
    }

    private fun parserAorti(url: String) {
        val pageTen = downloadFromUrl(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("div.panel.panel-custom")
        tenders.forEach {
            try {
                parsingTender(it)
            } catch (e: Exception) {
                logger("Error in parserOrPnz function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTender(e: Element) {
        val purName = e.selectFirst("a.collapsed")?.ownText()?.trim { it <= ' ' }
                ?: run { logger("purName not found"); return }
        val urlTender = e.selectFirst("a:contains(Подробнее)")?.attr("href")?.trim { it <= ' ' }
                ?: run { logger("urlTender not found on $purName"); return }
        val href = "https://www.aorti.ru$urlTender"
        val purNum = href.getDataFromRegexp("""/(\d+)/$""")
        val pubDateT = e.selectFirst("span.main-color")?.ownText()?.trim { it <= ' ' }
                ?: run { logger("pubDateT not found"); return }
        var datePub = LocalDate.now()
        if (pubDateT.contains("вчера")) {
            datePub = datePub.minusDays(1)
        } else if (!pubDateT.contains("сегодня")) {
            datePub = pubDateT.getDateFromString(formatterOnlyDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        }
        val tt = Achi(href, purNum, purName)
        val t = TenderAorti(tt, Date.from(datePub.atStartOfDay(ZoneId.systemDefault()).toInstant()))
        ParserTender(t)
    }
}