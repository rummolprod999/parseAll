package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Molskaz
import parser.tenders.TenderMolskaz
import parser.tools.formatter

class ParserMolskaz : IParser, ParserAbstract() {

    companion object WebCl {
        const val BaseUrl = "https://molskaz.priceflow.ru/tenders/list/?ajaxifyID=tenders_list"
    }

    override fun parser() = parse { parserMolskaz() }

    fun parserMolskaz() {
        val pageTen = downloadFromUrl(BaseUrl)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", BaseUrl)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        htmlTen.select("div.tenderitem")?.forEach {
            try {
                parserTender(it)
            } catch (e: Exception) {
                logger("error in  ${this::class.simpleName}", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(el: Element) {
        val purName =
            el.selectFirst("h4")?.text()?.trim { it <= ' ' }
                ?: run {
                    logger("purName was not found", el.text())
                    throw Exception("purName was not found")
                }
        val hrefT = el.selectFirst("a")?.attr("href") ?: throw Exception("href was not found")
        val href = "$BaseUrl$hrefT"
        val purNum = hrefT.getDataFromRegexp("collapse_(\\d+)_")
        if (purNum == "") {
            logger("cannot find purNum in tender ", href)
            return
        }
        val endDateT =
            el.selectFirst("td:contains(Окончание приема заявок) + td")?.text()?.trim { it <= ' ' }
                ?: run {
                    logger("endDateT was not found", el.text())
                    throw Exception("endDateT was not found")
                }
        val dateEndR = endDateT.getDataFromRegexp("""(\d{2}\.\d{2}\.\d{4}\s\d{2}:\d{2}:\d{2})""")
        val dateEnd = dateEndR.getDateFromString(formatter)

        val pubDateT =
            el.selectFirst("td:contains(Начало приема заявок) + td")?.text()?.trim { it <= ' ' }
                ?: run {
                    logger("pubDateT was not found", el.text())
                    throw Exception("pubDateT was not found")
                }
        val datePubR = pubDateT.getDataFromRegexp("""(\d{2}\.\d{2}\.\d{4}\s\d{2}:\d{2}:\d{2})""")
        val datePub = datePubR.getDateFromString(formatter)

        val binDateT =
            el.selectFirst("td:contains(Подведение итогов) + td")?.text()?.trim { it <= ' ' } ?: ""
        val dateBinR = binDateT.getDataFromRegexp("""(\d{2}\.\d{2}\.\d{4}\s\d{2}:\d{2}:\d{2})""")
        val dateBin = dateBinR.getDateFromString(formatter)

        val notice = el.selectFirst("p.news-item")?.text()?.trim { it <= ' ' } ?: ""
        val delivTerm =
            el.selectFirst("td:contains(Условия поставки) + td")?.text()?.trim { it <= ' ' } ?: ""
        val delivPlace =
            el.selectFirst("td:contains(Место поставки) + td")?.text()?.trim { it <= ' ' } ?: ""
        val currency =
            el.selectFirst("td:contains(Валюта тендера) + td")?.text()?.trim { it <= ' ' } ?: ""
        val pwName =
            el.selectFirst("td:contains(Вид тендера) + td")?.text()?.trim { it <= ' ' } ?: ""
        val tn =
            Molskaz(
                purNum,
                href,
                purName,
                datePub,
                dateEnd,
                dateBin,
                pwName,
                currency,
                delivTerm,
                delivPlace,
                notice
            )
        val t = TenderMolskaz(tn)
        ParserTender(t)
    }
}
