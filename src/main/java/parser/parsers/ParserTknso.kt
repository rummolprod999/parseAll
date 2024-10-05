package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.deleteDoubleWhiteSpace
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.extensions.md5
import parser.logger.logger
import parser.networkTools.downloadWaitWithRef
import parser.tenderClasses.Tknso
import parser.tenders.TenderTknso
import parser.tools.formatterGpn
import java.time.ZoneId
import java.util.*

class ParserTknso :
    ParserAbstract(),
    IParser {
    override fun parser() =
        parse {
            System.setProperty("jsse.enableSNIExtension", "false")
            parserTknso("http://tknso.ru/tendery.html")
            (2..20).forEach {
                try {
                    parserTknso("http://tknso.ru/tendery/$it.html")
                } catch (e: Exception) {
                    logger(e)
                }
            }
        }

    private fun parserTknso(url: String) {
        val pageTen = downloadWaitWithRef(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("div.media")
        tenders.forEach {
            try {
                parsingTender(it)
            } catch (e: Exception) {
                logger("Error in parserTknso function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTender(e: Element) {
        val purName =
            e.selectFirst("h4.media-heading")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }
        val urlT =
            e.selectFirst("a:contains(Подробнее)")?.attr("href")?.trim { it <= ' ' }
                ?: run {
                    logger("urlT not found on $purName")
                    return
                }
        val urlTend = "http://tknso.ru$urlT"
        val dates =
            e
                .selectFirst("span:contains(Прием заявок:)")
                ?.ownText()
                ?.replace("Прием заявок:", "")
                ?.trim { it <= ' ' }
                ?: run {
                    logger("dates not found")
                    return
                }
        val datePubR =
            dates
                .getDataFromRegexp("""с\s+(\d{2}\.\d{2}\.\d{4}\s\d{2}:\d{2})""")
                .deleteDoubleWhiteSpace()
                .trim()
        val datePub = datePubR.getDateFromString(formatterGpn)
        if (datePub == Date(0L)) {
            run {
                logger("datePub was not found", urlTend)
                throw Exception("datePub was not found")
            }
        }
        val dateEndR =
            dates
                .getDataFromRegexp("""до\s+(\d{2}\.\d{2}\.\d{4}\s\d{2}:\d{2})""")
                .deleteDoubleWhiteSpace()
                .trim()
        var dateEnd = dateEndR.getDateFromString(formatterGpn)
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
        val purNum = urlTend.md5()
        val status = e.selectFirst("span.sp-2")?.ownText()?.trim { it <= ' ' } ?: ""

        val tn = Tknso(purNum, urlTend, purName, datePub, dateEnd, status)
        val t = TenderTknso(tn)
        ParserTender(t)
    }
}
