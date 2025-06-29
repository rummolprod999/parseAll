package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.*
import parser.logger.logger
import parser.networkTools.downloadFromUrlNoSslNew
import parser.tenderClasses.Tknso
import parser.tenders.TenderTknso
import parser.tools.formatterGpn
import java.time.ZoneId
import java.util.*

class ParserTknso :
    ParserAbstract(),
    IParser {
    val listOfUrl = mutableListOf<String>(
        "https://gorkunov.com/company/altay/tenders/",
        "https://gorkunov.com/company/bel/tenders/",
        "https://gorkunov.com/company/nsk/tenders/",
        "https://gorkunov.com/company/obskoy/tenders/",
        "https://gorkunov.com/company/smolensk/tenders/",
        "https://gorkunov.com/company/tkt/tenders/",
        "https://gorkunov.com/company/tkyar/tenders/"
    )

    override fun parser() =
        parse {
            System.setProperty("jsse.enableSNIExtension", "false")
            listOfUrl.forEach { parserTknso(it) }

        }

    private fun parserTknso(url: String) {
        val pageTen = downloadFromUrlNoSslNew(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("div.uncos__item")
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
            e.selectFirst("div.uncos__name a")?.ownText()?.trim { it <= ' ' }
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
        val urlTend = "https://gorkunov.com$urlT"
        val dates =
            e
                .selectFirst("div.uncos__priem p")
                ?.ownText()
                ?.replace("«", "")
                ?.replace("»", "")
                ?.replace(",", "")
                ?.trim { it <= ' ' }
                ?: run {
                    logger("dates not found")
                    return
                }
        val datePubR =
            dates
                .getDataFromRegexp("""Начало приема предложений:\s+(\d{1,2}.+?\d{4}\s+\d{2}:\d{2})""")
                .deleteDoubleWhiteSpace()
                .replaceDateBoretsEnd()
                .replace(" .", ".")
                .replace(". ", ".")
                .trim()
        val datePub = datePubR.getDateFromString(formatterGpn)
        if (datePub == Date(0L)) {
            run {
                logger("datePub was not found", urlTend)
                throw Exception("datePub was not found")
            }
        }
        val dateEndR = dates
            .getDataFromRegexp("""Окончание приема предложений:\s+(\d{1,2}.+?\d{4}\s+\d{2}:\d{2})""")
            .deleteDoubleWhiteSpace()
            .replaceDateBoretsEnd()
            .replace(" .", ".")
            .replace(". ", ".")
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
