package parser.tenders

import org.jsoup.Jsoup
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.extensions.replaceDateAchi
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Achi
import parser.tools.formatterAchi
import java.util.*

class TenderAchi(val tn: Achi) : TenderAbstract(), ITender {
    data class Result(val cancelstatus: Int, val updated: Boolean)

    init {
        etpName = "Achizitii.md"
        etpUrl = "https://achizitii.md/"
    }

    val typeFz by lazy {
        207
    }

    override fun parsing() {
        val pageTen = downloadFromUrl(tn.href)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", tn.href)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val status = htmlTen.selectFirst("div.tender__item__status > span")?.text()?.trim { it <= ' ' }
                ?: ""
        val dates = htmlTen.selectFirst("div.tender__item__client__info:contains(Подача предложений:) span")?.text()?.trim { it <= ' ' }
                ?: run { logger("dates was not found", tn.href); throw Exception("dates was not found") }
        val pubDateT = dates.getDataFromRegexp("-\\s+(.+)").replaceDateAchi()
        val datePub = pubDateT.getDateFromString(formatterAchi)
        if (datePub == Date(0L)) {
            run { logger("datePub was not found", tn.href); throw Exception("datePub was not found") }
        }
        println(pubDateT)
        println(datePub)
        println()

    }
}