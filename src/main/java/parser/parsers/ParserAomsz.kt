package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrlNoSslNew
import parser.tenderClasses.Aomsz
import parser.tenders.TenderAomsz
import parser.tools.formatter
import parser.tools.formatterOnlyDate
import java.time.ZoneId
import java.util.*

class ParserAomsz :
    ParserAbstract(),
    IParser {
    val url = "https://oaomsz.ru/purchases/requests/"

    override fun parser() =
        parse {
            System.setProperty("jsse.enableSNIExtension", "false")
            parserAomsz("$url")
        }

    private fun parserAomsz(url: String) {
        val pageTen = downloadFromUrlNoSslNew(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("table.list_purchases_uncompleted tr:gt(0)")
        tenders.forEach {
            try {
                parsingTender(it)
            } catch (e: Exception) {
                logger("Error in parserKurganKhim function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTender(e: Element) {
        val purName =
            e.selectFirst("td:eq(1) strong")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }
        val href =
            e.selectFirst("td:eq(5) a")?.attr("href")?.trim { it <= ' ' }
                ?: run {
                    logger("href not found on $purName")
                    return
                }
        val purNum =
            e
                .selectFirst("td:eq(0)")
                ?.ownText()
                ?.replace("№", "")
                ?.trim { it <= ' ' }
                ?: run {
                    logger("purNum not found")
                    return
                }
        val status = e.selectFirst("td:eq(2) div")?.ownText()?.trim { it <= ' ' } ?: ""
        val pubDateT =
            e.selectFirst("td:eq(3) > div:eq(0)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("pubDateT not found")
                    return
                }
        val datePub = pubDateT.getDateFromString(formatterOnlyDate)
        val endDateT =
            e.selectFirst("td:eq(4) > div:eq(0)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("endDateT not found")
                    return
                }
        var dateEnd = endDateT.getDateFromString(formatter)
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
        val tn = Aomsz(purNum, href, purName, datePub, dateEnd, status)
        val t = TenderAomsz(tn)
        ParserTender(t)
    }
}
