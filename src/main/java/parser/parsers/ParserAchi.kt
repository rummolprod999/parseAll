package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDataFromRegexp
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Achi
import parser.tenders.TenderAchi

class ParserAchi : IParser, ParserAbstract() {
    companion object WebCl {
        val BaseUrl = arrayOf("https://achizitii.md/ru/public/tender/list?page=")
        const val CountPage = 20
    }

    override fun parser() = parse {
        try {
            parserAchi()
        } catch (e: Exception) {
            logger("Error in ${this::class.simpleName} function", e.stackTrace, e)
        }
    }

    private fun parserAchi() {
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
        val tenders = html.select("div.tender__list__items > div.tender__list__item")
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
        val urlT =
            el.selectFirst("h2 > a")?.attr("href")?.trim { it <= ' ' }
                ?: throw Exception("urlT was not found")
        val urlTend = "https://achizitii.md$urlT"
        val purNum = urlT.getDataFromRegexp("/(\\d+)/$")
        if (purNum == "") {
            logger("empty purNum", urlTend)
            return
        }
        val purName =
            el.selectFirst("h2 > a")?.ownText()?.trim { it <= ' ' }
                ?: throw Exception("purName was not found")
        val t = TenderAchi(Achi(urlTend, purNum, purName))
        ParserTender(t)
    }
}
