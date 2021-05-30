package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrlNoSsl
import parser.tenderClasses.Aomsz
import parser.tenders.TenderRusFish
import parser.tools.formatterOnlyDate
import java.time.ZoneId
import java.util.*

class ParserRusFish : IParser, ParserAbstract() {

    val url = "https://russianfishery.ru/tenders/"
    override fun parser() = parse {
        System.setProperty("jsse.enableSNIExtension", "false")
        parserRusfish("$url")
    }

    private fun parserRusfish(url: String) {
        val pageTen = downloadFromUrlNoSsl(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("div.tenderlist_item")
        tenders.forEach {
            try {
                parsingTender(it)
            } catch (e: Exception) {
                logger("Error in parserKurganKhim function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTender(e: Element) {
        val purName = e.selectFirst("span")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("purName not found"); return }
        val purNum = e.attr("rel")?.trim { it <= ' ' } ?: run { logger("purNum not found"); return }
        val href = "https://russianfishery.ru/tenders/item.php?page=$purNum"
        val status = e.selectFirst("div.tender_active")?.ownText()?.trim { it <= ' ' }
            ?: ""
        val pubDateT = e.selectFirst("div.date_tag")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("pubDateT not found"); return }
        var datePub = pubDateT.getDateFromString(formatterOnlyDate)
        if (datePub == Date(0L)) {
            datePub = Date()
        }
        val dateEnd = Date.from(datePub.toInstant().atZone(ZoneId.systemDefault()).plusDays(2).toInstant())
        val tn = Aomsz(purNum, href, purName, datePub, dateEnd, status)
        val t = TenderRusFish(tn)
        ParserTender(t)
    }
}