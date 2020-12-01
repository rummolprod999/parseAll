package parser.parsers;

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDateFromString
import parser.extensions.md5
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Kamaz
import parser.tenders.TenderKamaz
import parser.tools.formatterOnlyDate
import java.util.*

public class ParserKamaz : IParser, ParserAbstract() {
    companion object WebCl {
        const val BaseUrl = "https://kamaz.ru/about/supplier/notification/"
    }

    override fun parser() = parse { parserProtek() }

    fun parserProtek() {
        val pageTen = downloadFromUrl(BaseUrl)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", BaseUrl)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        htmlTen.select("table.table-technical-characteristics > tbody > tr")?.drop(1)?.forEach {
            try {
                parserTender(it)
            } catch (e: Exception) {
                logger("error in  ${this::class.simpleName}", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(el: Element) {
        val purName = el.selectFirst("td:eq(1)")?.text()?.trim { it <= ' ' }
                ?: run { logger("purName was not found", el.text()); throw Exception("purName was not found") }
        val purNum = purName.md5()
        val pubDateT = el.selectFirst("td:eq(0)")?.text()?.trim { it <= ' ' }
                ?: run { logger("pubDateT was not found", el.text()); throw Exception("pubDateT was not found") }
        val datePub = pubDateT.getDateFromString(formatterOnlyDate)
        val dateEnd = Date(0L)
        val orgName = el.selectFirst("td:eq(2)")?.text()?.trim { it <= ' ' }
                ?: run { logger("orgName was not found", el.text()); throw Exception("orgName was not found") }
        val attachments = mutableMapOf<String, String>()
        el.select("td:eq(3) a").forEach {
            val urlAtt = "https://kamaz.ru${it.attr("href")}"
            val attName = it.text()
            if (attName != "") {
                attachments[attName] = urlAtt
            }
        }
        val href = attachments["Скачать"] ?: BaseUrl
        val tn = Kamaz(purNum, href, purName, datePub, dateEnd, attachments, orgName)
        val t = TenderKamaz(tn)
        ParserTender(t)

    }
}
