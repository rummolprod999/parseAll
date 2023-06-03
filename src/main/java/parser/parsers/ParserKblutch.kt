package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrlNoSslNew
import parser.tenderClasses.Kblutch
import parser.tenders.TenderKblutch
import parser.tools.formatterOnlyDate
import java.time.ZoneId
import java.util.*

class ParserKblutch : IParser, ParserAbstract() {
    val url = "https://kb-lutch.ru/wp-content/themes/wp-luch/archive_gette_cat-5.php"
    override fun parser() = parse {
        System.setProperty("jsse.enableSNIExtension", "false")
        ParserKblutch("$url")
    }

    private fun ParserKblutch(url: String) {
        val pageTen = downloadFromUrlNoSslNew(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("div.fileblock")
        tenders.forEach {
            try {
                parsingTender(it)
            } catch (e: Exception) {
                logger("Error in parserFpk function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTender(e: Element) {
        val purName =
            e.selectFirst("h4")?.ownText()?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }
        val href =
            e.selectFirst("div a")?.attr("href")?.trim { it <= ' ' }
                ?: run {
                    logger("href not found on $purName")
                    return
                }
        val purNum = purName.getDataFromRegexp("""№\sСЗ-(\d+)""")
        val datePubT = purName.getDataFromRegexp("""(\d{2}\.\d{2}\.\d{4})""")
        val datePub = datePubT.getDateFromString(formatterOnlyDate)
        val dateEnd =
            Date.from(
                datePub.toInstant().atZone(ZoneId.systemDefault()).plusDays(2).toInstant()
            )
        val attName =
            e.selectFirst("div a")?.text()?.trim { it <= ' ' }
                ?: run {
                    logger("href not found on $purName")
                    return
                }
        val tn = Kblutch(purNum, href, purName, datePub, dateEnd, attName)
        val t = TenderKblutch(tn)
        ParserTender(t)
    }
}
