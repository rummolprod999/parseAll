package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.RusNano
import parser.tenders.TenderRusNano
import parser.tools.formatterGpn

class ParserRusNano : IParser, ParserAbstract() {
    val url = "https://www.b2b-rusnano.ru/market/"

    override fun parser() = parse { parserRusNano() }

    private fun parserRusNano() {
        val pageTen = downloadFromUrl(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("table.search-results tbody tr")
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
            e.selectFirst("td:eq(0) a div")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }
        val purNunAndPwName =
            e.selectFirst("td:eq(0) a")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purNunAndPwName not found")
                    return
                }
        val purNum = purNunAndPwName.getDataFromRegexp("№\\s+(\\d+)")
        val pwName = purNunAndPwName.getDataFromRegexp("(.+(?=№))")
        val orgName =
            e.selectFirst("td:eq(1) a")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("orgName not found")
                    return
                }
        val urlT =
            e.selectFirst("td:eq(0) a")?.attr("href")?.trim { it <= ' ' }
                ?: run {
                    logger("urlT not found on $purName")
                    return
                }
        val urlTend = "https://www.b2b-rusnano.ru$urlT"
        val pubDateT =
            e.selectFirst("td:eq(2)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("pubDateT not found")
                    return
                }
        val datePub = pubDateT.getDateFromString(formatterGpn)
        val endDateT =
            e.selectFirst("td:eq(2)")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("endDateT not found")
                    return
                }
        val dateEnd = endDateT.getDateFromString(formatterGpn)
        val tt = RusNano(purNum, urlTend, purName, datePub, dateEnd, pwName, orgName)
        val t = TenderRusNano(tt)
        ParserTender(t)
    }
}
