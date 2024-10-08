package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.extractPrice
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Spnova
import parser.tenders.TenderSpnova
import parser.tools.formatter
import java.time.ZoneId
import java.util.*

class ParserSpnova :
    ParserAbstract(),
    IParser {
    companion object WebCl {
        const val CountPage = 11
    }

    override fun parser() =
        parse {
            try {
                parserSpnova()
            } catch (e: Exception) {
                logger("Error in ${this::class.simpleName} function", e.stackTrace, e)
            }
        }

    private fun parserSpnova() {
        (1..CountPage).forEach {
            val url = "https://etp.snpnova.com/procurement-notices/?nav-list=page-$it"
            try {
                parserPageList(url)
            } catch (e: Exception) {
                logger(e, e.stackTrace)
            }
        }
    }

    private fun parserPageList(url: String) {
        val pageTen = downloadFromUrl(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val html = Jsoup.parse(pageTen)
        val tenders = html.select("table tbody tr:contains(Извещение)")
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
        val urlTend =
            el.selectFirst("td:eq(0) a")?.attr("href")?.trim { it <= ' ' }
                ?: run {
                    logger("urlTend not found")
                    return
                }
        val href = "https://etp.snpnova.com$urlTend"
        val urlDoc =
            el.selectFirst("td:eq(10) a")?.attr("href")?.trim { it <= ' ' }
                ?: run {
                    logger("urlDoc not found")
                    return
                }
        val hrefDoc = "https://etp.snpnova.com$urlDoc"
        val purName =
            el.selectFirst("td:eq(3)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }
        val purNumT =
            el.selectFirst("td:eq(0)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purNumT not found")
                    return
                }
        val purNum = purNumT.getDataFromRegexp("""№(ЭТ-\d+)""")
        val pwName = el.selectFirst("td:eq(5)")?.ownText()?.trim { it <= ' ' } ?: ""
        val datePubTmp =
            el.selectFirst("td:eq(6)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("datePubTmp not found")
                    return
                }
        val datePub = datePubTmp.getDateFromString(formatter)

        val dateEndTmp = el.selectFirst("td:eq(7)")?.ownText()?.trim { it <= ' ' } ?: ""
        var dateEnd = dateEndTmp.getDateFromString(formatter)
        if (dateEnd == Date(0L)) {
            dateEnd =
                Date.from(
                    datePub
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .plusDays(2)
                        .toInstant(),
                )
        }

        val nmck =
            el
                .selectFirst("td:eq(4)")
                ?.ownText()
                ?.trim { it <= ' ' }
                ?.extractPrice() ?: ""
        val tt = Spnova(purNum, href, purName, dateEnd, pwName, datePub, nmck, hrefDoc)
        val t = TenderSpnova(tt)
        ParserTender(t)
    }
}
