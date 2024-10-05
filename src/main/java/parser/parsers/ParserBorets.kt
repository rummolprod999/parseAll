package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.*
import parser.logger.logger
import parser.networkTools.downloadWaitWithRef
import parser.tenderClasses.Borets
import parser.tenders.TenderBorets
import parser.tools.formatterBorets
import parser.tools.formatterOnlyDate
import java.time.ZoneId
import java.util.*

class ParserBorets :
    ParserAbstract(),
    IParser {
    val url = "http://tenderborets.ru/node?page="

    override fun parser() =
        parse {
            System.setProperty("jsse.enableSNIExtension", "false")
            parserBorets("http://tenderborets.ru/node")
            (1..20).forEach { parserBorets("$url$it") }
        }

    private fun parserBorets(url: String) {
        val pageTen = downloadWaitWithRef(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("div.node")
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
            e.selectFirst("h2.title > a")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }
        val urlT =
            e.selectFirst("h2.title > a")?.attr("href")?.trim { it <= ' ' }
                ?: run {
                    logger("urlT not found on $purName")
                    return
                }
        val urlTend = "http://tenderborets.ru$urlT"
        val delivTerm = e.selectFirst("div.content > p")?.ownText()?.trim { it <= ' ' } ?: ""
        val datePubT =
            e.selectFirst("span.submitted")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("datePubT not found")
                    return
                }
        val datePubR =
            datePubT
                .getDataFromRegexp("""в\s+(.+)""")
                .lowercase()
                .replaceDateBorets()
                .replace(",", "")
                .deleteDoubleWhiteSpace()
                .trim()
        val datePub = datePubR.getDateFromString(formatterBorets)
        if (datePub == Date(0L)) {
            run {
                logger("datePub was not found", urlTend)
                throw Exception("datePub was not found")
            }
        }
        val dateEndR =
            delivTerm
                .getDataFromRegexp("""Срок\s+предоставления\s+предложений\s+-\s+до\s+(.+)\s+г.""")
                .lowercase()
                .replaceDateBoretsEnd()
                .deleteAllWhiteSpace()
                .trim()
        var dateEnd = dateEndR.getDateFromString(formatterOnlyDate)
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
        val cusName =
            e
                .selectFirst("img[src ^='http://tender']")
                ?.attr("title")
                ?.replace("Аватар пользователя", "")
                ?.trim { it <= ' ' } ?: ""
        val purNum = purName.md5()
        val tn = Borets(purNum, urlTend, purName, datePub, dateEnd, cusName, delivTerm)
        val t = TenderBorets(tn)
        ParserTender(t)
    }
}
