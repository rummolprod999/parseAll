package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.extractPrice
import parser.extensions.getDateFromString
import parser.extensions.md5
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Akbars
import parser.tenders.TenderAkbars
import parser.tools.formatterOnlyDate

class ParserAkbars :
    ParserAbstract(),
    IParser {
    companion object WebCl {
        const val BaseUrl = "https://akbarsstroi.ru/tenders/all.php"
    }

    override fun parser() = parse { parserAkbars() }

    fun parserAkbars() {
        val pageTen = downloadFromUrl(BaseUrl)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", BaseUrl)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        htmlTen.select("table.MsoNormalTable tr")?.drop(2)?.forEach {
            try {
                parserTender(it)
            } catch (e: Exception) {
                logger("error in  ${this::class.simpleName}", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(el: Element) {
        val purName1 =
            el.selectFirst("td:eq(1)")?.text()?.trim { it <= ' ' }
                ?: run {
                    logger("purName1 was not found", el.text())
                    throw Exception("purName1 was not found")
                }
        val purName2 =
            el.selectFirst("td:eq(2)")?.text()?.trim { it <= ' ' }
                ?: run {
                    logger("purName2 was not found", el.text())
                    throw Exception("purName2 was not found")
                }
        val purName = "$purName1 $purName2"
        val purNum = purName.md5()
        val pubDateT =
            el.selectFirst("td:eq(5)")?.text()?.trim { it <= ' ' }
                ?: run {
                    logger("pubDateT was not found", el.text())
                    throw Exception("pubDateT was not found")
                }
        val datePub = pubDateT.getDateFromString(formatterOnlyDate)
        val endDateT =
            el.selectFirst("td:eq(6)")?.text()?.trim { it <= ' ' }
                ?: run {
                    logger("endDateT was not found", el.text())
                    throw Exception("endDateT was not found")
                }
        val dateEnd = endDateT.getDateFromString(formatterOnlyDate)
        val delivTerm1 = el.selectFirst("td:eq(3)")?.text()?.trim { it <= ' ' } ?: ""
        val delivTerm2 = el.selectFirst("td:eq(4)")?.text()?.trim { it <= ' ' } ?: ""
        val delivTerm =
            "Начало проведения работ: $delivTerm1\nОкончание проведения работ: $delivTerm2"
        val nmck =
            el
                .selectFirst("td:eq(7)")
                ?.text()
                ?.trim { it <= ' ' }
                ?.extractPrice() ?: ""
        val href = BaseUrl
        val docHrefT =
            el.selectFirst("td:eq(9) a")?.attr("href") ?: throw Exception("docHrefT was not found")
        val doc = "https://akbarsstroi.ru$docHrefT"
        val tn = Akbars(purNum, href, purName, datePub, dateEnd, nmck, delivTerm, doc)
        val t = TenderAkbars(tn)
        ParserTender(t)
    }
}
