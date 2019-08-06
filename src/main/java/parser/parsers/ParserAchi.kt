package parser.parsers

import parser.logger.logger
import parser.networkTools.downloadFromUrl

class ParserAchi : IParser, ParserAbstract() {
    companion object WebCl {
        val BaseUrl = arrayOf("https://achizitii.md/ru/public/tender/list?page=")
        const val CountPage = 20
    }

    override fun parser() = parse {
        try {
            parserAchi()
        } catch (e: Exception) {
            logger("Error in ${this::class.simpleName} function", e.stackTrace, e)
        }
    }

    private fun parserAchi() {
        BaseUrl.forEach { b ->
            (1..CountPage).forEach {
                val url = "$b$it"
                try {
                    parserPageList(url)
                } catch (e: Exception) {
                    logger(e, e.stackTrace)
                }
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
    }
}