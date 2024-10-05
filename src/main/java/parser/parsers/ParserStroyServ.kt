package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadWaitWithRef
import parser.tenderClasses.StroyServ
import parser.tenders.TenderStroyServ
import parser.tools.formatterOnlyDate
import java.time.ZoneId
import java.util.*

class ParserStroyServ :
    ParserAbstract(),
    IParser {
    val url = "https://stroyservis.com/tenderstorgs/ads?_p="

    override fun parser() =
        parse {
            System.setProperty("jsse.enableSNIExtension", "false")
            (1..3).forEach { parserStroyServ("$url$it&") }
        }

    private fun parserStroyServ(url: String) {
        val pageTen = downloadWaitWithRef(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("a.d_bid-table")
        tenders.forEach {
            try {
                parsingTender(it)
            } catch (e: Exception) {
                logger("Error in parserBorets function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTender(e: Element) {
        val purName =
            e.selectFirst("p:eq(2)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }

        val urlT =
            e.attr("href")?.trim { it <= ' ' }
                ?: run {
                    logger("urlT not found on $purName")
                    return
                }
        val urlTend = "https://stroyservis.com$urlT"
        val purNum = urlT.getDataFromRegexp("""id=(\d+)""")
        if (purNum == "") {
            logger("purNum not found on $purName")
            return
        }
        val delivTerm =
            e.selectFirst("td:contains(Описание:) + td")?.ownText()?.trim { it <= ' ' } ?: ""
        val datePub = Date()
        val dateEndT =
            e.selectFirst("td:contains(Заявки принимаются до:) + td")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("datePubT not found")
                    return
                }
        var dateEnd = dateEndT.getDateFromString(formatterOnlyDate)
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
        val cusName = e.selectFirst("p:eq(1)")?.ownText()?.trim { it <= ' ' } ?: ""
        val tn = StroyServ(purNum, urlTend, purName, datePub, dateEnd, cusName, delivTerm)
        val t = TenderStroyServ(tn)
        ParserTender(t)
    }
}
