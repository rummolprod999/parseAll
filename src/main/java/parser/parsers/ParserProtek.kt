import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDateFromString
import parser.extensions.md5
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.parsers.IParser
import parser.parsers.ParserAbstract
import parser.tenderClasses.Protek
import parser.tenders.TenderProtek
import parser.tools.formatterOnlyDate
import java.util.*

class ParserProtek : IParser, ParserAbstract() {

    companion object WebCl {
        const val BaseUrl = "https://protek.ru/partners/tendery/active/"
    }

    override fun parser() = parse { parserProtek() }

    fun parserProtek() {
        val pageTen = downloadFromUrl(BaseUrl)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", BaseUrl)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        htmlTen.select("div.tenders_list > div.item > section.container")?.forEach {
            try {
                parserTender(it)
            } catch (e: Exception) {
                logger("error in  ${this::class.simpleName}", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(el: Element) {
        val purName = el.selectFirst("p")?.text()?.trim { it <= ' ' }
                ?: run { logger("purName was not found", el.text()); throw Exception("purName was not found") }
        val purNum = purName.md5()
        var href = el.selectFirst("a:contains(Подробнее...)")?.attr("href") ?: throw Exception("href was not found")
        href = "https://protek.ru$href"
        val endDateT = el.selectFirst("span > time")?.text()?.trim { it <= ' ' }
                ?: run { logger("endDateT was not found", el.text()); throw Exception("endDateT was not found") }
        val dateEnd = endDateT.getDateFromString(formatterOnlyDate)
        val attachments = mutableMapOf<String, String>()
        el.select("div.detail_box a").forEach {
            val urlAtt = "https://protek.ru${it.attr("href")}"
            val attName = it.text()
            if (attName != "") {
                attachments[attName] = urlAtt
            }
        }
        val tn = Protek(purNum, href, purName, Date(), dateEnd, attachments)
        val t = TenderProtek(tn)
        ParserTender(t)
    }
}