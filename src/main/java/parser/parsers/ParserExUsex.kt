package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.extractNum
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Uzex
import parser.tenders.TenderExUzex
import parser.tools.formatter
import java.util.*

class ParserExUzex : IParser, ParserAbstract() {
    companion object WebCl {
        val BaseUrl = arrayOf("https://exarid.uzex.uz/ru?page=", "https://exarid.uzex.uz/ru/competitive/?page=", "https://exarid.uzex.uz/ru/tender2/?page=")
        const val CountPage = 50
    }

    override fun parser() = parse {
        try {
            parserUzex()
        } catch (e: Exception) {
            logger("Error in ${this::class.simpleName} function", e.stackTrace, e)
        }
    }

    private fun parserUzex() {
        BaseUrl.forEach { b ->
            (1..CountPage).forEach {
                val url = "$b$it"
                try {
                    parserPageList(url)
                } catch (e: Exception) {
                    logger(e, e.stackTrace)
                }
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
        val tenders = html.select("#table_main tbody tr")
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
        val urlT = el.selectFirst("td a.table_link")?.attr("href")?.trim { it <= ' ' }
                ?: throw Exception("urlT was not found")
        val urlTend = "https://exarid.uzex.uz$urlT"
        val purNum = el.selectFirst("td a.table_link")?.ownText()?.trim { it <= ' ' } ?: ""
        val region1 = el.selectFirst("td:eq(3)")?.ownText()?.trim { it <= ' ' } ?: ""
        val region2 = el.selectFirst("td:eq(4)")?.ownText()?.trim { it <= ' ' } ?: ""
        val region = "$region1, $region2"
        val purName = el.selectFirst("td:eq(5) a span")?.ownText()?.trim { it <= ' ' }
                ?: throw Exception("purName was not found")
        val fullNmck = el.selectFirst("td:eq(6)")?.ownText()?.trim { it <= ' ' } ?: ""
        val nmck = fullNmck.extractNum()
        val currency = fullNmck.getDataFromRegexp("(?<=\\d\\s)([a-zA-Z]+)\$")
        val dateEndTmp = el.selectFirst("td:eq(2)")?.ownText()?.trim { it <= ' ' }
                ?: throw Exception("dateEndTmp was not found")
        val dateEnd = dateEndTmp.getDateFromString(formatter)
        val datePub = Date(0L)
        val tt = Uzex(purNum, urlTend, purName, datePub, dateEnd, region, nmck, currency)
        val t = TenderExUzex(tt)
        ParserTender(t)
    }
}