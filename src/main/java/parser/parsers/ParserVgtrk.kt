package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.extractPrice
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Vgtrk
import parser.tenders.TenderVgtrk
import parser.tools.formatter

class ParserVgtrk :
    ParserAbstract(),
    IParser {
    val url = "https://tendering.vgtrk.com/orders/working"

    override fun parser() = parse { parserRusNano() }

    private fun parserRusNano() {
        val pageTen = downloadFromUrl(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("table.zebra tbody tr")
        tenders.forEach {
            try {
                parsingTender(it)
            } catch (e: Exception) {
                logger("Error in parserRusNano function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTender(e: Element) {
        val purName =
            e.selectFirst("td:eq(1) a")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }
        val purNum =
            e.selectFirst("td:eq(0) a")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purNum not found")
                    return
                }
        val urlT =
            e.selectFirst("td:eq(0) a")?.attr("href")?.trim { it <= ' ' }
                ?: run {
                    logger("urlT not found on $purName")
                    return
                }
        val urlTend = "https://tendering.vgtrk.com$urlT"
        val pubDateT =
            e.selectFirst("td:eq(4)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("pubDateT not found")
                    return
                }
        val datePub = pubDateT.getDateFromString(formatter)
        val endDateT =
            e.selectFirst("td:eq(5)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("endDateT not found")
                    return
                }
        val dateEnd = endDateT.getDateFromString(formatter)
        val state = e.selectFirst("td:eq(6)")?.ownText()?.trim { it <= ' ' } ?: ""
        val cusName = e.selectFirst("td:eq(2)")?.ownText()?.trim { it <= ' ' } ?: ""
        val nmckT = e.selectFirst("td:eq(3)")?.ownText()?.trim { it <= ' ' } ?: ""
        val nmck = nmckT.extractPrice()
        val tn =
            Vgtrk(
                cusName = cusName,
                endDate = dateEnd,
                href = urlTend,
                Nmck = nmck,
                pubDate = datePub,
                purName = purName,
                purNum = purNum,
                state = state,
            )
        val t = TenderVgtrk(tn)
        ParserTender(t)
    }
}
