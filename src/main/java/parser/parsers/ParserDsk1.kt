package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrlNoSslNew
import parser.tenderClasses.Dsk1
import parser.tenders.TenderDsk1
import parser.tools.formatterGpn
import parser.tools.formatterOnlyDate
import java.time.ZoneId
import java.util.*

class ParserDsk1 : IParser, ParserAbstract() {
    val url = "https://tender.dsk1.ru/tendery/?PAGEN_1="

    override fun parser() = parse {
        System.setProperty("jsse.enableSNIExtension", "false")
        (1..20).forEach { parserDsk1("$url$it") }
    }

    private fun parserDsk1(url: String) {
        val pageTen = downloadFromUrlNoSslNew(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("table.table-tenders tr[data-href]")
        tenders.forEach {
            try {
                parsingTender(it)
            } catch (e: Exception) {
                logger("Error in parserFpk function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTender(e: Element) {
        val purName =
            e.selectFirst("td:eq(1)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }
        var href =
            e.attr("data-href")?.replace("tendery/", "") ?: throw Exception("href was not found")
        href = "https://tender.dsk1.ru$href"
        val purNum = href.getDataFromRegexp("""dsk1.ru/(\d+)/""")
        if (purNum == "") throw Exception("purNum was not found")
        val pubDateT =
            e.selectFirst("td:eq(3)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("pubDateT not found")
                    return
                }
        var datePub = pubDateT.getDateFromString(formatterGpn)
        if (datePub == Date(0L)) {
            datePub = pubDateT.getDateFromString(formatterOnlyDate)
        }
        if (datePub == Date(0L)) {
            throw Exception("datePub was not found")
        }
        val endDateT = e.selectFirst("td:eq(4)")?.ownText()?.trim { it <= ' ' } ?: ""
        var dateEnd = endDateT.getDateFromString(formatterGpn)
        if (dateEnd == Date(0L)) {
            dateEnd = endDateT.getDateFromString(formatterOnlyDate)
        }
        if (dateEnd == Date(0)) {
            dateEnd =
                Date.from(
                    datePub.toInstant().atZone(ZoneId.systemDefault()).plusDays(2).toInstant()
                )
        }
        val cusName = e.selectFirst("td:eq(5)")?.ownText()?.trim { it <= ' ' } ?: ""
        val tn = Dsk1(purNum, href, purName, datePub, dateEnd, cusName)
        val t = TenderDsk1(tn)
        ParserTender(t)
    }
}
