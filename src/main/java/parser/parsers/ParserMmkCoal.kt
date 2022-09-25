package parser.parsers

import java.util.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDateFromString
import parser.extensions.md5
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.MmkCoal
import parser.tenders.TenderMmkCoal
import parser.tools.formatter

class ParserMmkCoal : IParser, ParserAbstract() {
    companion object WebCl {
        const val BaseUrl = "http://mmk-coal.ru/pokupatelyam-i-postavshchikam/tendery/"
    }

    override fun parser() = parse { parserMmkCoal() }

    fun parserMmkCoal() {
        val pageTen = downloadFromUrl(BaseUrl)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", BaseUrl)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        htmlTen.select("h1:contains(Тендеры) + table tr")?.drop(1)?.forEach {
            try {
                parserTender(it)
            } catch (e: Exception) {
                logger("error in  ${this::class.simpleName}", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(el: Element) {
        val purName =
            el.selectFirst("td:eq(0)")?.text()?.trim { it <= ' ' }
                ?: run {
                    logger("purName was not found", el.text())
                    throw Exception("purName was not found")
                }
        val purNum = purName.md5()
        val datePub = Date()
        var endDateT = el.selectFirst("td:eq(1)")?.text()?.trim { it <= ' ' } ?: "BAD"
        if (endDateT == "") {
            endDateT = "BAD"
        }
        val dateEnd = endDateT.getDateFromString(formatter)
        val noticeVer = el.selectFirst("td:eq(2)")?.text()?.trim { it <= ' ' } ?: ""
        val attachments = mutableMapOf<String, String>()
        el.select("td:eq(3) a").forEach {
            val urlAtt = "http://mmk-coal.ru${it.attr("href")}"
            val attName = it.text()
            if (attName != "") {
                attachments[attName] = urlAtt
            }
        }
        val tn = MmkCoal(purNum, BaseUrl, purName, datePub, dateEnd, attachments, noticeVer)
        val t = TenderMmkCoal(tn)
        ParserTender(t)
    }
}
