package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrlNoSslNew
import parser.tenderClasses.Fpk
import parser.tenders.TenderFpk
import parser.tools.formatterOnlyDate
import java.time.ZoneId
import java.util.*

class ParserFpk :
    ParserAbstract(),
    IParser {
    val url = "https://fpkinvest.ru/purchase?Purchase_page="

    override fun parser() =
        parse {
            System.setProperty("jsse.enableSNIExtension", "false")
            (1..10).forEach { parserFpk("$url$it") }
        }

    private fun parserFpk(url: String) {
        val pageTen = downloadFromUrlNoSslNew(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("table.purchase-table tbody tr")
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
            e.selectFirst("td:eq(3)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }
        val purNum =
            e.selectFirst("td:eq(0) strong")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purNum not found")
                    return
                }
        val href = "https://fpkinvest.ru/purchase/$purNum"
        val orgName = e.selectFirst("td:eq(9)")?.ownText()?.trim { it <= ' ' } ?: ""
        val cusName = e.selectFirst("td:eq(8)")?.ownText()?.trim { it <= ' ' } ?: ""
        val pubDateT =
            e.selectFirst("td:eq(1)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("pubDateT not found")
                    return
                }
        val datePub = pubDateT.getDateFromString(formatterOnlyDate)
        val endDateT = e.selectFirst("td:eq(2)")?.ownText()?.trim { it <= ' ' } ?: ""
        var dateEnd = endDateT.getDateFromString(formatterOnlyDate)
        if (dateEnd == Date(0)) {
            dateEnd =
                Date.from(
                    datePub
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .plusDays(2)
                        .toInstant(),
                )
        }
        val pwName = e.selectFirst("td:eq(7)")?.ownText()?.trim { it <= ' ' } ?: ""
        val tn = Fpk(purNum, href, purName, datePub, dateEnd, pwName, orgName, cusName)
        val t = TenderFpk(tn)
        ParserTender(t)
    }
}
