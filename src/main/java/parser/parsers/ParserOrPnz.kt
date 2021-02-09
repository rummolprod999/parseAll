package parser.parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import parser.extensions.getDateFromString
import parser.extensions.md5
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.OrPnz
import parser.tenders.TenderOrPnz
import parser.tools.formatterOnlyDate

class ParserOrPnz : IParser, ParserAbstract() {
    val url = "https://www.ornpz.ru/tenderyi/predlozheniya/"
    val urlNew =
        "https://www.ornpz.ru/tenderyi/potrebnosti/dogovornaya-konkursnaya-komissiya/tablicza-tenderov-oao-orsknefteorgsintez/"

    override fun parser() = parse {
        parserOrPnz()
        parserOrPnzNew()
    }

    private fun parserOrPnz() {
        val pageTen = downloadFromUrl(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("table.table-tender tbody tr:gt(0)")
        tenders.forEach {
            try {
                parsingTender(it)
            } catch (e: Exception) {
                logger("Error in parserOrPnz function", e.stackTrace, e)
            }
        }
    }

    private fun parserOrPnzNew() {
        val pageTen = downloadFromUrl(urlNew)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val tenders = htmlTen.select("table.table-tender tr:gt(0)")
        tenders.forEach {
            try {
                parsingTenderNew(it)
            } catch (e: Exception) {
                logger("Error in parserOrPnzNew function", e.stackTrace, e)
            }
        }
    }

    private fun parsingTender(e: Element) {
        val purName = e.selectFirst("td:eq(2) div")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("purName not found"); return }
        val purNum = purName.md5()
        if (purNum == "") return
        val urlTender = e.selectFirst("td:eq(3) div a")?.attr("href")?.trim { it <= ' ' }
            ?: run { logger("urlTender not found on $purName"); return }
        val href = "https://www.ornpz.ru/$urlTender"
        val pubDateT = e.selectFirst("td:eq(0) div")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("pubDateT not found"); return }
        val datePub = pubDateT.getDateFromString(formatterOnlyDate)
        val endDateT = e.selectFirst("td:eq(1) div")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("endDateT not found"); return }
        val dateEnd = endDateT.getDateFromString(formatterOnlyDate)
        val tt = OrPnz(purNum, href, purName, datePub, dateEnd)
        val t = TenderOrPnz(tt)
        ParserTender(t)
    }

    private fun parsingTenderNew(e: Element) {
        val purName = e.selectFirst("td:eq(3) div")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("purName not found"); return }
        val purNum = e.selectFirst("td:eq(4) div")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("purNum not found"); return }
        val href = urlNew
        val pubDateT = e.selectFirst("td:eq(0) div")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("pubDateT not found"); return }
        val datePub = pubDateT.getDateFromString(formatterOnlyDate)
        val endDateT = e.selectFirst("td:eq(1) div")?.ownText()?.trim { it <= ' ' }
            ?: run { logger("endDateT not found"); return }
        val dateEnd = endDateT.getDateFromString(formatterOnlyDate)
        val tt = OrPnz(purNum, href, purName, datePub, dateEnd)
        val t = TenderOrPnz(tt)
        ParserTender(t)
    }
}