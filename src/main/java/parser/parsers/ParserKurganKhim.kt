package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadWaitWithRef
import parser.tenderClasses.KurganKhim
import parser.tenders.TenderKurganKhim
import parser.tools.formatterGpn
import parser.tools.formatterOnlyDate

class ParserKurganKhim : IParser, ParserAbstract() {
    val url = "https://kurgankhimmash-zaproscen.ru/zakupki/list?active=1&page="
    override fun parser() = parse {
        System.setProperty("jsse.enableSNIExtension", "false")
        (1..10).forEach { parserKurganKhim("$url$it") }
    }

    private fun parserKurganKhim(url: String) {
        val pageTen = downloadWaitWithRef(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("tr.registerBox")
        tenders.forEach {
            try {
                parsingTender(it)
            } catch (e: Exception) {
                logger("Error in parserKurganKhim function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTender(e: Element) {
        val purName = e.selectFirst("td:eq(1) a b")?.ownText()?.trim { it <= ' ' }
                ?: run { logger("purName not found"); return }
        val urlTender = e.selectFirst("td:eq(1) a")?.attr("href")?.trim { it <= ' ' }
                ?: run { logger("urlTender not found on $purName"); return }
        val href = "https://kurgankhimmash-zaproscen.ru$urlTender"
        val purNum = e.selectFirst("td:eq(0) > div:eq(0) b")?.ownText()?.replace("№", "")?.trim { it <= ' ' }
                ?: run { logger("purNum not found"); return }
        val orgName = e.selectFirst("td:eq(0) > div:eq(1)")?.ownText()?.trim { it <= ' ' }
                ?: ""
        val pubDateT = e.selectFirst("td:eq(2) > div:eq(0)")?.ownText()?.trim { it <= ' ' }
                ?: run { logger("pubDateT not found"); return }
        val datePub = pubDateT.getDateFromString(formatterOnlyDate)
        val endDateT = e.selectFirst("td:eq(2) > div:eq(0) > div > div")?.ownText()?.trim { it <= ' ' }
                ?: run { logger("endDateT not found"); return }
        val dateEnd = endDateT.getDateFromString(formatterGpn)
        val attachments = mutableMapOf<String, String>()
        e.select("td:eq(1) a[href^='/lotdocs/']").forEach {
            val urlAtt = "https://kurgankhimmash-zaproscen.ru${it.attr("href")}"
            val attName = it.text()
            if (attName != "") {
                attachments[attName] = urlAtt
            }
        }
        val tn = KurganKhim(purNum, href, purName, datePub, dateEnd, attachments, orgName)
        val t = TenderKurganKhim(tn)
        ParserTender(t)
    }
}