package parser.tenders

import org.jsoup.Jsoup
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.parsers.IParser
import parser.parsers.ParserAbstract

class ParserUzex : IParser, ParserAbstract() {
    companion object WebCl {
        const val BaseUrl = "https://dxarid.uzex.uz/ru?page="
        const val CountPage = 50
    }

    override fun parser() = parse {
        try {
            parserUzex()
        } catch (e: Exception) {
            logger("Error in ${this::class.simpleName} function", e.stackTrace, e)
        }
    }

    private fun parserUzex() {
        (1..CountPage).forEach {
            val url = "$BaseUrl$it"
            try {
                parserPageList(url)
            } catch (e: Exception) {
                logger(e, e.stackTrace)
            }
        }
    }

    private fun parserPageList(url: String) {
        val pageTen = downloadFromUrl(url)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        println(pageTen)
        val htmlTen = Jsoup.parse(pageTen)
    }
}