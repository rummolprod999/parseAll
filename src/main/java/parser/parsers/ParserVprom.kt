package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Vprom
import parser.tenders.TenderVprom
import parser.tools.formatterOnlyDate
import java.time.ZoneId
import java.util.*

class ParserVprom : IParser, ParserAbstract() {

    override fun parser() = parse {
        try {
            parserVprom()
        } catch (e: Exception) {
            logger("Error in ${this::class.simpleName} function", e.stackTrace, e)
        }
    }

    private fun parserVprom() {
        val url = "https://voltyre-prom.ru/cooperation/tenders/"
        try {
            parserPageList(url)
        } catch (e: Exception) {
            logger(e, e.stackTrace)
        }
    }

    private fun parserPageList(url: String) {
        val pageTen = downloadFromUrl(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val html = Jsoup.parse(pageTen)
        val tenders = html.select("div.s-tender")
        if (tenders.isEmpty()) {
            logger("Gets empty list tenders", url)
        }
        tenders.forEach {
            try {
                parserTend(it)
            } catch (e: Exception) {
                logger(e, e.stackTrace)
            }
        }
    }

    private fun parserTend(el: Element) {
        var urlTend =
            el.selectFirst("div.s-tender__name a")?.attr("href")?.trim { it <= ' ' }
                ?: run {
                    logger("urlTend not found")
                    return
                }
        urlTend = "https://voltyre-prom.ru${urlTend}"
        val purName =
            el.selectFirst("div.s-tender__name a")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }
        val purNum = purName.getDataFromRegexp("""№(.+?)\s""")
        val datePubTmp =
            el.selectFirst("span:contains(Дата размещения:) + span")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("datePubTmp not found")
                    return
                }
        val datePub = datePubTmp.getDateFromString(formatterOnlyDate)
        val dateEndTmp =
            el.selectFirst("span:contains(Дата окончания приема заявок:) + span")?.ownText()?.trim {
                it <= ' '
            }
                ?: ""
        var dateEnd = dateEndTmp.getDateFromString(formatterOnlyDate)
        if (dateEnd == Date(0L)) {
            dateEnd =
                Date.from(
                    datePub.toInstant().atZone(ZoneId.systemDefault()).plusDays(2).toInstant()
                )
        }
        val tt = Vprom(purNum, urlTend, purName, dateEnd, datePub)
        val t = TenderVprom(tt)
        ParserTender(t)
    }
}
