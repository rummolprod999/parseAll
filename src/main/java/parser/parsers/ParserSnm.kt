package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDataFromRegexp
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Snm
import parser.tenders.TenderSnm
import java.util.*

class ParserSnm : IParser, ParserAbstract() {
    companion object WebCl {
        const val BaseUrl = "http://www.snm.ru/tender"
    }

    override fun parser() = parse { parserSnm() }

    fun parserSnm() {
        val pageTen = downloadFromUrl(BaseUrl)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", BaseUrl)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        htmlTen.select("div.tender-item")?.forEach {
            try {
                parserTender(it)
            } catch (e: Exception) {
                logger("error in  ${this::class.simpleName}", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(el: Element) {
        val purName =
            el.selectFirst("span.tender-title")?.text()?.trim { it <= ' ' }
                ?: run {
                    logger("purName was not found", el.text())
                    throw Exception("purName was not found")
                }
        val purNum = purName.getDataFromRegexp("№(.+)$")
        if (purNum == "") {
            logger("cannot find purNum in tender ", purName)
            return
        }
        val hrefT = el.selectFirst("a")?.attr("href") ?: throw Exception("href was not found")
        val href = "http://www.snm.ru$hrefT"
        val pubDate = Date()
        val delivTerm = el.selectFirst("div.tender-item")?.ownText()?.trim { it <= ' ' } ?: ""
        val tn =
            Snm(
                purNum,
                href,
                purName,
                pubDate,
                pubDate,
                delivTerm,
            )
        val t = TenderSnm(tn)
        ParserTender(t)
    }
}
