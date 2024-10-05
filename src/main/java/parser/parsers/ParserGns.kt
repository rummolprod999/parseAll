package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDateFromString
import parser.extensions.replaceDateBoretsEnd
import parser.logger.logger
import parser.networkTools.downloadFromUrlNoSslNew
import parser.tenderClasses.Gns
import parser.tenders.TenderGns
import parser.tools.formatter
import parser.tools.formatterOnlyDate
import java.time.ZoneId
import java.util.*

class ParserGns :
    ParserAbstract(),
    IParser {
    override fun parser() =
        parse {
            System.setProperty("jsse.enableSNIExtension", "false")
            parserGns("https://www.gns-tender.ru/")
            (2..3).forEach {
                try {
                    parserGns("https://www.gns-tender.ru/?page=$it")
                } catch (e: Exception) {
                    logger(e)
                }
            }
        }

    private fun parserGns(url: String) {
        val pageTen = downloadFromUrlNoSslNew(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("div.tender-item")
        tenders.forEach {
            try {
                parsingTenderList(it)
            } catch (e: Exception) {
                logger("Error in parserGns function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTenderList(e: Element) {
        val urlT =
            e.selectFirst("a:contains(Подробнее)")?.attr("href")?.trim { it <= ' ' }
                ?: run {
                    logger("urlT not found")
                    return
                }
        val urlTend = "https://www.gns-tender.ru$urlT"
        val pageTen = downloadFromUrlNoSslNew(urlTend)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", urlTend)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        parsingTender(htmlTen, urlTend)
    }

    private fun parsingTender(
        e: Element,
        url: String,
    ) {
        val purName =
            e.selectFirst("h1")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }
        val purNum =
            e.selectFirst("div.tender-number > span")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purNum not found")
                    return
                }
        val status =
            e.selectFirst("div.tender-status > p:eq(0) span")?.ownText()?.trim { it <= ' ' } ?: ""
        val pubMounth =
            e.selectFirst("div.tender-mounth")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("pubMounth not found")
                    return
                }
        val pubDay =
            e.selectFirst("div.tender-date")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("pubDay not found")
                    return
                }
        val pubYear =
            e
                .selectFirst("div.tender-year")
                ?.ownText()
                ?.replace("год", "")
                ?.trim { it <= ' ' }
                ?: run {
                    logger("pubYear not found")
                    return
                }
        val datePubT = "$pubDay$pubMounth$pubYear".replaceDateBoretsEnd()
        val datePub = Date()
        val dateEndT =
            e
                .selectFirst("div.tender-info:contains(Дата окончания приема заявок на участие:)")
                ?.ownText()
                ?.trim { it <= ' ' }
                ?: run {
                    logger("dateEndT not found")
                    return
                }
        var dateEnd = dateEndT.getDateFromString(formatterOnlyDate)
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
        val dateBiddingT =
            e.selectFirst("div.tender-info:contains(Проводится:)")?.ownText()?.trim { it <= ' ' }
                ?: ""
        val dateBidding = dateBiddingT.getDateFromString(formatter)
        val cusName =
            e.selectFirst("div.tender-info:contains(Организация:)")?.ownText()?.trim { it <= ' ' }
                ?: ""
        val region =
            e.selectFirst("div.tender-region:contains(Регион:)")?.ownText()?.trim { it <= ' ' }
                ?: ""
        val tn = Gns(purNum, url, purName, datePub, dateEnd, status, region, cusName, dateBidding)
        val t = TenderGns(tn)
        ParserTender(t)
    }
}
